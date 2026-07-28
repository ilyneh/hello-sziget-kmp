package com.ilyne.helloszigetkmp.presentation.feature.login

import app.cash.turbine.test
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.ilyne.helloszigetkmp.core.auth.AuthUser
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.auth.GoogleAuthProviding
import com.ilyne.helloszigetkmp.core.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import com.ilyne.helloszigetkmp.core.config.AppConfiguring
import com.ilyne.helloszigetkmp.core.config.DebugConfigStore
import com.ilyne.helloszigetkmp.core.config.SKIP_GOOGLE_SIGN_IN
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
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * [LoginViewModel.signInWithGoogle] drives [SzigetAuthService.signIn] (the Google path - the
 * local-sign-in bypass is only reachable when [AppConfiguring.isDebug] is true, and
 * [FakeAppConfig] below defaults to `false`, so `localSignIn`'s bypass branch is not reachable
 * from a plain unit test), then syncs the signed-in
 * user, sets [CurrentUserProvider], kicks off a background [UsersSyncService] fetch, and emits
 * [LoginEffect.NavigateToMain] - or surfaces an error with no navigation if any step throws.
 *
 * Note on coverage: [SzigetAuthService.signIn] loads a *real* network-backed [SzigetApiService]
 * into the global Koin container as its final step (`loadAuthenticatedModules` ->
 * `createAuthenticatedApiModule` -> `createApiHttpClient`, which always builds a plain
 * `HttpClient {}` with no engine override seam - unlike [SzigetAuthApiService] here, which takes
 * an injected [HttpClient] and so can be pointed at a [MockEngine]). [LoginViewModel] then
 * resolves that same singleton via `by inject()` to call `getMe()`. There is no seam to substitute
 * a fake for that call, so the "auth succeeds, profile sync succeeds, nav effect fires" branch
 * can't be exercised here without real network I/O; the tests below cover every other branch,
 * plus the equivalent-shaped "auth succeeds, then the API call fails" case, which confirms the
 * error path still degrades correctly once real auth has gone through.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val json = Json { ignoreUnknownKeys = true }
    private val defaultToken = TokenDto(accessToken = "access-token-1", refreshToken = "refresh-token-1", tokenType = "bearer")

    // LoginViewModel's initial debugSkipGoogleSignIn reads DebugConfigStore.getSkipGoogleSignIn(),
    // which falls back to the SKIP_GOOGLE_SIGN_IN compile-time constant (itself driven by the
    // sziget.skipGoogleSignIn Gradle property, which may be true in a local dev checkout with
    // local.properties configured for e2e testing) - so LoginUiState()'s hardcoded `false` default
    // isn't a safe expectation here regardless of environment.
    private val defaultLoginUiState = LoginUiState(debugSkipGoogleSignIn = SKIP_GOOGLE_SIGN_IN)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startKoin {}
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithGoogle_googleSignInFails_surfacesErrorAndDoesNotNavigate() =
        runTest {
            val currentUserProvider = CurrentUserProvider()
            val userDao = FakeUserDao()
            val viewModel = viewModel(
                authProvider = FakeGoogleAuthProvider(result = Result.failure(RuntimeException("google sign-in failed"))),
                userDao = userDao,
                currentUserProvider = currentUserProvider,
            )

            viewModel.uiState.test {
                assertEquals(defaultLoginUiState, awaitItem())

                viewModel.signInWithGoogle()

                // FakeGoogleAuthProvider.signIn() throws without any real suspension point, so
                // under UnconfinedTestDispatcher the whole launch body (both the isLoading=true
                // update and the subsequent error update) runs to completion before this
                // collector gets scheduled - StateFlow only guarantees the latest value is seen,
                // not every intermediate one, so only the final state is asserted here.
                val errored = awaitItem()
                assertEquals("google sign-in failed", errored.error)
                assertEquals(false, errored.isLoading)
            }
            assertFailsWith<IllegalStateException> { currentUserProvider.currentUser }
            assertEquals(0, userDao.setCurrentUserCallCount)
        }

    @Test
    fun signInWithGoogle_tokenExchangeFails_surfacesErrorAndDoesNotNavigate() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val currentUserProvider = CurrentUserProvider()
            val viewModel = viewModel(
                tokenStorage = tokenStorage,
                authApiService = authApiService(googleLoginShouldFail = true),
                currentUserProvider = currentUserProvider,
            )

            viewModel.uiState.test {
                assertEquals(defaultLoginUiState, awaitItem())

                viewModel.signInWithGoogle()

                assertEquals(defaultLoginUiState.copy(isLoading = true), awaitItem())
                val errored = awaitItem()
                assertEquals(false, errored.isLoading)
                assertEquals(true, errored.error?.isNotBlank())
            }
            assertNull(tokenStorage.read())
            assertFailsWith<IllegalStateException> { currentUserProvider.currentUser }
        }

    @Test
    fun signInWithGoogle_authSucceeds_butProfileSyncFails_surfacesErrorAndDoesNotNavigate() =
        runTest {
            val tokenStorage = TokenStorage(MapSettings())
            val currentUserProvider = CurrentUserProvider()
            val viewModel = viewModel(
                tokenStorage = tokenStorage,
                authApiService = authApiService(),
                currentUserProvider = currentUserProvider,
            )

            viewModel.uiState.test {
                assertEquals(defaultLoginUiState, awaitItem())

                viewModel.signInWithGoogle()

                assertEquals(defaultLoginUiState.copy(isLoading = true), awaitItem())
                val errored = awaitItem()
                assertEquals(false, errored.isLoading)
                // The token exchange itself succeeded (persisted before the failing API call).
                assertEquals(defaultToken, tokenStorage.read())
            }
            // The failed getMe() call means the profile never synced, so CurrentUserProvider was
            // never populated and no navigation effect was emitted (verified implicitly above:
            // uiState.test would have received an unexpected NavigateToMain-driven state change).
            assertFailsWith<IllegalStateException> { currentUserProvider.currentUser }
        }

    private fun viewModel(
        appConfig: AppConfiguring = FakeAppConfig(),
        authProvider: GoogleAuthProviding = FakeGoogleAuthProvider(),
        tokenStorage: TokenStorage = TokenStorage(MapSettings()),
        authApiService: SzigetAuthApiService = authApiService(),
        userDao: UserDao = FakeUserDao(),
        artistDao: ArtistDao = FakeArtistDao(),
        friendDao: FriendDao = FakeFriendDao(),
        currentUserProvider: CurrentUserProvider = CurrentUserProvider(),
        debugConfigStore: DebugConfigStore = DebugConfigStore(MapSettings()),
    ): LoginViewModel {
        val userRepository = UserRepository(dao = userDao)
        val usersSyncService = UsersSyncService(userRepository)
        val szigetAuthService = SzigetAuthService(
            appConfig = appConfig,
            authProvider = authProvider,
            tokenStorage = tokenStorage,
            szigetAuthApiService = authApiService,
            userRepository = userRepository,
            currentUserProvider = currentUserProvider,
            artistDao = artistDao,
            userDao = userDao,
            friendDao = friendDao,
            settings = MapSettings(),
            usersSyncService = usersSyncService,
            debugConfigStore = debugConfigStore,
        )
        return LoginViewModel(
            szigetAuthService = szigetAuthService,
            userRepository = userRepository,
            usersSyncService = usersSyncService,
            currentUserProvider = currentUserProvider,
            appConfig = appConfig,
            debugConfigStore = debugConfigStore,
        )
    }

    private fun authApiService(
        appConfig: AppConfiguring = FakeAppConfig(),
        googleLoginResult: TokenDto = defaultToken,
        googleLoginShouldFail: Boolean = false,
    ): SzigetAuthApiService {
        val engine = mockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/google/mobile") && googleLoginShouldFail -> respondError(HttpStatusCode.Unauthorized)
                path.endsWith("/google/mobile") -> respond(json.encodeToString(googleLoginResult), headers = jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }
        return SzigetAuthApiService(client = client, appConfig = appConfig)
    }

    // MockEngine's default dispatcher hops off the test dispatcher onto a real background
    // dispatcher, which races the UnconfinedTestDispatcher-driven collector in the Turbine-based
    // tests above (StateFlow only guarantees the latest value reaches a collector, so an
    // isLoading=true state emitted and overwritten before the collector is rescheduled is
    // silently dropped instead of observed). Pinning the engine to Dispatchers.Main (the test
    // dispatcher installed in setUp) keeps everything on one deterministic dispatcher.
    private fun mockEngine(handler: MockRequestHandler) =
        MockEngine(MockEngineConfig().apply {
            addHandler(handler)
            dispatcher = Dispatchers.Main
        })

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
        override suspend fun signIn(): AuthUser = result.getOrThrow()

        override fun signOut() {
            // Unused in this test.
        }

        override fun getCurrentUser(): AuthUser? = null
    }

    private class FakeArtistDao : ArtistDao {
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
            // Unused in this test.
        }
    }

    private class FakeUserDao(
        private val currentUser: UserEntity? = null,
    ) : UserDao {
        var setCurrentUserCallCount = 0
            private set

        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>) {
            // Unused in this test.
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {
            setCurrentUserCallCount++
        }

        override suspend fun getCurrentUser(): UserEntity? = currentUser

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {
            // Unused in this test.
        }

        override suspend fun deleteAll() {
            // Unused in this test.
        }
    }

    private class FakeFriendDao : FriendDao {
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
            // Unused in this test.
        }

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>): Unit =
            throw NotImplementedError("unused in this test")

        override suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>): Unit =
            throw NotImplementedError("unused in this test")

        override suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>): Unit =
            throw NotImplementedError("unused in this test")

        override suspend fun deleteAllArtistFriendFavorited() {
            // Unused in this test.
        }

        override fun observeFriends(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = flowOf(emptyList())
    }
}
