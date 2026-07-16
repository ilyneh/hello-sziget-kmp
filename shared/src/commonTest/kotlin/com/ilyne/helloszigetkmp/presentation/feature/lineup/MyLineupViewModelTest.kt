package com.ilyne.helloszigetkmp.presentation.feature.lineup

import app.cash.turbine.test
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.core.db.dao.StageDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.util.datetime.formatDate
import com.ilyne.helloszigetkmp.util.datetime.toFestivalDate
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Clock

/**
 * [MyLineupViewModel] collects [ScheduleRepository.observeFavoriteSetTimes] and groups the
 * results by "festival date" (the 6am-to-6am day used across the app, see
 * [com.ilyne.helloszigetkmp.util.datetime.toFestivalDate]) into formatted day labels, and exposes
 * two independent try/catch paths - [MyLineupViewModel.refresh] and
 * [MyLineupViewModel.removeFavorite] - that both fall back to an [MyLineupUiState.Status.Error]
 * on repository failure. These tests drive it through real [ScheduleRepository]/[ArtistRepository]
 * instances backed by fake DAOs, matching the approach used in
 * [com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleViewModelTest] and
 * [com.ilyne.helloszigetkmp.presentation.feature.artistdetail.ArtistDetailViewModelTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyLineupViewModelTest {
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

    private fun dayLabel(isoLocalDateTime: String): String = millisAt(isoLocalDateTime).toFestivalDate().formatDate()

    private fun favorite(
        id: String,
        startTime: String,
    ) = SetTimeWithArtistStageSummary(
        id = id,
        startTime = millisAt(startTime),
        endTime = millisAt(startTime),
        artistId = "artist-$id",
        artistName = "Artist $id",
        stageId = "stage-1",
        stageName = "Main Stage",
    )

    @Test
    fun init_groupsFavoriteSetTimesByFestivalDayAndFormatsLabels() =
        runTest {
            // 01:00 on the 7th is before the 6am festival-day cutoff, so it still belongs to
            // the 6th's festival date alongside the 20:00 set time that same evening.
            val setTimeDao = FakeSetTimeDao(
                favorites = listOf(
                    favorite("st-1", "2026-08-06T20:00:00"),
                    favorite("st-2", "2026-08-07T01:00:00"),
                    favorite("st-3", "2026-08-07T20:00:00"),
                ),
            )
            val viewModel = newViewModel(setTimeDao = setTimeDao)

            val state = viewModel.uiState.value
            assertIs<MyLineupUiState.Status.Success>(state.status)

            val dayOneLabel = dayLabel("2026-08-06T20:00:00")
            val dayTwoLabel = dayLabel("2026-08-07T20:00:00")

            assertEquals(setOf(dayOneLabel, dayTwoLabel), state.favoritesGroupedByDay.keys)
            assertEquals(
                listOf("st-1", "st-2"),
                state.favoritesGroupedByDay.getValue(dayOneLabel).map { it.id },
            )
            assertEquals(
                listOf("st-3"),
                state.favoritesGroupedByDay.getValue(dayTwoLabel).map { it.id },
            )
        }

    @Test
    fun init_noFavorites_groupsToEmptyMapWithSuccessStatus() =
        runTest {
            val viewModel = newViewModel()

            val state = viewModel.uiState.value
            assertIs<MyLineupUiState.Status.Success>(state.status)
            assertEquals(emptyMap(), state.favoritesGroupedByDay)
        }

    @Test
    fun refresh_softRefreshGateFresh_skipsNetworkAndTransitionsToSuccess() =
        runTest {
            // Both repositories' SoftRefreshGates are pre-warmed as fresh, so refresh() never
            // suspends on the mock HTTP client at all - the Loading -> Success transition
            // completes synchronously within the single UnconfinedTestDispatcher resumption, so
            // (as in ScheduleViewModelTest's non-HTTP cases) it's read directly via .value rather
            // than via turbine, which would otherwise coalesce the intermediate Loading state.
            val viewModel = newViewModel(settings = freshSettings())
            assertIs<MyLineupUiState.Status.Success>(viewModel.uiState.value.status)

            viewModel.refresh()

            assertIs<MyLineupUiState.Status.Success>(viewModel.uiState.value.status)
        }

    @Test
    fun refresh_repositoryFetchFailsBecauseUnparseableApiResponse_setsErrorStatus() =
        runTest {
            // Settings are left stale (not pre-warmed), so refresh() actually calls through to
            // the mock API. Its respondOk() with no body can't be parsed as List<StageDto>, so
            // ScheduleRepository.refresh() throws and is caught by the ViewModel.
            val viewModel = newViewModel(settings = MapSettings())

            viewModel.uiState.test {
                assertIs<MyLineupUiState.Status.Success>(awaitItem().status)

                viewModel.refresh()

                assertIs<MyLineupUiState.Status.Loading>(awaitItem().status)
                assertIs<MyLineupUiState.Status.Error>(awaitItem().status)
            }
        }

    @Test
    fun removeFavorite_success_leavesStatusAsSuccess() =
        runTest {
            val artistDao = FakeArtistDao(
                listOf(
                    ArtistEntity(
                        id = "artist-1",
                        name = "Artist One",
                        bio = null,
                        imageUrl = null,
                        isFavorited = true,
                        tags = null,
                    ),
                ),
            )
            val viewModel = newViewModel(artistDao = artistDao)

            viewModel.removeFavorite("artist-1")

            assertIs<MyLineupUiState.Status.Success>(viewModel.uiState.value.status)
            assertEquals(false, artistDao.isFavorited("artist-1"))
        }

    @Test
    fun removeFavorite_repositoryThrows_setsErrorStatus() =
        runTest {
            val artistDao = FakeArtistDao(
                listOf(
                    ArtistEntity(
                        id = "boom-artist",
                        name = "Boom",
                        bio = null,
                        imageUrl = null,
                        isFavorited = true,
                        tags = null,
                    ),
                ),
            )
            artistDao.throwOnSetFavoritedFor("boom-artist")
            val viewModel = newViewModel(artistDao = artistDao)

            viewModel.removeFavorite("boom-artist")

            val status = assertIs<MyLineupUiState.Status.Error>(viewModel.uiState.value.status)
            assertEquals("setFavorited boom for boom-artist", status.message)
        }

    // --- test fixtures -------------------------------------------------------------------

    /**
     * Pre-warms both repositories' `SoftRefreshGate`s as "just fetched" so a non-forced
     * `refresh(force = false)` skips the network call entirely rather than hitting the mock
     * API and failing to parse an empty response body.
     */
    private fun freshSettings(): Settings {
        val settings = MapSettings()
        val now = Clock.System.now().toEpochMilliseconds()
        listOf("ScheduleRepository", "ArtistRepository").forEach { key ->
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
        setTimeDao: SetTimeDao = FakeSetTimeDao(emptyList()),
        stageDao: StageDao = FakeStageDao(emptyList()),
        artistDao: ArtistDao = FakeArtistDao(emptyList()),
        settings: Settings = freshSettings(),
    ): MyLineupViewModel {
        val scheduleRepository = ScheduleRepository(
            api = mockApi(),
            setTimeDao = setTimeDao,
            stageDao = stageDao,
            artistDao = artistDao,
            settings = settings,
        )
        val artistRepository = ArtistRepository(api = mockApi(), dao = artistDao, settings = settings)

        return MyLineupViewModel(
            artistRepository = artistRepository,
            scheduleRepository = scheduleRepository,
            ioDispatcher = Dispatchers.Main,
        )
    }

    private class FakeSetTimeDao(
        favorites: List<SetTimeWithArtistStageSummary>,
    ) : SetTimeDao {
        private val favoritesFlow = MutableStateFlow(favorites)

        override fun observeAll(): Flow<List<com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity>> = flowOf(emptyList())

        override fun observeFavorites(): Flow<List<SetTimeWithArtistStageSummary>> = favoritesFlow

        override fun observeByDay(
            dayStartMillis: Long,
            dayEndMillis: Long,
        ): Flow<List<com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(setTimes: List<com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity>) {}

        override fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> = flowOf(SetTimeDao.SetTimeRange(0L, 0L))
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

        fun isFavorited(id: String): Boolean? = artistsFlow.value.find { it.id == id }?.isFavorited

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
}
