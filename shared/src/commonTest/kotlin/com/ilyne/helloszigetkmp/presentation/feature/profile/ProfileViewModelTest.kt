package com.ilyne.helloszigetkmp.presentation.feature.profile

import app.cash.turbine.test
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.ArtistDto
import com.ilyne.helloszigetkmp.core.api.dto.ArtistFriendsFavoritedDto
import com.ilyne.helloszigetkmp.core.api.dto.UserDto
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity.Status
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.ilyne.helloszigetkmp.core.media.DeviceImage
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.domain.usecase.GetLikedArtistCountUseCase
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [ProfileViewModel.init] awaits [UsersSyncService.awaitSuccessfulSync] before refreshing friends
 * and artists (skipping the refresh entirely on a failed sync). [ProfileViewModel.uploadPhoto]
 * (triggered via [ProfileIntent.PhotoPicked]) applies `pendingImageUrl` optimistically and rolls
 * it back on API failure. Friend accept/decline/remove intents delegate to [FriendRepository],
 * whose own optimistic-write/rollback behavior is covered by FriendRepositoryTest — here we only
 * verify [ProfileViewModel] wires the right currentUserId/friendId and reacts correctly to
 * success/failure. Logout is a simple two-step confirm flow gated by [ProfileUiState.showLogoutAlert].
 *
 * [UsersSyncService.fetchAllUsers] runs its fetch on a real `Dispatchers.Default` scope (not the
 * test dispatcher), so every test that needs a resolved sync drives it to completion via
 * `buildViewModel`'s pre-construction await *before* constructing the [ProfileViewModel] — that
 * way `awaitSuccessfulSync()` resolves against an already-terminal
 * [kotlinx.coroutines.flow.StateFlow] value instead of racing a real background thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val json = Json { ignoreUnknownKeys = true }
    private val currentUser = User(id = "me", name = "Me", imageUrl = "https://cdn.test/me.png")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- init: sync-then-refresh sequencing ----

    @Test
    fun init_syncSucceeds_refreshesFriendsAndArtists() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val artistDao = FakeProfileArtistDao()
            val friends = listOf(UserDto(id = "f1", name = "Friend One", imageUrl = null))
            val artists = listOf(artistDto("a1", "Artist One"))

            val viewModel = buildViewModel(
                friendDao = friendDao,
                artistDao = artistDao,
                friendsApi = friendApi(friends = friends),
                artistsApi = artistApi(artists = artists),
            )

            assertEquals(currentUser.name, viewModel.uiState.value.name)
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(friendDao.calls.contains("upsert(me, f1, ${Status.ACCEPTED})"))
            assertEquals(listOf("a1"), artistDao.upsertAllCalls.flatten().map { it.id })
        }

    @Test
    fun init_syncFails_skipsFriendsAndArtistsRefresh() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val artistDao = FakeProfileArtistDao()

            val viewModel = buildViewModel(
                friendDao = friendDao,
                artistDao = artistDao,
                syncApi = failingApi(),
            )

            assertEquals(currentUser.name, viewModel.uiState.value.name)
            assertTrue(friendDao.calls.isEmpty())
            assertTrue(artistDao.upsertAllCalls.isEmpty())
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    // ---- refresh (pull-to-refresh) ----

    @Test
    fun refresh_repositoryThrows_setsErrorAndClearsLoadingInsteadOfCrashing() =
        runTest {
            // syncApi fails, so init's own friendRepository.refresh() call is skipped entirely
            // (see init_syncFails_skipsFriendsAndArtistsRefresh above) - isolating this test to
            // the explicit refresh() call below, which must survive friendsApi throwing instead
            // of crashing the process (the bug this test guards against: refresh() previously had
            // no try/catch around friendRepository.refresh(force = true)).
            val viewModel = buildViewModel(friendsApi = failingApi(), syncApi = failingApi())
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)

            viewModel.refresh()
            viewModel.uiState.first { it.error != null }

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(ProfileErrorReason.LOAD_PROFILE_FAILED, viewModel.uiState.value.error)
        }

    // ---- photo upload: optimistic pendingImageUrl + rollback ----

    @Test
    fun photoPicked_success_setsPendingImageThenResolvesToUploadedUrl() =
        runTest {
            val image = DeviceImage(bytes = byteArrayOf(1, 2, 3), contentType = "image/png", localUri = "file://local/photo.png")
            val viewModel = buildViewModel(profileApi = uploadApi(resultImageUrl = "https://cdn.test/new.png"))

            viewModel.uiState.test {
                awaitItem() // already-settled state from construction

                viewModel.onIntent(ProfileIntent.PhotoPicked(image))

                val pending = awaitItem()
                assertEquals(image.localUri, pending.pendingImageUrl)
                assertTrue(pending.isUploadingImage)
                assertNull(pending.error)

                val resolved = awaitItem()
                assertFalse(resolved.isUploadingImage)
                assertEquals("https://cdn.test/new.png", resolved.imageUrl)
                // pendingImageUrl is intentionally left in place on success (see ProfileUiState doc)
                // to avoid a flicker back to the old picture before the new one is fetched.
                assertEquals(image.localUri, resolved.pendingImageUrl)
            }
        }

    @Test
    fun photoPicked_apiFailure_rollsBackPendingImageAndSetsError() =
        runTest {
            val image = DeviceImage(bytes = byteArrayOf(1, 2, 3), contentType = "image/png", localUri = "file://local/photo.png")
            val viewModel = buildViewModel(profileApi = uploadApi(shouldFail = true))

            viewModel.uiState.test {
                val initial = awaitItem()

                viewModel.onIntent(ProfileIntent.PhotoPicked(image))

                val pending = awaitItem()
                assertEquals(image.localUri, pending.pendingImageUrl)
                assertTrue(pending.isUploadingImage)

                val failed = awaitItem()
                assertFalse(failed.isUploadingImage)
                assertNull(failed.pendingImageUrl)
                assertEquals(ProfileErrorReason.UPLOAD_PHOTO_FAILED, failed.error)
                // imageUrl (the confirmed picture) is untouched by a failed upload.
                assertEquals(initial.imageUrl, failed.imageUrl)
            }
        }

    @Test
    fun photoPicked_nullImage_doesNothing() =
        runTest {
            val viewModel = buildViewModel()
            val before = viewModel.uiState.value

            viewModel.onIntent(ProfileIntent.PhotoPicked(image = null))

            assertEquals(before, viewModel.uiState.value)
        }

    // ---- friend accept / decline / remove ----

    @Test
    fun acceptFriendRequest_success_upsertsFriendshipAsAccepted() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi())

            viewModel.onIntent(ProfileIntent.AcceptFriendRequest(friendId = "f1"))
            friendDao.callsFlow.first { it.isNotEmpty() }

            assertTrue(friendDao.calls.contains("upsert(me, f1, ${Status.ACCEPTED})"))
        }

    @Test
    fun acceptFriendRequest_apiFailure_setsError() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi(actionsShouldFail = true))

            viewModel.onIntent(ProfileIntent.AcceptFriendRequest(friendId = "f1"))
            viewModel.uiState.first { it.error != null }

            assertEquals(ProfileErrorReason.ACCEPT_FRIEND_REQUEST_FAILED, viewModel.uiState.value.error)
        }

    @Test
    fun declineFriendRequest_success_deletesFriendshipLocally() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi())

            viewModel.onIntent(ProfileIntent.DeclineFriendRequest(friendId = "f1"))
            friendDao.callsFlow.first { it.isNotEmpty() }

            assertTrue(friendDao.calls.contains("delete(me, f1)"))
        }

    @Test
    fun declineFriendRequest_apiFailure_setsError() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi(actionsShouldFail = true))

            viewModel.onIntent(ProfileIntent.DeclineFriendRequest(friendId = "f1"))
            viewModel.uiState.first { it.error != null }

            assertEquals(ProfileErrorReason.DECLINE_FRIEND_REQUEST_FAILED, viewModel.uiState.value.error)
        }

    @Test
    fun viewFriend_setsRemoveFriendAlertFromCurrentFriendsList() =
        runTest {
            val friendDao = FakeProfileFriendDao().apply {
                friendsFlow.value = listOf(UserEntity(id = "f1", name = "Friend One", imageUrl = null))
            }
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi())

            viewModel.onIntent(ProfileIntent.ViewFriend(friendId = "f1"))

            assertEquals(
                "f1",
                viewModel.uiState.value.removeFriendAlert
                    ?.id,
            )
        }

    @Test
    fun dismissRemoveFriendAlert_clearsAlertWithoutRemoving() =
        runTest {
            val friendDao = FakeProfileFriendDao().apply {
                friendsFlow.value = listOf(UserEntity(id = "f1", name = "Friend One", imageUrl = null))
            }
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi())
            viewModel.onIntent(ProfileIntent.ViewFriend(friendId = "f1"))

            viewModel.onIntent(ProfileIntent.DismissRemoveFriendAlert)

            assertNull(viewModel.uiState.value.removeFriendAlert)
            assertTrue(friendDao.calls.isEmpty())
        }

    @Test
    fun removeFriendClicked_success_removesFriendAndClosesAlertWithoutError() =
        runTest {
            val friendDao = FakeProfileFriendDao().apply {
                friendsFlow.value = listOf(UserEntity(id = "f1", name = "Friend One", imageUrl = null))
            }
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi())
            viewModel.onIntent(ProfileIntent.ViewFriend(friendId = "f1"))

            viewModel.onIntent(ProfileIntent.RemoveFriendClicked)
            friendDao.callsFlow.first { it.isNotEmpty() }

            assertNull(viewModel.uiState.value.removeFriendAlert)
            assertNull(viewModel.uiState.value.error)
            assertTrue(friendDao.calls.contains("delete(me, f1)"))
        }

    @Test
    fun removeFriendClicked_apiFailure_setsErrorButStillClosesAlert() =
        runTest {
            val friendDao = FakeProfileFriendDao().apply {
                friendsFlow.value = listOf(UserEntity(id = "f1", name = "Friend One", imageUrl = null))
            }
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi(actionsShouldFail = true))
            viewModel.onIntent(ProfileIntent.ViewFriend(friendId = "f1"))

            viewModel.onIntent(ProfileIntent.RemoveFriendClicked)
            viewModel.uiState.first { it.error != null }

            assertNull(viewModel.uiState.value.removeFriendAlert)
            assertEquals(ProfileErrorReason.REMOVE_FRIEND_FAILED, viewModel.uiState.value.error)
            assertEquals(
                listOf(
                    "delete(me, f1)",
                    "upsert(me, f1, ${Status.ACCEPTED})",
                ),
                friendDao.calls,
            )
        }

    @Test
    fun removeFriendClicked_withNoAlertSet_isNoOp() =
        runTest {
            val friendDao = FakeProfileFriendDao()
            val viewModel = buildViewModel(friendDao = friendDao, friendsApi = friendApi())

            viewModel.onIntent(ProfileIntent.RemoveFriendClicked)

            assertTrue(friendDao.calls.isEmpty())
        }

    // ---- logout confirmation flow ----

    @Test
    fun logoutClicked_showsConfirmationAlert() =
        runTest {
            val viewModel = buildViewModel()

            viewModel.onIntent(ProfileIntent.LogoutClicked)

            assertTrue(viewModel.uiState.value.showLogoutAlert)
        }

    @Test
    fun dismissLogoutAlert_hidesAlertWithoutLoggingOut() =
        runTest {
            val viewModel = buildViewModel()
            viewModel.onIntent(ProfileIntent.LogoutClicked)

            viewModel.effects.test {
                viewModel.onIntent(ProfileIntent.DismissLogoutAlert)
                expectNoEvents()
            }
            assertFalse(viewModel.uiState.value.showLogoutAlert)
        }

    @Test
    fun confirmLogout_hidesAlertAndEmitsLogoutEffect() =
        runTest {
            val viewModel = buildViewModel()
            viewModel.onIntent(ProfileIntent.LogoutClicked)

            viewModel.effects.test {
                viewModel.onIntent(ProfileIntent.ConfirmLogout)
                assertEquals(ProfileEffect.Logout, awaitItem())
            }
            assertFalse(viewModel.uiState.value.showLogoutAlert)
        }

    @Test
    fun addFriend_emitsNavigateToAddFriendEffect() =
        runTest {
            val viewModel = buildViewModel()

            viewModel.effects.test {
                viewModel.onIntent(ProfileIntent.AddFriend)
                assertEquals(ProfileEffect.NavigateToAddFriend, awaitItem())
            }
        }

    // ---- fixture builders ----

    private fun artistDto(
        id: String,
        name: String,
    ) = ArtistDto(id = id, name = name, bio = null, imageUrl = null, favoriteCount = 0, isFavorited = false, tags = null)

    private fun jsonClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }

    private fun api(engine: MockEngine): SzigetApiService = SzigetApiService(client = jsonClient(engine), baseUrl = "https://unused.test")

    private fun successApi(getUsers: List<UserDto>): SzigetApiService =
        api(MockEngine { respond(json.encodeToString(getUsers), headers = jsonHeaders) })

    private fun failingApi(): SzigetApiService = api(MockEngine { respondError(HttpStatusCode.InternalServerError) })

    private fun friendApi(
        friends: List<UserDto> = emptyList(),
        friendRequests: List<UserDto> = emptyList(),
        actionsShouldFail: Boolean = false,
    ): SzigetApiService =
        api(
            MockEngine { request ->
                val path = request.url.encodedPath
                when {
                    path.endsWith("/friends/requests") -> {
                        respond(json.encodeToString(friendRequests), headers = jsonHeaders)
                    }

                    path.endsWith("/friends/sent") -> {
                        respond(json.encodeToString(emptyList<UserDto>()), headers = jsonHeaders)
                    }

                    path.endsWith("/friends/favorites") -> {
                        respond(json.encodeToString(emptyList<ArtistFriendsFavoritedDto>()), headers = jsonHeaders)
                    }

                    path.endsWith("/friends") && request.method == HttpMethod.Get -> {
                        respond(json.encodeToString(friends), headers = jsonHeaders)
                    }

                    actionsShouldFail -> {
                        respondError(HttpStatusCode.InternalServerError)
                    }

                    else -> {
                        respondOk()
                    }
                }
            },
        )

    private fun artistApi(artists: List<ArtistDto> = emptyList()): SzigetApiService =
        api(MockEngine { respond(json.encodeToString(artists), headers = jsonHeaders) })

    private fun uploadApi(
        resultImageUrl: String? = "https://cdn.test/new.png",
        shouldFail: Boolean = false,
    ): SzigetApiService =
        api(
            MockEngine {
                if (shouldFail) {
                    respondError(HttpStatusCode.InternalServerError)
                } else {
                    respond(
                        json.encodeToString(UserDto(id = currentUser.id, name = currentUser.name, imageUrl = resultImageUrl)),
                        headers = jsonHeaders,
                    )
                }
            },
        )

    /**
     * Builds a [ProfileViewModel] wired to real repositories/fake DAOs, driving
     * [UsersSyncService]'s fetch to a terminal state *before* constructing the view model (see
     * class doc) so `awaitSuccessfulSync()` in `init` resolves deterministically.
     */
    private suspend fun buildViewModel(
        friendDao: FriendDao = FakeProfileFriendDao(),
        artistDao: ArtistDao = FakeProfileArtistDao(),
        userDao: UserDao = FakeProfileUserDao(UserEntity(id = currentUser.id, name = currentUser.name, imageUrl = currentUser.imageUrl)),
        friendsApi: SzigetApiService = friendApi(),
        artistsApi: SzigetApiService = artistApi(),
        syncApi: SzigetApiService = successApi(getUsers = listOf(UserDto(currentUser.id, currentUser.name, currentUser.imageUrl))),
        profileApi: SzigetApiService = uploadApi(),
    ): ProfileViewModel {
        val settings = MapSettings()
        val friendRepository = FriendRepository(api = friendsApi, friendDao = friendDao, userDao = userDao, settings = settings)
        val artistRepository = ArtistRepository(api = artistsApi, dao = artistDao, settings = settings)
        val userRepository = UserRepository(dao = userDao)
        val usersSyncService = UsersSyncService(userRepository)

        usersSyncService.fetchAllUsers(syncApi)
        usersSyncService.status.first { it is UsersSyncService.SyncStatus.Success || it is UsersSyncService.SyncStatus.Failure }

        val currentUserProvider = CurrentUserProvider()
        currentUserProvider.set(currentUser)

        val viewModel = ProfileViewModel(
            friendRepository = friendRepository,
            artistRepository = artistRepository,
            userRepository = userRepository,
            usersSyncService = usersSyncService,
            getLikedArtistCountUseCase = GetLikedArtistCountUseCase(artistRepository),
            api = profileApi,
            currentUserProvider = currentUserProvider,
        )
        // friendRepository.refresh()/artistRepository.refresh() (triggered from `init` on a
        // successful sync) genuinely suspend on the underlying Ktor call, so `init`'s launch is
        // not guaranteed to have finished by the time the constructor returns. Wait for real
        // completion here (rather than reading `.value` immediately) so every test starts from a
        // fully-settled view model instead of racing that in-flight refresh.
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    /**
     * Manually-driven friends/friendRequests flows; friendship mutations are recorded (not
     * replayed) into [calls]. [calls] is backed by a [MutableStateFlow] rather than a plain list
     * so tests can suspend-wait (via [callsFlow]) for an in-flight optimistic-write-then-rollback
     * sequence to actually finish — those calls happen inside a real (suspending) Ktor request,
     * which is not guaranteed to complete synchronously within the launching coroutine.
     */
    private class FakeProfileFriendDao : FriendDao {
        private val _calls = MutableStateFlow<List<String>>(emptyList())
        val calls: List<String> get() = _calls.value
        val callsFlow: Flow<List<String>> = _calls
        val friendsFlow = MutableStateFlow<List<UserEntity>>(emptyList())
        val friendRequestsFlow = MutableStateFlow<List<UserEntity>>(emptyList())

        private fun recordCall(entry: String) {
            _calls.update { it + entry }
        }

        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {
            friendships.forEach { recordCall("upsert(${it.userId}, ${it.friendId}, ${it.status})") }
        }

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {
            recordCall("delete($userId, $friendId)")
        }

        override suspend fun deleteFriendshipsNotIn(
            userId: String,
            friendIds: List<String>,
        ) {
            // Unused in this test.
        }

        override suspend fun deleteAllFriendships() {
            // Unused in this test.
        }

        override suspend fun getArtistIdsFriendsFavorited(): List<String> = emptyList()

        override suspend fun getAllArtistFriendFavorites(): List<ArtistFriendFavoritedEntity> = emptyList()

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>) {
            // Unused in this test.
        }

        override suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>) {
            // Unused in this test.
        }

        override suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>) {
            // Unused in this test.
        }

        override suspend fun deleteAllArtistFriendFavorited() {
            // Unused in this test.
        }

        override suspend fun deleteStaleFavoritesForArtist(
            artistId: String,
            activeFriendIds: List<String>,
        ) {
            // Unused in this test.
        }

        override suspend fun deleteStaleArtistsFromArtistFriendFavorites(artistIds: List<String>) {
            // Unused in this test.
        }

        override fun observeFriends(): Flow<List<UserEntity>> = friendsFlow

        override fun observeFriendRequests(): Flow<List<UserEntity>> = friendRequestsFlow

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = MutableStateFlow(emptyList())

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = MutableStateFlow(emptyList())
    }

    private class FakeProfileArtistDao(
        private val artistsFlow: MutableStateFlow<List<ArtistEntity>> = MutableStateFlow(emptyList()),
    ) : ArtistDao {
        val upsertAllCalls = mutableListOf<List<ArtistEntity>>()

        override fun observeAll(): Flow<List<ArtistEntity>> = artistsFlow

        override fun observeById(id: String): Flow<ArtistEntity?> = MutableStateFlow(null)

        override fun observeFavorites(): Flow<List<ArtistEntity>> = MutableStateFlow(emptyList())

        override suspend fun upsertAll(artists: List<ArtistEntity>) {
            upsertAllCalls.add(artists)
            artistsFlow.value = artists
        }

        override suspend fun setFavorited(
            id: String,
            isFavorited: Boolean,
        ) {
            // Unused in this test.
        }

        override fun searchByName(query: String): Flow<List<ArtistEntity>> = MutableStateFlow(emptyList())

        override suspend fun deleteAll() {
            // Unused in this test.
        }
    }

    private class FakeProfileUserDao(
        private var currentUserEntity: UserEntity,
    ) : UserDao {
        override fun observeAll(): Flow<List<UserEntity>> = MutableStateFlow(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = MutableStateFlow(null)

        override suspend fun upsertAll(users: List<UserEntity>) {
            users.find { it.id == currentUserEntity.id }?.let { currentUserEntity = it }
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {
            // Unused in this test.
        }

        override suspend fun getCurrentUser(): UserEntity = currentUserEntity

        override fun observeCurrentUser(): Flow<UserEntity?> = MutableStateFlow(currentUserEntity)

        override suspend fun clearCurrentUser() {
            // Unused in this test.
        }

        override suspend fun deleteAll() {
            // Unused in this test.
        }
    }
}
