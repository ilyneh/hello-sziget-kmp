package com.ilyne.helloszigetkmp.presentation.feature.schedule

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
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterStorage
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase.ActiveFilterItem
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase.GetActiveFiltersTextUseCase
import com.ilyne.helloszigetkmp.presentation.feature.schedule.usecase.GetFilteredScheduleContentUseCase
import com.ilyne.helloszigetkmp.presentation.util.LoadStatus
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * [ScheduleViewModel] is the largest state machine in the app: it combines the persisted
 * [ScheduleFilter], the selected [SetTimeDay], set-times-for-that-day and friend-favorite flows
 * via `flatMapLatest`/`combine`, auto-corrects the selected day when it falls out of the
 * filtered day list, and has three independent try/catch error paths (initial refresh, festival
 * day loading, selected-day content loading). These tests drive it through real
 * [ScheduleRepository]/[ArtistRepository]/[FriendRepository] instances backed by fake DAOs (same
 * approach as [com.ilyne.helloszigetkmp.core.repository.ScheduleRepositoryTest] and
 * [com.ilyne.helloszigetkmp.presentation.feature.artistdetail.ArtistDetailViewModelTest]) rather
 * than mocking the repositories themselves, so the combine/flatMapLatest wiring is exercised for
 * real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val budapest = TimeZone.of("Europe/Budapest")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun millisAt(isoLocalDateTime: String): Long = LocalDateTime.parse(isoLocalDateTime).toInstant(budapest).toEpochMilliseconds()

    @Test
    fun init_loadsDaysSelectsFirstDayAndFiltersSetTimesWithDefaultFilter() =
        runTest {
            val stage = StageEntity(id = "stage-1", name = "Main Stage", description = null)
            val favoritedArtist = ArtistEntity(
                id = "artist-1",
                name = "Favorited Artist",
                bio = null,
                imageUrl = null,
                isFavorited = true,
                tags = listOf("genre-rock"),
            )
            val otherArtist = ArtistEntity(
                id = "artist-2",
                name = "Other Artist",
                bio = null,
                imageUrl = null,
                isFavorited = false,
                tags = listOf("genre-rock"),
            )
            val setTime1 = SetTimeEntity(
                id = "st-1",
                artistId = favoritedArtist.id,
                stageId = stage.id,
                startTime = millisAt("2026-08-06T20:00:00"),
                endTime = millisAt("2026-08-06T21:00:00"),
                hideEndTime = false,
            )
            val setTime2 = SetTimeEntity(
                id = "st-2",
                artistId = otherArtist.id,
                stageId = stage.id,
                startTime = millisAt("2026-08-06T21:00:00"),
                endTime = millisAt("2026-08-06T22:00:00"),
                hideEndTime = false,
            )

            val viewModel = newViewModel(
                setTimeDao = FakeSetTimeDao(
                    setTimes = listOf(setTime1, setTime2),
                    range = SetTimeDao.SetTimeRange(
                        minStart = millisAt("2026-08-06T18:00:00"),
                        maxStart = millisAt("2026-08-06T18:00:00"),
                    ),
                ),
                stageDao = FakeStageDao(listOf(stage)),
                artistDao = FakeArtistDao(listOf(favoritedArtist, otherArtist)),
            )

            val state = viewModel.uiState.value
            assertIs<LoadStatus.Success>(state.status)
            assertEquals(1, state.days.size)
            assertEquals(6, state.days.first().dateOfMonth)
            assertEquals(state.days.first(), state.selectedDay)
            assertEquals(listOf("st-1", "st-2"), state.setTimes.map { it.id })
            assertEquals(1, state.stages.size)
            // Default filter has no favorites/friends-only toggles active, so both set times show.
            assertEquals(
                true,
                state.setTimes
                    .first { it.id == "st-1" }
                    .artist
                    ?.isFavorited,
            )
            assertEquals(
                false,
                state.setTimes
                    .first { it.id == "st-2" }
                    .artist
                    ?.isFavorited,
            )
        }

    @Test
    fun applyFilter_showFavoritesOnly_filtersSetTimesToFavoritedArtistOnly() =
        runTest {
            val stage = StageEntity(id = "stage-1", name = "Main Stage", description = null)
            val favoritedArtist = ArtistEntity(
                id = "artist-1",
                name = "Favorited Artist",
                bio = null,
                imageUrl = null,
                isFavorited = true,
                tags = listOf("genre-rock"),
            )
            val otherArtist = ArtistEntity(
                id = "artist-2",
                name = "Other Artist",
                bio = null,
                imageUrl = null,
                isFavorited = false,
                tags = listOf("genre-rock"),
            )
            val setTime1 = SetTimeEntity(
                id = "st-1",
                artistId = favoritedArtist.id,
                stageId = stage.id,
                startTime = millisAt("2026-08-06T20:00:00"),
                endTime = millisAt("2026-08-06T21:00:00"),
                hideEndTime = false,
            )
            val setTime2 = SetTimeEntity(
                id = "st-2",
                artistId = otherArtist.id,
                stageId = stage.id,
                startTime = millisAt("2026-08-06T21:00:00"),
                endTime = millisAt("2026-08-06T22:00:00"),
                hideEndTime = false,
            )

            val settings = freshSettings()
            val viewModel = newViewModel(
                setTimeDao = FakeSetTimeDao(
                    setTimes = listOf(setTime1, setTime2),
                    range = SetTimeDao.SetTimeRange(
                        minStart = millisAt("2026-08-06T18:00:00"),
                        maxStart = millisAt("2026-08-06T18:00:00"),
                    ),
                ),
                stageDao = FakeStageDao(listOf(stage)),
                artistDao = FakeArtistDao(listOf(favoritedArtist, otherArtist)),
                settings = settings,
            )

            viewModel.onIntent(ScheduleIntent.ApplyFilter(ScheduleFilter(showFavorites = true)))

            assertEquals(
                listOf("st-1"),
                viewModel.uiState.value.setTimes
                    .map { it.id },
            )
            // ScheduleFilter(showFavorites = true) also keeps hideEmptyStages at its true
            // default, so activeCount() counts both of those (see ScheduleFilter.activeCount()).
            assertEquals(2, viewModel.uiState.value.activeFilterCount)

            // The filter must be persisted via the shared ScheduleFilterStorage as a side effect.
            val persisted = ScheduleFilterStorage(settings).read()
            assertTrue(persisted?.showFavorites == true)
        }

    @Test
    fun selectDay_updatesSelectedDayAndReloadsSetTimesForThatDay() =
        runTest {
            val stage = StageEntity(id = "stage-1", name = "Main Stage", description = null)
            val artist = ArtistEntity(
                id = "artist-1",
                name = "Artist",
                bio = null,
                imageUrl = null,
                isFavorited = false,
                tags = listOf("genre-rock"),
            )
            val dayOneSetTime = SetTimeEntity(
                id = "st-day-1",
                artistId = artist.id,
                stageId = stage.id,
                startTime = millisAt("2026-08-06T20:00:00"),
                endTime = millisAt("2026-08-06T21:00:00"),
                hideEndTime = false,
            )
            val dayTwoSetTime = SetTimeEntity(
                id = "st-day-2",
                artistId = artist.id,
                stageId = stage.id,
                startTime = millisAt("2026-08-07T20:00:00"),
                endTime = millisAt("2026-08-07T21:00:00"),
                hideEndTime = false,
            )

            val viewModel = newViewModel(
                setTimeDao = FakeSetTimeDao(
                    setTimes = listOf(dayOneSetTime, dayTwoSetTime),
                    range = SetTimeDao.SetTimeRange(
                        minStart = millisAt("2026-08-06T18:00:00"),
                        maxStart = millisAt("2026-08-07T18:00:00"),
                    ),
                ),
                stageDao = FakeStageDao(listOf(stage)),
                artistDao = FakeArtistDao(listOf(artist)),
            )

            val days = viewModel.uiState.value.days
            assertEquals(2, days.size)
            assertEquals(
                listOf("st-day-1"),
                viewModel.uiState.value.setTimes
                    .map { it.id },
            )

            viewModel.onIntent(ScheduleIntent.SelectDay(days[1]))

            assertEquals(days[1], viewModel.uiState.value.selectedDay)
            assertEquals(
                listOf("st-day-2"),
                viewModel.uiState.value.setTimes
                    .map { it.id },
            )
        }

    @Test
    fun selectedDayFallsOutOfFilteredDays_afterShowExtraDaysToggledOff_autoSelectsFirstRemainingDay() =
        runTest {
            val viewModel = newViewModel(
                setTimeDao = FakeSetTimeDao(
                    setTimes = emptyList(),
                    range = SetTimeDao.SetTimeRange(
                        // Aug 6 -> Aug 9: day 9 is an "extra day" (EXTRA_FESTIVAL_DAYS_OF_MONTH).
                        minStart = millisAt("2026-08-06T18:00:00"),
                        maxStart = millisAt("2026-08-09T14:00:00"),
                    ),
                ),
                stageDao = FakeStageDao(emptyList()),
                artistDao = FakeArtistDao(emptyList()),
            )

            // Default filter (showExtraDays = false) hides the extra day.
            assertEquals(
                listOf(6, 7, 8),
                viewModel.uiState.value.days
                    .map { it.dateOfMonth },
            )
            val firstDay = viewModel.uiState.value.selectedDay
            assertEquals(6, firstDay?.dateOfMonth)

            // Reveal extra days and select the extra day.
            viewModel.onIntent(ScheduleIntent.ApplyFilter(ScheduleFilter(showExtraDays = true)))
            val extraDay = viewModel.uiState.value.days
                .first { it.dateOfMonth == 9 }
            viewModel.onIntent(ScheduleIntent.SelectDay(extraDay))
            assertEquals(extraDay, viewModel.uiState.value.selectedDay)

            // Hiding extra days again should notice the selected day is no longer in the list and
            // fall back to the first remaining day instead of leaving a dangling selection.
            viewModel.onIntent(ScheduleIntent.ApplyFilter(ScheduleFilter(showExtraDays = false)))

            assertEquals(firstDay, viewModel.uiState.value.selectedDay)
            assertEquals(
                listOf(6, 7, 8),
                viewModel.uiState.value.days
                    .map { it.dateOfMonth },
            )
        }

    @Test
    fun toggleFavorite_nullArtistId_ignoresIntentWithoutThrowing() =
        runTest {
            // A null artistId happens legitimately when a set time's artist hasn't synced
            // locally yet (schedule sync finished before artist sync); tapping favorite on it
            // should be a no-op, not a crash.
            val viewModel = newViewModel()
            val statusBeforeIntent = viewModel.uiState.value.status

            viewModel.onIntent(ScheduleIntent.ToggleFavorite(artistId = null, current = false))

            assertEquals(statusBeforeIntent, viewModel.uiState.value.status)
        }

    @Test
    fun toggleFavorite_repositoryFailure_setsErrorStatus() =
        runTest {
            val artistDao = FakeArtistDao(
                listOf(
                    ArtistEntity(
                        id = "boom-artist",
                        name = "Boom",
                        bio = null,
                        imageUrl = null,
                        isFavorited = false,
                        tags = null,
                    ),
                ),
            )
            artistDao.throwOnSetFavoritedFor("boom-artist")
            val viewModel = newViewModel(artistDao = artistDao)

            viewModel.onIntent(ScheduleIntent.ToggleFavorite(artistId = "boom-artist", current = false))

            val status = assertIs<LoadStatus.Error<*>>(viewModel.uiState.value.status)
            assertEquals(ScheduleErrorReason.UPDATE_FAVORITE_FAILED, status.reason)
        }

    @Test
    fun refresh_forcedRefreshFailsBecauseUnparseableApiResponse_setsErrorStatus() =
        runTest {
            val viewModel = newViewModel()
            // Construction succeeds because the soft-refresh gate is pre-warmed as "fresh" so
            // init's non-forced refresh skips the network call entirely.
            assertIs<LoadStatus.Success>(viewModel.uiState.value.status)

            // An explicit pull-to-refresh always forces the network call, which fails here
            // because the mock engine returns a response the API DTOs can't be parsed from. The
            // ktor client engine hops off the test dispatcher internally, so the Loading ->
            // Error transition isn't synchronous like the rest of this suite - collect for it
            // via turbine instead of reading uiState.value immediately.
            viewModel.uiState.test {
                assertIs<LoadStatus.Success>(awaitItem().status)

                viewModel.refresh()

                assertIs<LoadStatus.Loading>(awaitItem().status)
                val status = assertIs<LoadStatus.Error<*>>(awaitItem().status)
                assertEquals(ScheduleErrorReason.REFRESH_FAILED, status.reason)
            }
        }

    @Test
    fun observeSetTimeDaysFailure_setsFestivalDaysErrorStatus() =
        runTest {
            val fakeSetTimeDao = FakeSetTimeDao(emptyList(), SetTimeDao.SetTimeRange(0L, 0L))
            val throwingSetTimeDao = object : SetTimeDao by fakeSetTimeDao {
                override fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> = flow { throw IllegalStateException("range boom") }
            }

            val viewModel = newViewModel(setTimeDao = throwingSetTimeDao)

            val status = assertIs<LoadStatus.Error<*>>(viewModel.uiState.value.status)
            assertEquals(ScheduleErrorReason.LOAD_DAYS_FAILED, status.reason)
        }

    @Test
    fun observeSelectedDayFailure_setsScheduleErrorStatus() =
        runTest {
            val fakeSetTimeDao = FakeSetTimeDao(
                emptyList(),
                SetTimeDao.SetTimeRange(
                    minStart = millisAt("2026-08-06T18:00:00"),
                    maxStart = millisAt("2026-08-06T18:00:00"),
                ),
            )
            val throwingSetTimeDao = object : SetTimeDao by fakeSetTimeDao {
                override fun observeByDay(
                    dayStartMillis: Long,
                    dayEndMillis: Long,
                ): Flow<List<SetTimeEntity>> = flow { throw IllegalStateException("day boom") }
            }

            val viewModel = newViewModel(setTimeDao = throwingSetTimeDao)

            val status = assertIs<LoadStatus.Error<*>>(viewModel.uiState.value.status)
            assertEquals(ScheduleErrorReason.LOAD_DAY_FAILED, status.reason)
        }

    @Test
    fun openFilter_emitsNavigateToFilterEffectWithCurrentFilter() =
        runTest {
            val viewModel = newViewModel()

            viewModel.effects.test {
                viewModel.onIntent(ScheduleIntent.ApplyFilter(ScheduleFilter(showFavorites = true)))
                viewModel.onIntent(ScheduleIntent.OpenFilter)

                val effect = assertIs<ScheduleEffect.NavigateToFilter>(awaitItem())
                assertTrue(effect.filter.showFavorites)
            }
        }

    @Test
    fun changeViewMode_updatesUiStateViewMode() =
        runTest {
            val viewModel = newViewModel()

            assertEquals(ViewMode.GRID, viewModel.uiState.value.viewMode)

            viewModel.onIntent(ScheduleIntent.ChangeViewMode(ViewMode.LIST))

            assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
        }

    @Test
    fun changeViewMode_persistsSelectionViaScheduleViewModeStorage() =
        runTest {
            val settings = freshSettings()
            val viewModel = newViewModel(settings = settings)

            viewModel.onIntent(ScheduleIntent.ChangeViewMode(ViewMode.LIST))

            assertEquals(ViewMode.LIST, ScheduleViewModeStorage(settings).read())
        }

    @Test
    fun init_seedsViewModeFromPersistedStorage() =
        runTest {
            val settings = freshSettings()
            ScheduleViewModeStorage(settings).save(ViewMode.SWIMLANE)

            val viewModel = newViewModel(settings = settings)

            assertEquals(ViewMode.SWIMLANE, viewModel.uiState.value.viewMode)
        }

    @Test
    fun init_seedsFilterFromPersistedStorage() =
        runTest {
            val settings = freshSettings()
            ScheduleFilterStorage(settings).save(ScheduleFilter(showFriendsGoing = true))

            val viewModel = newViewModel(settings = settings)

            assertTrue(
                viewModel.uiState.value.activeFilterItems
                    .contains(ActiveFilterItem.FriendsGoing),
            )
            // ScheduleFilter(showFriendsGoing = true) also keeps hideEmptyStages at its true
            // default, so activeCount() counts both of those (see ScheduleFilter.activeCount()).
            assertEquals(2, viewModel.uiState.value.activeFilterCount)
        }

    // --- test fixtures -------------------------------------------------------------------

    /**
     * Pre-warms every repository's `SoftRefreshGate` as "just fetched" so a non-forced
     * `refresh(force = false)` (as triggered by [ScheduleViewModel]'s init) skips the network
     * call entirely rather than hitting the mock API and failing to parse an empty response body.
     */
    private fun freshSettings(): Settings {
        val settings = MapSettings()
        val now = Clock.System.now().toEpochMilliseconds()
        listOf("ScheduleRepository", "ArtistRepository", "FriendRepository").forEach { key ->
            settings.putLong("SoftRefreshGate_lastFetchedAt_$key", now)
        }
        return settings
    }

    private fun mockApi(): SzigetApiService =
        SzigetApiService(
            client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
            baseUrl = "https://unused.test",
        )

    private fun newViewModel(
        setTimeDao: SetTimeDao = FakeSetTimeDao(emptyList(), SetTimeDao.SetTimeRange(0L, 0L)),
        stageDao: StageDao = FakeStageDao(emptyList()),
        artistDao: ArtistDao = FakeArtistDao(emptyList()),
        friendDao: FriendDao = FakeFriendDao(),
        userDao: UserDao = FakeUserDao(),
        settings: Settings = freshSettings(),
    ): ScheduleViewModel {
        val scheduleRepository = ScheduleRepository(
            api = mockApi(),
            setTimeDao = setTimeDao,
            stageDao = stageDao,
            artistDao = artistDao,
            settings = settings,
        )
        val artistRepository = ArtistRepository(api = mockApi(), dao = artistDao, settings = settings)
        val friendRepository = FriendRepository(api = mockApi(), friendDao = friendDao, userDao = userDao, settings = settings)

        return ScheduleViewModel(
            scheduleRepository = scheduleRepository,
            artistRepository = artistRepository,
            friendRepository = friendRepository,
            getSetTimeDaysUseCase = GetSetTimeDaysUseCase(scheduleRepository),
            getFilteredScheduleContentUseCase = GetFilteredScheduleContentUseCase(),
            getActiveFiltersTextUseCase = GetActiveFiltersTextUseCase(),
            scheduleFilterStorage = ScheduleFilterStorage(settings),
            scheduleViewModeStorage = ScheduleViewModeStorage(settings),
            backgroundDispatcher = Dispatchers.Main,
        )
    }

    private class FakeSetTimeDao(
        setTimes: List<SetTimeEntity>,
        range: SetTimeDao.SetTimeRange,
    ) : SetTimeDao {
        private val setTimesFlow = MutableStateFlow(setTimes)
        private val rangeFlow = MutableStateFlow(range)

        override fun observeByDay(
            dayStartMillis: Long,
            dayEndMillis: Long,
        ): Flow<List<SetTimeEntity>> = flowOf(setTimesFlow.value.filter { it.startTime >= dayStartMillis && it.startTime < dayEndMillis })

        override fun observeByArtist(artistId: String): Flow<List<SetTimeEntity>> =
            flowOf(setTimesFlow.value.filter { it.artistId == artistId })

        override suspend fun upsertAll(setTimes: List<SetTimeEntity>) {}

        override fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> = rangeFlow

        override fun observeFavorites(): Flow<List<SetTimeWithArtistStageSummary>> = flowOf(emptyList())
    }

    private class FakeStageDao(
        stages: List<StageEntity>,
    ) : StageDao {
        private val stagesFlow = MutableStateFlow(stages)

        override fun observeAll(): Flow<List<StageEntity>> = stagesFlow

        override suspend fun upsertAll(stages: List<StageEntity>) {}
    }

    private class FakeArtistDao(
        artists: List<ArtistEntity>,
    ) : ArtistDao {
        private val artistsFlow = MutableStateFlow(artists)
        private val throwOnSetFavoritedIds = mutableSetOf<String>()

        fun throwOnSetFavoritedFor(id: String) {
            throwOnSetFavoritedIds.add(id)
        }

        override fun observeAll(): Flow<List<ArtistEntity>> = artistsFlow

        override fun observeById(id: String): Flow<ArtistEntity?> = flowOf(artistsFlow.value.find { it.id == id })

        override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(artistsFlow.value.filter { it.isFavorited })

        override fun searchByName(query: String): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(artists: List<ArtistEntity>) {}

        override suspend fun setFavorited(
            id: String,
            isFavorited: Boolean,
        ) {
            if (id in throwOnSetFavoritedIds) {
                throw IllegalStateException("setFavorited boom for $id")
            }
            artistsFlow.update { list -> list.map { if (it.id == id) it.copy(isFavorited = isFavorited) else it } }
        }

        override suspend fun deleteAll() {}
    }

    private class FakeFriendDao(
        favorited: List<ArtistFriendsFavoritedSummary> = emptyList(),
    ) : FriendDao {
        private val favoritedFlow = MutableStateFlow(favorited)

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

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>) {}

        override suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>) {}

        override suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>) {}

        override suspend fun deleteAllArtistFriendFavorited() {}

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

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {}

        override suspend fun deleteAll() {}
    }
}
