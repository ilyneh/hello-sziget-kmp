package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.ilyne.helloszigetkmp.core.config.AppConfiguring
import com.ilyne.helloszigetkmp.core.config.BEARER_TOKEN_LOCALHOST
import com.ilyne.helloszigetkmp.core.config.DebugConfigStore
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.repository.SoftRefreshGate
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
import com.ilyne.helloszigetkmp.di.createAuthenticatedApiModule
import com.ilyne.helloszigetkmp.di.presentationModule
import com.ilyne.helloszigetkmp.util.Logger
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

class SzigetAuthService(
    private val appConfig: AppConfiguring,
    private val authProvider: GoogleAuthProviding,
    private val tokenStorage: TokenStorage,
    private val szigetAuthApiService: SzigetAuthApiService,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val artistDao: ArtistDao,
    private val userDao: UserDao,
    private val friendDao: FriendDao,
    private val settings: Settings,
    private val usersSyncService: UsersSyncService,
    private val debugConfigStore: DebugConfigStore,
) : KoinComponent {
    // Resolved lazily: the authenticated SzigetApiService only exists in Koin once
    // loadAuthenticatedModules() has loaded its module - mirrors LoginViewModel's own
    // lazy `by inject()` of the same type, for the same reason.
    private val apiService: SzigetApiService by inject()

    private var authenticatedApiModule: Module? = null

    // Guards authenticatedApiModule so a logout() racing a concurrent token-refresh-failure
    // callback (see AuthenticatedApiModule's onSessionInvalidated, invoked from Ktor's Auth
    // plugin on the client engine's dispatcher) can't both observe it non-null and each run
    // teardown/Room deletes.
    private val sessionMutex = Mutex()

    // Outlives any single screen/ViewModel so the best-effort server-side revoke below still
    // fires even if logout() itself returns (and its caller navigates away) before that
    // network call finishes.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _sessionInvalidated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionInvalidated = _sessionInvalidated.asSharedFlow()

    suspend fun signIn() {
        val authUser = authProvider.signIn()
        val token = szigetAuthApiService.googleLogin(googleToken = authUser.idToken)
        tokenStorage.save(token)
        loadAuthenticatedModules(token)
    }

    /**
     * Logging out should feel instantaneous to the user, so this only awaits the local work
     * (clearing tokens/DB/Koin modules, all fast in-memory/local-disk operations) before
     * returning - the server-side token revocation is a nice-to-have, not something the UI
     * should block navigation on, so it's fired in the background instead. The local token is
     * already cleared by the time this returns, so the device can't use it again regardless of
     * whether the revoke call itself succeeds.
     */
    suspend fun logout() {
        val token = tokenStorage.read()
        invalidateSession()
        if (token != null) {
            serviceScope.launch {
                try {
                    szigetAuthApiService.logout(accessToken = token.accessToken, refreshToken = token.refreshToken)
                } catch (e: Exception) {
                    // Best-effort: the token is already cleared locally, so a failed revoke just
                    // means it stays valid server-side until it naturally expires.
                    Logger.e("SzigetAuthService", "Background server-side logout failed", e)
                }
            }
        }
    }

    /**
     * Dev-only shortcut that bypasses Google sign-in entirely and logs in as a fixed backend
     * account using [BEARER_TOKEN_LOCALHOST], or [DebugConfigStore]'s token override if the login
     * screen's debug bottom sheet has set one. Callers gate this behind [SKIP_GOOGLE_SIGN_IN] (or
     * its runtime [DebugConfigStore] counterpart), but that alone is not a safe-enough rail: it's
     * trivial to leave set to `true` by accident. So the real guarantee lives here, next to the
     * bypass itself - if this is ever invoked in a non-debug (release) build, we ignore the
     * bypass and fall back to the real [signIn] flow instead of throwing or silently no-op-ing,
     * so the app doesn't get stuck for the user. A release build therefore behaves as if
     * [SKIP_GOOGLE_SIGN_IN] were always `false`, regardless of its actual value.
     */
    suspend fun localSignIn() {
        if (!appConfig.isDebug()) {
            signIn()
            return
        }

        val token = TokenDto(
            accessToken = debugConfigStore.getTokenOverride() ?: BEARER_TOKEN_LOCALHOST,
            refreshToken = "",
            tokenType = "bearer",
        )
        tokenStorage.save(token)
        loadAuthenticatedModules(token)
    }

    /**
     * Restores a previously signed-in session. Also re-establishes [currentUserProvider] from the
     * local DB (already synced by an earlier login) before returning, so screens reached via the
     * `Main` start destination can rely on [CurrentUserProvider.currentUser] being set with no
     * race. A missing DB user means a corrupt/incomplete session — treat it as logged out rather
     * than loading Main with no current user to show.
     *
     * Also (re)starts [usersSyncService]'s background fetch, same as a fresh [signIn] does. This
     * is the only path that keeps `awaitSuccessfulSync()` callers (e.g. ProfileViewModel) from
     * suspending forever on every launch that isn't a brand-new login - fetchAllUsers is
     * otherwise never called on a restored session.
     */
    suspend fun restoreSession(): Boolean {
        val tokenFromStorage = tokenStorage.read() ?: return false
        loadAuthenticatedModules(tokenFromStorage)

        val user = userRepository.getCurrentUser()
        return if (user == null) {
            invalidateSession()
            false
        } else {
            currentUserProvider.set(user)
            usersSyncService.fetchAllUsers(apiService)
            true
        }
    }

    suspend fun invalidateSession() {
        val moduleToUnload = sessionMutex.withLock {
            val current = authenticatedApiModule
            authenticatedApiModule = null
            current
        }

        // Clearing credentials must not be gated on a Koin module happening to be loaded - always
        // run local teardown; only the module unload itself is conditional on there being one.
        tokenStorage.clear()
        currentUserProvider.clear()
        clearLocalUserData()
        if (moduleToUnload != null) {
            unloadKoinModules(presentationModule)
            unloadKoinModules(moduleToUnload)
        }

        _sessionInvalidated.tryEmit(Unit)
    }

    /**
     * Wipes every locally cached row that is scoped to the signed-out user, plus the
     * [SoftRefreshGate] "last fetched" timestamps that gate each repository's refetch. Without
     * this, a subsequent login (same device, different Google account) would read the previous
     * user's still-intact Room cache - stale favorited artists, friend-favorite summaries, etc. -
     * and `SoftRefreshGate` would even suppress refetching it for up to its staleness threshold.
     * Deletion order respects the `users`/`artists` foreign keys (children before parents).
     */
    private suspend fun clearLocalUserData() {
        friendDao.deleteAllArtistFriendFavorited()
        friendDao.deleteAllFriendships()
        userDao.clearCurrentUser()
        userDao.deleteAll()
        artistDao.deleteAll()
        SoftRefreshGate.clearAll(settings)
    }

    /**
     * Idempotent under [sessionMutex]: if a session module is already loaded (e.g. [restoreSession]
     * re-entered after an Android Activity recreation, since `MainActivity` has no
     * `android:configChanges` and this is called unconditionally from a `LaunchedEffect(Unit)`),
     * the stale module is unloaded first instead of being silently orphaned - otherwise its
     * `HttpClient` leaks and `presentationModule`'s repository singletons get replaced out from
     * under any ViewModel still holding the old ones.
     */
    private suspend fun loadAuthenticatedModules(token: TokenDto) {
        val apiModule = createAuthenticatedApiModule(
            baseUrl = appConfig.baseUrlLocal(),
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            tokenStorage = tokenStorage,
            onSessionInvalidated = ::invalidateSession,
            isDebug = appConfig.isDebug(),
        )
        val previousModule = sessionMutex.withLock {
            val previous = authenticatedApiModule
            authenticatedApiModule = apiModule
            previous
        }
        if (previousModule != null) {
            unloadKoinModules(presentationModule)
            unloadKoinModules(previousModule)
        }
        loadKoinModules(apiModule)
        loadKoinModules(presentationModule)
    }
}
