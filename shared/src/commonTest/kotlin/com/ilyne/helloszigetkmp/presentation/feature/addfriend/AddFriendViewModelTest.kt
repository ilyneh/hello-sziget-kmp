package com.ilyne.helloszigetkmp.presentation.feature.addfriend

import app.cash.turbine.test
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.domain.model.User
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [AddFriendViewModel.observeResults] combines the local users/friends/sent/received-requests
 * tables with the search query to produce a filtered, status-tagged result list: the current user
 * and existing friends are excluded, the query filter is case-insensitive, and each remaining user
 * is tagged with the [FriendshipStatus] derived from the sent/received request sets.
 *
 * [AddFriendViewModel.onIntent] handling of [AddFriendIntent.SendFriendRequest] and
 * [AddFriendIntent.AcceptFriendRequest] both clear any prior error on success and surface a
 * distinct error message on failure - mirroring the recent accept/decline friend request bugfix,
 * this also verifies that a failure only ever produces the intended error status and never leaves
 * a stale success/idle state hanging around.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddFriendViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val currentUserId = "me"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeResults_excludesCurrentUserAndExistingFriends() =
        runTest {
            val userDao = FakeUserDao(
                users = listOf(
                    user(currentUserId, "Me"),
                    user("friend-1", "Friend One"),
                    user("stranger-1", "Stranger One"),
                ),
            )
            val friendDao = FakeFriendDao(friends = listOf(user("friend-1", "Friend One")))
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(listOf("stranger-1"), state.results.map { it.user.id })
            }
        }

    @Test
    fun observeResults_derivesNoneStatus_whenNoRequestExists() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(FriendshipStatus.NONE, state.results.single { it.user.id == "u1" }.status)
            }
        }

    @Test
    fun observeResults_derivesRequestSentStatus() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao(sentRequests = listOf(user("u1", "Uno")))
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(FriendshipStatus.REQUEST_SENT, state.results.single { it.user.id == "u1" }.status)
            }
        }

    @Test
    fun observeResults_derivesRequestReceivedStatus() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao(receivedRequests = listOf(user("u1", "Uno")))
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(FriendshipStatus.REQUEST_RECEIVED, state.results.single { it.user.id == "u1" }.status)
            }
        }

    @Test
    fun observeResults_existingFriend_isExcludedRatherThanTaggedFriendStatus() =
        runTest {
            // Friends are filtered out of `results` entirely before status derivation runs, so
            // FriendshipStatus.FRIEND is never actually produced by observeResults - it only
            // exists as a status a caller could apply to an already-known friend elsewhere.
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao(friends = listOf(user("u1", "Uno")))
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.results.none { it.user.id == "u1" })
            }
        }

    @Test
    fun searchQueryChanged_filtersCaseInsensitively() =
        runTest {
            val userDao = FakeUserDao(
                users = listOf(
                    user(currentUserId, "Me"),
                    user("u1", "Alice Wonderland"),
                    user("u2", "Bob Builder"),
                ),
            )
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                awaitItem() // initial, unfiltered

                // SearchQueryChanged updates the `searchQuery` MutableStateFlow (which the
                // combine recomputes `results` from) and the uiState's `searchQuery` field as
                // two separate sequential state writes, so two emissions land here.
                viewModel.onIntent(AddFriendIntent.SearchQueryChanged("ALICE"))
                val filteredResults = awaitItem()
                assertEquals(listOf("u1"), filteredResults.results.map { it.user.id })

                val updatedQuery = awaitItem()
                assertEquals("ALICE", updatedQuery.searchQuery)
            }
        }

    @Test
    fun searchQueryChanged_blankQuery_returnsAllEligibleResults() =
        runTest {
            val userDao = FakeUserDao(
                users = listOf(
                    user(currentUserId, "Me"),
                    user("u1", "Alice Wonderland"),
                    user("u2", "Bob Builder"),
                ),
            )
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao)

            viewModel.uiState.test {
                awaitItem() // initial

                // Each SearchQueryChanged intent produces two sequential emissions: one from the
                // `combine` recomputing `results` off the new query, one from the explicit
                // `searchQuery` field update.
                viewModel.onIntent(AddFriendIntent.SearchQueryChanged("bob"))
                awaitItem()
                awaitItem()

                viewModel.onIntent(AddFriendIntent.SearchQueryChanged(""))
                awaitItem()
                val cleared = awaitItem()
                assertEquals(setOf("u1", "u2"), cleared.results.map { it.user.id }.toSet())
            }
        }

    @Test
    fun sendFriendRequest_success_setsIdleStatus() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao, apiSucceeds = true)

            // Status starts as Idle, and a successful send also sets Idle - `copy(status = Idle)`
            // is structurally equal to the existing state, so the StateFlow (distinct-until-
            // changed) never emits for this path. Assert on the settled `.value` instead of
            // observing a Turbine emission that will never arrive.
            viewModel.onIntent(AddFriendIntent.SendFriendRequest("u1"))

            assertIs<AddFriendUiState.Status.Idle>(viewModel.uiState.value.status)
            assertEquals(listOf("upsert(me, u1, SENT)"), friendDao.calls)
        }

    @Test
    fun sendFriendRequest_apiFailure_setsErrorStatus() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao, apiSucceeds = false)

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.onIntent(AddFriendIntent.SendFriendRequest("u1"))

                val errored = awaitItem()
                val status = assertIs<AddFriendUiState.Status.Error>(errored.status)
                assertEquals("Failed to send friend request", status.message)
            }
        }

    @Test
    fun acceptFriendRequest_success_setsIdleStatus() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao, apiSucceeds = true)

            // As with the send-request success path, Idle -> Idle is not a state change, so this
            // asserts on the settled `.value` rather than waiting on a Turbine emission.
            viewModel.onIntent(AddFriendIntent.AcceptFriendRequest("u1"))

            assertIs<AddFriendUiState.Status.Idle>(viewModel.uiState.value.status)
            assertEquals(listOf("upsert(me, u1, ACCEPTED)"), friendDao.calls)
        }

    @Test
    fun acceptFriendRequest_apiFailure_setsErrorStatus() =
        runTest {
            val userDao = FakeUserDao(users = listOf(user(currentUserId, "Me"), user("u1", "Uno")))
            val friendDao = FakeFriendDao()
            val viewModel = viewModel(userDao, friendDao, apiSucceeds = false)

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.onIntent(AddFriendIntent.AcceptFriendRequest("u1"))

                val errored = awaitItem()
                val status = assertIs<AddFriendUiState.Status.Error>(errored.status)
                assertEquals("Failed to accept friend request", status.message)
            }
        }

    private fun user(
        id: String,
        name: String,
    ) = User(id = id, name = name, imageUrl = null)

    private fun viewModel(
        userDao: FakeUserDao,
        friendDao: FakeFriendDao,
        apiSucceeds: Boolean = true,
    ): AddFriendViewModel {
        // MockEngine hops requests onto its own dispatcher (real Dispatchers.IO by default) rather
        // than running on the caller's dispatcher, so a fire-and-forget viewModelScope.launch that
        // awaits a request (see refreshFriendsFavoritedInBackground) can still be in flight on that
        // real thread after this test method returns and tearDown() calls Dispatchers.resetMain(),
        // crashing with "Dispatchers.Main ... test dispatcher was unset" - misattributed to
        // whichever test runs next. Pin it to this test's Main dispatcher so requests resolve
        // synchronously within the test instead of on a real background thread.
        val engine = MockEngine(
            MockEngineConfig().apply {
                this.dispatcher = this@AddFriendViewModelTest.dispatcher
                requestHandlers.add { if (apiSucceeds) respondOk() else throw ApiFailureException() }
            },
        )
        val api = SzigetApiService(client = HttpClient(engine), baseUrl = "https://unused.test")
        val friendRepository = FriendRepository(
            api = api,
            friendDao = friendDao,
            userDao = userDao,
            settings = MapSettings(),
        )
        val userRepository = UserRepository(dao = userDao)
        val currentUserProvider = CurrentUserProvider().apply { set(user(currentUserId, "Me")) }
        return AddFriendViewModel(
            friendRepository = friendRepository,
            userRepository = userRepository,
            currentUserProvider = currentUserProvider,
            backgroundDispatcher = Dispatchers.Main,
        )
    }

    private class ApiFailureException : Exception("simulated api failure")

    /**
     * Fake [UserDao] backed by a [MutableStateFlow] so [observeAll] emits synchronously. Only
     * the members [AddFriendViewModel] actually reaches (`observeAll` via [UserRepository], and
     * `getCurrentUser`/`upsertAll` via [FriendRepository.refresh]'s background sync) are
     * implemented meaningfully; the rest throw since they're unused by this ViewModel's paths.
     */
    private class FakeUserDao(
        users: List<User> = emptyList(),
    ) : UserDao {
        private val usersFlow = MutableStateFlow(users.map { UserEntity(id = it.id, name = it.name, imageUrl = it.imageUrl) })

        override fun observeAll(): Flow<List<UserEntity>> = usersFlow

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(usersFlow.value.find { it.id == id })

        override suspend fun upsertAll(users: List<UserEntity>) {
            // Background refresh path only - not asserted on in these tests.
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity): Unit = throw NotImplementedError("unused in this test")

        override suspend fun getCurrentUser(): UserEntity? = usersFlow.value.find { it.id == "me" }

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(getCurrentUserOrNull())

        private fun getCurrentUserOrNull(): UserEntity? = usersFlow.value.find { it.id == "me" }

        override suspend fun clearCurrentUser(): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteAll(): Unit = throw NotImplementedError("unused in this test")
    }

    /**
     * Fake [FriendDao] backed by [MutableStateFlow]s for the three observe* queries the
     * ViewModel's `combine` depends on, plus call-tracking for the upsert/delete calls
     * exercised via [FriendRepository.sendFriendRequest]/[FriendRepository.acceptFriendRequest].
     */
    private class FakeFriendDao(
        friends: List<User> = emptyList(),
        receivedRequests: List<User> = emptyList(),
        sentRequests: List<User> = emptyList(),
    ) : FriendDao {
        val calls = mutableListOf<String>()
        private val friendsFlow = MutableStateFlow(friends.map { it.toEntity() })
        private val requestsFlow = MutableStateFlow(receivedRequests.map { it.toEntity() })
        private val sentFlow = MutableStateFlow(sentRequests.map { it.toEntity() })

        private fun User.toEntity() = UserEntity(id = id, name = name, imageUrl = imageUrl)

        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {
            friendships.forEach { calls.add("upsert(${it.userId}, ${it.friendId}, ${it.status})") }
        }

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {
            calls.add("delete($userId, $friendId)")
        }

        override suspend fun deleteFriendshipsNotIn(
            userId: String,
            friendIds: List<String>,
        ) {
            // Unused: only exercised by refresh()'s success path, which the background-sync
            // API-failure setup in this test file never reaches.
        }

        override suspend fun deleteAllFriendships(): Unit = throw NotImplementedError("unused in this test")

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

        override suspend fun deleteAllArtistFriendFavorited(): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteStaleFavoritesForArtist(
            artistId: String,
            activeFriendIds: List<String>,
        ): Unit = throw NotImplementedError("unused in this test")

        override suspend fun deleteStaleArtistsFromArtistFriendFavorites(artistIds: List<String>): Unit =
            throw NotImplementedError("unused in this test")

        override fun observeFriends(): Flow<List<UserEntity>> = friendsFlow

        override fun observeFriendRequests(): Flow<List<UserEntity>> = requestsFlow

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = sentFlow

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = flowOf(emptyList())
    }
}
