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
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * [SzigetAuthService] loads/unloads session-scoped Koin modules (see [loadAuthenticatedModules]
 * and `invalidateSession`) via the global `loadKoinModules`/`unloadKoinModules` functions, which
 * require a running Koin instance regardless of whether anything is actually resolved from it -
 * hence the [startKoin]/[stopKoin] around every test.
 *
 * [GoogleAuthProvider] and [com.ilyne.helloszigetkmp.core.config.AppConfig] are both expect/actual
 * classes whose Android actuals lazily inject an Android `Context` via Koin - unusable from a
 * plain commonTest. [GoogleAuthProviding] and [AppConfiguring] were extracted from them (minimal
 * constructor-injection change, see their kdoc) specifically so this suite can fake both instead.
 *
 * A successful [SzigetAuthService.restoreSession] now also kicks off [UsersSyncService.fetchAllUsers]
 * with the real, network-backed [com.ilyne.helloszigetkmp.core.api.SzigetApiService] loaded by
 * `loadAuthenticatedModules` (no mock-engine seam there - see the equivalent note in
 * `LoginViewModelTest`). That call is fire-and-forget on `UsersSyncService`'s own background scope,
 * so the tests below don't await or assert on it; it fails harmlessly against the fake base URL.
 */
class SzigetAuthServiceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val json = Json { ignoreUnknownKeys = true }
    private val defaultToken = TokenDto(accessToken = "access-token-1", refreshToken = "refresh-token-1", tokenType = "bearer")

    @BeforeTest
    fun setUpKoin() {
        startKoin {}
    }

    @AfterTest
    fun tearDownKoin() {
        stopKoin()
    }

    // --- signIn --------------------------------------------------------------------------------

    @Test
    fun signIn_success_persistsTokenAndLoadsAuthenticatedModules() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val service = service(
                authProvider = FakeGoogleAuthProvider(),
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
            )

            service.signIn()

            assertEquals(defaultToken, tokenStorage.read())
            // loadAuthenticatedModules() loaded the session-scoped Koin module - resolving the
            // authenticated API service confirms it actually happened.
            KoinPlatformTools.defaultContext().get().get<SzigetApiService>() // throws if the session module wasn't loaded
        }

    @Test
    fun signIn_googleSignInFails_doesNotPersistToken() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val service = service(
                authProvider = FakeGoogleAuthProvider(result = Result.failure(RuntimeException("google sign-in failed"))),
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
            )

            assertFailsWith<RuntimeException> { service.signIn() }

            assertNull(tokenStorage.read())
        }

    @Test
    fun signIn_tokenExchangeFails_doesNotPersistToken() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val service = service(
                authProvider = FakeGoogleAuthProvider(),
                tokenStorage = tokenStorage,
                authApiService = authApiService(googleLoginShouldFail = true),
            )

            assertFailsWith<Exception> { service.signIn() }

            assertNull(tokenStorage.read())
        }

    // --- logout ----------------------------------------------------------------------------

    @Test
    fun logout_clearsLocalStateBeforeAwaitingServerRevoke() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val logoutCalled = CompletableDeferred<Unit>()
            val service = service(
                authProvider = FakeGoogleAuthProvider(),
                tokenStorage = tokenStorage,
                authApiService = authApiService(onLogoutCalled = { logoutCalled.complete(Unit) }),
            )
            service.signIn()
            KoinPlatformTools.defaultContext().get().get<SzigetApiService>() // throws if the session module wasn't loaded

            service.logout()

            // Local state is already cleared by the time logout() returns - the caller can
            // navigate away immediately without waiting on the network revoke below.
            assertNull(tokenStorage.read())

            // The best-effort server-side revoke still fires in the background.
            // withTimeout() must run under a real dispatcher here, not runTest's virtual-time
            // one: on the virtual dispatcher, delay() auto-advances instantly once nothing else
            // is scheduled on it (the actual revoke runs on the service's own real
            // Dispatchers.Default background coroutine, invisible to that virtual clock), which
            // would fire the 2s timeout immediately instead of actually waiting on it.
            withContext(Dispatchers.Default) { withTimeout(2.seconds) { logoutCalled.await() } }
        }

    @Test
    fun logout_serverRevokeFails_doesNotThrowAndLocalStateStaysCleared() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val logoutCalled = CompletableDeferred<Unit>()
            val service = service(
                authProvider = FakeGoogleAuthProvider(),
                tokenStorage = tokenStorage,
                authApiService = authApiService(
                    onLogoutCalled = { logoutCalled.complete(Unit) },
                    logoutShouldFail = true,
                ),
            )
            service.signIn()

            // Must not throw even though the server-side revoke will fail.
            service.logout()

            assertNull(tokenStorage.read())
            // withTimeout() must run under a real dispatcher here, not runTest's virtual-time
            // one: on the virtual dispatcher, delay() auto-advances instantly once nothing else
            // is scheduled on it (the actual revoke runs on the service's own real
            // Dispatchers.Default background coroutine, invisible to that virtual clock), which
            // would fire the 2s timeout immediately instead of actually waiting on it.
            withContext(Dispatchers.Default) { withTimeout(2.seconds) { logoutCalled.await() } }
        }

    @Test
    fun logout_neverSignedIn_clearsLocalDataButDoesNotCallServer() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val userDao = FakeUserDao()
            val artistDao = FakeArtistDao()
            val friendDao = FakeFriendDao()
            var logoutCallCount = 0
            val service = service(
                tokenStorage = tokenStorage,
                authApiService = authApiService(onLogoutCalled = { logoutCallCount++ }),
                userDao = userDao,
                artistDao = artistDao,
                friendDao = friendDao,
            )

            // No prior signIn()/restoreSession(): authenticatedApiModule is still null, and
            // tokenStorage is empty, so there's no server-side token to revoke. Local teardown
            // still runs unconditionally though (see invalidateSession's kdoc) - it must not be
            // gated on a Koin module happening to be loaded.
            service.logout()

            assertEquals(0, logoutCallCount)
            assertTrue(userDao.calls.contains("user.deleteAll"))
            assertTrue(artistDao.calls.contains("artist.deleteAll"))
            assertTrue(friendDao.calls.contains("friend.deleteAllFriendships"))
        }

    // --- localSignIn -----------------------------------------------------------------------

    @Test
    fun localSignIn_debugBuild_bypassesGoogleSignIn_andUsesLocalBearerToken() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val authProvider = FakeGoogleAuthProvider()
            val service = service(
                appConfig = FakeAppConfig(isDebug = true),
                authProvider = authProvider,
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
            )

            service.localSignIn()

            assertEquals(0, authProvider.signInCallCount)
            val savedToken = tokenStorage.read()
            assertEquals(BEARER_TOKEN_LOCALHOST, savedToken?.accessToken)
            assertEquals("", savedToken?.refreshToken)
            KoinPlatformTools.defaultContext().get().get<SzigetApiService>() // throws if the session module wasn't loaded
        }

    @Test
    fun localSignIn_debugBuild_withTokenOverride_usesOverrideTokenInsteadOfLocalBearerToken() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val authProvider = FakeGoogleAuthProvider()
            val debugConfigStore = DebugConfigStore(MapSettings())
            debugConfigStore.setTokenOverride("debug-sheet-token")
            val service = service(
                appConfig = FakeAppConfig(isDebug = true),
                authProvider = authProvider,
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
                debugConfigStore = debugConfigStore,
            )

            service.localSignIn()

            assertEquals(0, authProvider.signInCallCount)
            val savedToken = tokenStorage.read()
            assertEquals("debug-sheet-token", savedToken?.accessToken)
            assertEquals("", savedToken?.refreshToken)
        }

    @Test
    fun localSignIn_releaseBuild_ignoresBypass_fallsBackToRealSignIn() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val authProvider = FakeGoogleAuthProvider()
            val service = service(
                appConfig = FakeAppConfig(isDebug = false),
                authProvider = authProvider,
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
            )

            service.localSignIn()

            assertEquals(1, authProvider.signInCallCount)
            assertEquals(defaultToken, tokenStorage.read())
        }

    // --- restoreSession ----------------------------------------------------------------------

    @Test
    fun restoreSession_noStoredToken_returnsFalse() =
        runTest {
            val currentUserProvider = CurrentUserProvider()
            val service = service(
                tokenStorage = TokenStorage(MapSettings()),
                authApiService = authApiService(),
                currentUserProvider = currentUserProvider,
            )

            val restored = service.restoreSession()

            assertFalse(restored)
            assertFailsWith<IllegalStateException> { currentUserProvider.currentUser }
        }

    @Test
    fun restoreSession_tokenPresentAndUserFound_returnsTrueAndSetsCurrentUser() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            tokenStorage.save(defaultToken)
            val currentUserProvider = CurrentUserProvider()
            val userDao = FakeUserDao(currentUser = UserEntity(id = "u1", name = "User One", imageUrl = null))
            val service = service(
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
                userDao = userDao,
                currentUserProvider = currentUserProvider,
            )

            val restored = service.restoreSession()

            assertTrue(restored)
            assertEquals("u1", currentUserProvider.currentUser.id)
        }

    @Test
    fun restoreSession_tokenPresentButNoLocalUser_treatsAsCorruptSession_invalidatesAndReturnsFalse() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            tokenStorage.save(defaultToken)
            val currentUserProvider = CurrentUserProvider()
            val userDao = FakeUserDao(currentUser = null)
            val artistDao = FakeArtistDao()
            val friendDao = FakeFriendDao()
            val service = service(
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
                userDao = userDao,
                artistDao = artistDao,
                friendDao = friendDao,
                currentUserProvider = currentUserProvider,
            )

            val restored = service.restoreSession()

            assertFalse(restored)
            // A missing DB user is treated as a corrupt/incomplete session: the token is wiped
            // rather than leaving the caller with a "restored" session with no user to show.
            assertNull(tokenStorage.read())
            assertFailsWith<IllegalStateException> { currentUserProvider.currentUser }
            assertTrue(userDao.calls.contains("user.deleteAll"))
            assertTrue(artistDao.calls.contains("artist.deleteAll"))
        }

    // --- clearLocalUserData (via invalidateSession / a corrupt restoreSession) -------------------

    @Test
    fun corruptRestoreSession_clearsLocalCachesInForeignKeySafeOrder() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            tokenStorage.save(defaultToken)
            val calls = mutableListOf<String>()
            val userDao = FakeUserDao(currentUser = null, calls = calls)
            val artistDao = FakeArtistDao(calls = calls)
            val friendDao = FakeFriendDao(calls = calls)
            val settings = MapSettings()
            settings.putLong("SoftRefreshGate_lastFetchedAt_ArtistRepository", 123L)
            val service = service(
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
                userDao = userDao,
                artistDao = artistDao,
                friendDao = friendDao,
                settings = settings,
            )

            service.restoreSession()

            // friend rows (children) before users/artists (parents), matching the users/artists
            // foreign keys clearLocalUserData()'s kdoc calls out.
            assertEquals(
                listOf(
                    "friend.deleteAllArtistFriendFavorited",
                    "friend.deleteAllFriendships",
                    "user.clearCurrentUser",
                    "user.deleteAll",
                    "artist.deleteAll",
                ),
                calls,
            )
            // SoftRefreshGate's persisted timestamps are wiped alongside the DB rows so a
            // subsequent login doesn't look "fresh" with an empty cache.
            assertFalse(settings.hasKey("SoftRefreshGate_lastFetchedAt_ArtistRepository"))
        }

    // --- test doubles ------------------------------------------------------------------------

    private fun service(
        appConfig: AppConfiguring = FakeAppConfig(),
        authProvider: GoogleAuthProviding = FakeGoogleAuthProvider(),
        tokenStorage: TokenStorage,
        authApiService: SzigetAuthApiService,
        userDao: UserDao = FakeUserDao(),
        artistDao: ArtistDao = FakeArtistDao(),
        friendDao: FriendDao = FakeFriendDao(),
        settings: Settings = MapSettings(),
        currentUserProvider: CurrentUserProvider = CurrentUserProvider(),
        usersSyncService: UsersSyncService = UsersSyncService(userRepository = UserRepository(dao = userDao)),
        debugConfigStore: DebugConfigStore = DebugConfigStore(MapSettings()),
    ): SzigetAuthService =
        SzigetAuthService(
            appConfig = appConfig,
            authProvider = authProvider,
            tokenStorage = tokenStorage,
            szigetAuthApiService = authApiService,
            userRepository = UserRepository(dao = userDao),
            currentUserProvider = currentUserProvider,
            artistDao = artistDao,
            userDao = userDao,
            friendDao = friendDao,
            settings = settings,
            usersSyncService = usersSyncService,
            debugConfigStore = debugConfigStore,
        )

    private fun authApiService(
        appConfig: AppConfiguring = FakeAppConfig(),
        googleLoginResult: TokenDto = defaultToken,
        googleLoginShouldFail: Boolean = false,
        onLogoutCalled: (() -> Unit)? = null,
        logoutShouldFail: Boolean = false,
    ): SzigetAuthApiService {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/google/mobile") && googleLoginShouldFail -> {
                    respondError(HttpStatusCode.Unauthorized)
                }

                path.endsWith("/google/mobile") -> {
                    respond(json.encodeToString(googleLoginResult), headers = jsonHeaders)
                }

                path.endsWith("/logout") -> {
                    onLogoutCalled?.invoke()
                    if (logoutShouldFail) {
                        respondError(HttpStatusCode.InternalServerError)
                    } else {
                        respond("{}", headers = jsonHeaders)
                    }
                }

                else -> {
                    respondError(HttpStatusCode.NotFound)
                }
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }
        return SzigetAuthApiService(client = client, appConfig = appConfig)
    }

    private class FakeAppConfig(
        private val isDebug: Boolean = false,
        private val baseUrl: String = "https://unused.test",
    ) : AppConfiguring {
        override fun baseUrlLocal(): String = baseUrl

        override fun isDebug(): Boolean = isDebug
    }

    private class FakeGoogleAuthProvider(
        private val result: Result<AuthUser> = Result.success(
            AuthUser(idToken = "google-id-token", email = "user@example.com", displayName = "Test User", photoUrl = null),
        ),
    ) : GoogleAuthProviding {
        var signInCallCount = 0
            private set

        override suspend fun signIn(): AuthUser {
            signInCallCount++
            return result.getOrThrow()
        }

        override fun signOut() {
            // Unused in this test.
        }

        override fun getCurrentUser(): AuthUser? = null
    }

    private class FakeArtistDao(
        val calls: MutableList<String> = mutableListOf(),
    ) : ArtistDao {
        override fun observeAll(): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<ArtistEntity?> = flowOf(null)

        override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override fun searchByName(query: String): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(artists: List<ArtistEntity>): Unit = throw NotImplementedError("unused in this test")

        override suspend fun setFavorited(
            id: String,
            isFavorited: Boolean,
        ): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteAll() {
            calls.add("artist.deleteAll")
        }
    }

    private class FakeUserDao(
        private val currentUser: UserEntity? = null,
        val calls: MutableList<String> = mutableListOf(),
    ) : UserDao {
        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>): Unit = throw NotImplementedError("unused in this test")

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity): Unit = throw NotImplementedError("unused in this test")

        override suspend fun getCurrentUser(): UserEntity? = currentUser

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {
            calls.add("user.clearCurrentUser")
        }

        override suspend fun deleteAll() {
            calls.add("user.deleteAll")
        }
    }

    private class FakeFriendDao(
        val calls: MutableList<String> = mutableListOf(),
    ) : FriendDao {
        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteFriendshipsNotIn(
            userId: String,
            friendIds: List<String>,
        ): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteAllFriendships() {
            calls.add("friend.deleteAllFriendships")
        }

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>): Unit =
            throw NotImplementedError("unused in this test")

        override suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>): Unit =
            throw NotImplementedError("unused in this test")

        override suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>): Unit =
            throw NotImplementedError("unused in this test")

        override suspend fun deleteAllArtistFriendFavorited() {
            calls.add("friend.deleteAllArtistFriendFavorited")
        }

        override fun observeFriends(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = flowOf(emptyList())
    }
}
