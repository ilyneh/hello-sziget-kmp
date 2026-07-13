package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.config.AppConfig
import com.ilyne.helloszigetkmp.core.config.BEARER_TOKEN_LOCALHOST
import com.ilyne.helloszigetkmp.di.createAuthenticatedApiModule
import com.ilyne.helloszigetkmp.di.presentationModule
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

class SzigetAuthService(
    private val appConfig: AppConfig,
    private val authProvider: GoogleAuthProvider,
    private val tokenStorage: TokenStorage,
    private val szigetAuthApiService: SzigetAuthApiService,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider,
) {
    private var authenticatedApiModule: Module? = null

    private val _sessionInvalidated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionInvalidated = _sessionInvalidated.asSharedFlow()

    suspend fun signIn() {
        val authUser = authProvider.signIn()
        val token = szigetAuthApiService.googleLogin(googleToken = authUser.idToken)
        tokenStorage.save(token)
        loadAuthenticatedModules(token)
    }

    suspend fun logout() {
        tokenStorage.read()?.let {
            try {
                szigetAuthApiService.logout(accessToken = it.accessToken, refreshToken = it.refreshToken)
            } catch (e: Exception) {
                Logger.e("SzigetAuthService", "Backend logout call failed, invalidating session locally anyway", e)
            }
        }
        invalidateSession()
    }

    /**
     * Dev-only shortcut that bypasses Google sign-in entirely and logs in as a fixed backend
     * account using [BEARER_TOKEN_LOCALHOST]. Callers gate this behind [SKIP_GOOGLE_SIGN_IN], but
     * that flag alone is not a safe-enough rail: it's trivial to leave set to `true` by accident.
     * So the real guarantee lives here, next to the bypass itself - if this is ever invoked in a
     * non-debug (release) build, we ignore the bypass and fall back to the real [signIn] flow
     * instead of throwing or silently no-op-ing, so the app doesn't get stuck for the user. A
     * release build therefore behaves as if [SKIP_GOOGLE_SIGN_IN] were always `false`, regardless
     * of its actual value.
     */
    suspend fun localSignIn() {
        if (!appConfig.isDebug()) {
            signIn()
            return
        }

        val token = TokenDto(
            accessToken = BEARER_TOKEN_LOCALHOST,
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
     */
    suspend fun restoreSession(): Boolean {
        val tokenFromStorage = tokenStorage.read() ?: return false
        loadAuthenticatedModules(tokenFromStorage)

        val user = userRepository.getCurrentUser()
        if (user == null) {
            invalidateSession()
            return false
        }
        currentUserProvider.set(user)
        return true
    }

    fun invalidateSession() {
        val moduleToUnload = authenticatedApiModule ?: return
        authenticatedApiModule = null

        tokenStorage.clear()
        currentUserProvider.clear()
        unloadKoinModules(presentationModule)
        unloadKoinModules(moduleToUnload)

        _sessionInvalidated.tryEmit(Unit)
    }

    private fun loadAuthenticatedModules(token: TokenDto) {
        val apiModule = createAuthenticatedApiModule(
            baseUrl = appConfig.baseUrlLocal(),
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            tokenStorage = tokenStorage,
            onSessionInvalidated = ::invalidateSession,
            isDebug = appConfig.isDebug(),
        )
        authenticatedApiModule = apiModule
        loadKoinModules(apiModule)
        loadKoinModules(presentationModule)
    }
}
