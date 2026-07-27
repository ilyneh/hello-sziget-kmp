package com.ilyne.helloszigetkmp.presentation.feature.artistdetail

import app.cash.turbine.test
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.core.db.dao.StageDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.presentation.util.LoadStatus
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNull

/**
 * [ArtistDetailViewModel.load] should transition through loading/success/error states as
 * [ArtistRepository.observeArtist] emits, and it should guard against redundant reloads for the
 * same artist id via the `loadedArtistId` cache while still reloading for a different id.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_transitionsFromLoadingToSuccessWithArtist() =
        runTest {
            val fakeDao = FakeArtistDao()
            val viewModel = ArtistDetailViewModel(artistRepository(fakeDao), friendRepository(), scheduleRepository())

            viewModel.uiState.test {
                assertEquals(ArtistDetailUiState(), awaitItem())

                viewModel.load("artist-1")

                fakeDao.emit("artist-1", entity("artist-1", "Artist One"))

                val loaded = awaitItem()
                assertIs<LoadStatus.Success>(loaded.status)
                assertEquals("artist-1", loaded.artist?.id)
                assertEquals("Artist One", loaded.artist?.name)
            }
        }

    @Test
    fun load_sameIdAgain_isNoOpAndDoesNotRetrigger() =
        runTest {
            val fakeDao = FakeArtistDao()
            var observeCallCount = 0
            val trackingDao =
                object : ArtistDao by fakeDao {
                    override fun observeById(id: String): Flow<ArtistEntity?> {
                        observeCallCount++
                        return fakeDao.observeById(id)
                    }
                }
            val viewModel = ArtistDetailViewModel(artistRepository(trackingDao), friendRepository(), scheduleRepository())

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.load("artist-1")
                fakeDao.emit("artist-1", entity("artist-1", "Artist One"))
                awaitItem() // success

                assertEquals(1, observeCallCount)

                viewModel.load("artist-1")

                // no further emission from the no-op call
                expectNoEvents()
                assertEquals(1, observeCallCount)
            }
        }

    @Test
    fun load_differentId_retriggersLoad() =
        runTest {
            val fakeDao = FakeArtistDao()
            val viewModel = ArtistDetailViewModel(artistRepository(fakeDao), friendRepository(), scheduleRepository())

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.load("artist-1")
                fakeDao.emit("artist-1", entity("artist-1", "Artist One"))
                val first = awaitItem()
                assertEquals("artist-1", first.artist?.id)

                viewModel.load("artist-2")
                fakeDao.emit("artist-2", entity("artist-2", "Artist Two"))
                val second = awaitItem()
                assertEquals("artist-2", second.artist?.id)
                assertEquals("Artist Two", second.artist?.name)
            }
        }

    @Test
    fun load_repositoryThrows_setsErrorState() =
        runTest {
            val throwingDao =
                object : ArtistDao by FakeArtistDao() {
                    override fun observeById(id: String): Flow<ArtistEntity?> =
                        kotlinx.coroutines.flow.flow { throw IllegalStateException("boom") }
                }
            val viewModel = ArtistDetailViewModel(artistRepository(throwingDao), friendRepository(), scheduleRepository())

            viewModel.uiState.test {
                assertEquals(ArtistDetailUiState(), awaitItem())

                viewModel.load("artist-1")

                val errored = awaitItem()
                val status = assertIs<LoadStatus.Error<*>>(errored.status)
                assertEquals("boom", status.reason)
                assertNull(errored.artist)
            }
        }

    @Test
    fun load_populatesFriendsFavoritedForMatchingArtist() =
        runTest {
            val fakeDao = FakeArtistDao()
            val friendsFavoritedFlow =
                MutableStateFlow(
                    listOf(
                        ArtistFriendsFavoritedSummary(
                            artist = entity("artist-1", "Artist One"),
                            friends = listOf(UserEntity(id = "friend-1", name = "Friend One", imageUrl = null)),
                        ),
                    ),
                )
            val viewModel =
                ArtistDetailViewModel(
                    artistRepository(fakeDao),
                    friendRepository(friendsFavoritedFlow),
                    scheduleRepository(),
                )

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.load("artist-1")
                fakeDao.emit("artist-1", entity("artist-1", "Artist One"))

                val loaded = awaitItem()
                assertEquals(listOf("friend-1"), loaded.friendsFavorited.map { it.id })
            }
        }

    @Test
    fun toggleFavorite_togglesThroughArtistRepository() =
        runTest {
            val fakeDao = FakeArtistDao()
            val viewModel = ArtistDetailViewModel(artistRepository(fakeDao), friendRepository(), scheduleRepository())

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.load("artist-1")
                fakeDao.emit("artist-1", entity("artist-1", "Artist One", isFavorited = false))
                val loaded = awaitItem()
                assertEquals(false, loaded.artist?.isFavorited)

                viewModel.toggleFavorite()

                val toggled = awaitItem()
                assertEquals(true, toggled.artist?.isFavorited)
            }
        }

    private fun entity(
        id: String,
        name: String,
        isFavorited: Boolean = false,
    ) = ArtistEntity(
        id = id,
        name = name,
        bio = null,
        imageUrl = null,
        isFavorited = isFavorited,
        tags = null,
    )

    private fun artistRepository(dao: ArtistDao): ArtistRepository {
        val api =
            SzigetApiService(
                client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
                baseUrl = "https://unused.test",
            )
        return ArtistRepository(api = api, dao = dao, settings = MapSettings())
    }

    private fun friendRepository(
        favoritedFlow: MutableStateFlow<List<ArtistFriendsFavoritedSummary>> = MutableStateFlow(emptyList()),
    ): FriendRepository {
        val api =
            SzigetApiService(
                client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
                baseUrl = "https://unused.test",
            )
        return FriendRepository(
            api = api,
            friendDao = FakeFriendDao(favoritedFlow),
            userDao = FakeUserDao(),
            settings = MapSettings(),
        )
    }

    private fun scheduleRepository(): ScheduleRepository {
        val api =
            SzigetApiService(
                client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
                baseUrl = "https://unused.test",
            )
        return ScheduleRepository(
            api = api,
            setTimeDao = FakeSetTimeDao(),
            stageDao = FakeStageDao(),
            artistDao = FakeArtistDao(),
            settings = MapSettings(),
        )
    }

    private class FakeSetTimeDao : SetTimeDao {
        override fun observeAll(): Flow<List<SetTimeEntity>> = flowOf(emptyList())

        override fun observeFavorites(): Flow<List<SetTimeWithArtistStageSummary>> = flowOf(emptyList())

        override fun observeByDay(
            dayStartMillis: Long,
            dayEndMillis: Long,
        ): Flow<List<SetTimeEntity>> = flowOf(emptyList())

        override fun observeByArtist(artistId: String): Flow<List<SetTimeEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(setTimes: List<SetTimeEntity>) {}

        override fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> = flowOf(SetTimeDao.SetTimeRange(0L, 0L))
    }

    private class FakeStageDao : StageDao {
        override fun observeAll(): Flow<List<StageEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(stages: List<StageEntity>) {}
    }

    /**
     * Fake [ArtistDao] whose [observeById] emissions are driven per-id via [emit]. Uses a
     * no-replay [MutableSharedFlow] (rather than a [kotlinx.coroutines.flow.MutableStateFlow])
     * so that subscribing via `observeById` does not itself produce an initial `null` emission -
     * tests only see emissions they explicitly trigger via [emit].
     */
    private class FakeArtistDao : ArtistDao {
        private val flows = mutableMapOf<String, MutableSharedFlow<ArtistEntity?>>()
        private val lastEmitted = mutableMapOf<String, ArtistEntity?>()

        suspend fun emit(
            id: String,
            entity: ArtistEntity?,
        ) {
            lastEmitted[id] = entity
            flowFor(id).emit(entity)
        }

        private fun flowFor(id: String) = flows.getOrPut(id) { MutableSharedFlow(replay = 0, extraBufferCapacity = 1) }

        override fun observeAll(): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<ArtistEntity?> = flowFor(id)

        override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(artists: List<ArtistEntity>) {}

        override suspend fun setFavorited(
            id: String,
            isFavorited: Boolean,
        ) {
            // Mirrors ArtistDao's real upsert-then-notify semantics closely enough for
            // ArtistDetailViewModel's toggleFavorite() test to observe the flip via observeById,
            // without needing a full in-memory table like FakeArtistDao in DiscoverViewModelTest.
            val updated = lastEmitted[id]?.copy(isFavorited = isFavorited) ?: return
            emit(id, updated)
        }

        override fun searchByName(query: String): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override suspend fun deleteAll() {}
    }

    private class FakeFriendDao(
        private val favoritedFlow: MutableStateFlow<List<ArtistFriendsFavoritedSummary>>,
    ) : FriendDao {
        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {}

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {}

        override suspend fun deleteFriendshipsNotIn(
            userId: String,
            friendIds: List<String>,
        ) {}

        override suspend fun deleteAllFriendships() {}

        override suspend fun getArtistIdsFriendsFavorited(): List<String> = emptyList()

        override suspend fun getAllArtistFriendFavorites(): List<ArtistFriendFavoritedEntity> = emptyList()

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>) {}

        override suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>) {}

        override suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>) {}

        override suspend fun deleteAllArtistFriendFavorited() {}

        override suspend fun deleteStaleFavoritesForArtist(
            artistId: String,
            activeFriendIds: List<String>,
        ) {}

        override suspend fun deleteStaleArtistsFromArtistFriendFavorites(artistIds: List<String>) {}

        override fun observeFriends(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = favoritedFlow
    }

    private class FakeUserDao : UserDao {
        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>) {}

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {}

        override suspend fun getCurrentUser(): UserEntity? = UserEntity(id = "me", name = "Me", imageUrl = null)

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(UserEntity(id = "me", name = "Me", imageUrl = null))

        override suspend fun clearCurrentUser() {}

        override suspend fun deleteAll() {}
    }
}
