package com.ilyne.helloszigetkmp.presentation.feature.discover

import app.cash.turbine.test
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.ArtistDto
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilter
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.usecase.GetActiveDiscoverFiltersTextUseCase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
import kotlin.test.assertIs
import kotlin.time.Clock

/**
 * [DiscoverViewModel] combines [DiscoverIntent.SearchQueryChanged] and
 * [DiscoverIntent.ApplyFilter] via `flatMapLatest`/`combine` against [ArtistRepository], and
 * separately drives a Loading/Success/Error refresh status plus a favorite-toggle side effect.
 * These tests drive it through a real [ArtistRepository] backed by a fake [ArtistDao] (same
 * approach as [com.ilyne.helloszigetkmp.presentation.feature.artistdetail.ArtistDetailViewModelTest]
 * and [com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleViewModelTest]) so the
 * combine/flatMapLatest wiring is exercised for real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val rockArtist = ArtistEntity(
        id = "artist-1",
        name = "Rockers",
        bio = null,
        imageUrl = null,
        isFavorited = false,
        tags = listOf("genre-rock"),
    )

    private val unknownArtist = ArtistEntity(
        id = "artist-2",
        name = "Mystery Act",
        bio = null,
        imageUrl = null,
        isFavorited = false,
        tags = null,
    )

    @Test
    fun init_loadsArtistsFromRepositoryWithSuccessStatus() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist, unknownArtist))
        val viewModel = newViewModel(artistDao = artistDao)

        val state = viewModel.uiState.value
        assertIs<DiscoverUiState.Status.Success>(state.status)
        assertEquals(listOf("artist-1", "artist-2"), state.artists.map { it.id })
    }

    @Test
    fun searchQueryChanged_filtersArtistsBySearchArtists() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist, unknownArtist))
        val viewModel = newViewModel(artistDao = artistDao)

        viewModel.onIntent(DiscoverIntent.SearchQueryChanged("rock"))

        val state = viewModel.uiState.value
        assertEquals("rock", state.searchQuery)
        assertEquals(listOf("artist-1"), state.artists.map { it.id })
    }

    @Test
    fun searchQueryChanged_blankQueryFallsBackToObserveArtists() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist, unknownArtist))
        val viewModel = newViewModel(artistDao = artistDao)

        viewModel.onIntent(DiscoverIntent.SearchQueryChanged("rock"))
        assertEquals(listOf("artist-1"), viewModel.uiState.value.artists.map { it.id })

        viewModel.onIntent(DiscoverIntent.SearchQueryChanged(""))

        assertEquals(listOf("artist-1", "artist-2"), viewModel.uiState.value.artists.map { it.id })
    }

    @Test
    fun applyFilter_keepsOnlyArtistsPassingGenreFilterAndUpdatesFilterMetadata() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist, unknownArtist))
        val viewModel = newViewModel(artistDao = artistDao)

        viewModel.onIntent(DiscoverIntent.ApplyFilter(DiscoverFilter(selectedGenreGroups = setOf(GenreGroup.ROCK))))

        val state = viewModel.uiState.value
        // unknownArtist has no tags, so it falls back to GenreGroup.UNKNOWN and is filtered out
        // once only ROCK is selected.
        assertEquals(listOf("artist-1"), state.artists.map { it.id })
        assertEquals(1, state.filterCount)
        assertEquals(listOf("Music"), state.filterTexts)
    }

    @Test
    fun openFilterDialog_emitsLaunchFilterDialogEffect() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onIntent(DiscoverIntent.OpenFilterDialog)

            assertIs<DiscoverEffect.LaunchFilterDialog>(awaitItem())
        }
    }

    @Test
    fun toggleFavorite_success_flipsFavoriteStateInRepository() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist))
        val viewModel = newViewModel(artistDao = artistDao)

        viewModel.toggleFavorite(artistId = "artist-1", current = false)

        assertEquals(true, artistDao.currentArtists.first { it.id == "artist-1" }.isFavorited)
        assertIs<DiscoverUiState.Status.Success>(viewModel.uiState.value.status)
    }

    @Test
    fun toggleFavorite_apiFailure_doesNotShowError() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist))
        val settings = freshSettings()
        val repository = ArtistRepository(api = failingFavoriteApi(), dao = artistDao, settings = settings)
        val viewModel = DiscoverViewModel(
            artistRepository = repository,
            getActiveDiscoverFiltersTextUseCase = GetActiveDiscoverFiltersTextUseCase(),
            backgroundDispatcher = Dispatchers.Main,
        )

        // The mock engine hops off the test dispatcher internally, so the optimistic-update ->
        // revert isn't synchronous like the rest of this suite - collect for the reverted
        // artist state via turbine instead of reading artistDao.currentArtists immediately
        // after advanceUntilIdle().
        artistDao.observeById("artist-1").test {
            assertEquals(false, awaitItem()?.isFavorited)

            viewModel.toggleFavorite(artistId = "artist-1", current = false)

            assertEquals(true, awaitItem()?.isFavorited)
            assertEquals(false, awaitItem()?.isFavorited)

            cancelAndIgnoreRemainingEvents()
        }
        assertIs<DiscoverUiState.Status.Success>(viewModel.uiState.value.status)
    }

    @Test
    fun refresh_forcedRefreshSucceeds_upsertsArtistsFromApi() = runTest {
        val artistDao = FakeArtistDao(emptyList())
        val settings = freshSettings()
        val fetchedDto = ArtistDto(
            id = "artist-3",
            name = "Fresh Act",
            bio = null,
            imageUrl = null,
            favoriteCount = 0,
            isFavorited = false,
            tags = listOf("genre-rock"),
        )
        val repository = ArtistRepository(
            api = artistsListApi(listOf(fetchedDto)),
            dao = artistDao,
            settings = settings,
        )
        val viewModel = DiscoverViewModel(
            artistRepository = repository,
            getActiveDiscoverFiltersTextUseCase = GetActiveDiscoverFiltersTextUseCase(),
            backgroundDispatcher = Dispatchers.Main,
        )

        viewModel.uiState.test {
            assertIs<DiscoverUiState.Status.Success>(awaitItem().status) // initial empty list, gate already fresh

            viewModel.refresh()

            assertIs<DiscoverUiState.Status.Loading>(awaitItem().status)

            // The dao's upsert (triggering observeArtists' own reactive emission) and
            // refreshArtists' explicit post-fetch Success update can arrive as separate
            // emissions in either order, so keep consuming until the upserted artist shows up.
            var state = awaitItem()
            while (state.artists.isEmpty()) {
                state = awaitItem()
            }
            assertIs<DiscoverUiState.Status.Success>(state.status)
            assertEquals(listOf("artist-3"), state.artists.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_forcedRefreshFailsBecauseUnparseableApiResponse_setsErrorStatus() = runTest {
        val artistDao = FakeArtistDao(listOf(rockArtist))
        val settings = freshSettings()
        // No ContentNegotiation plugin is installed, and the mock engine returns an empty 200
        // body, so parsing the response into List<ArtistDto> fails - this mirrors
        // ScheduleViewModelTest's equivalent forced-refresh-failure test.
        val repository = ArtistRepository(api = mockApi(), dao = artistDao, settings = settings)
        val viewModel = DiscoverViewModel(
            artistRepository = repository,
            getActiveDiscoverFiltersTextUseCase = GetActiveDiscoverFiltersTextUseCase(),
            backgroundDispatcher = Dispatchers.Main,
        )

        viewModel.uiState.test {
            assertIs<DiscoverUiState.Status.Success>(awaitItem().status)

            viewModel.refresh()

            assertIs<DiscoverUiState.Status.Loading>(awaitItem().status)
            val errored = awaitItem()
            assertIs<DiscoverUiState.Status.Error>(errored.status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- test fixtures -------------------------------------------------------------------

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    /**
     * Pre-warms the `ArtistRepository` [com.ilyne.helloszigetkmp.core.repository.SoftRefreshGate]
     * as "just fetched" so a non-forced `refresh(force = false)` (as triggered by
     * [DiscoverViewModel]'s init) skips the network call entirely rather than hitting the mock
     * API and failing to parse an empty response body.
     */
    private fun freshSettings(): Settings {
        val settings = MapSettings()
        settings.putLong(
            "SoftRefreshGate_lastFetchedAt_ArtistRepository",
            Clock.System.now().toEpochMilliseconds(),
        )
        return settings
    }

    private fun mockApi(): SzigetApiService =
        SzigetApiService(
            client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
            baseUrl = "https://unused.test",
        )

    private fun artistsListApi(artists: List<ArtistDto>): SzigetApiService {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/artists") -> respond(json.encodeToString(artists), headers = jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        return SzigetApiService(client = client, baseUrl = "https://unused.test")
    }

    private fun failingFavoriteApi(): SzigetApiService {
        val engine = MockEngine { throw ApiFailureException() }
        val client = HttpClient(engine)
        return SzigetApiService(client = client, baseUrl = "https://unused.test")
    }

    private class ApiFailureException : Exception("simulated api failure")

    private fun newViewModel(
        artistDao: ArtistDao = FakeArtistDao(emptyList()),
        settings: Settings = freshSettings(),
    ): DiscoverViewModel {
        val repository = ArtistRepository(api = mockApi(), dao = artistDao, settings = settings)
        return DiscoverViewModel(
            artistRepository = repository,
            getActiveDiscoverFiltersTextUseCase = GetActiveDiscoverFiltersTextUseCase(),
            backgroundDispatcher = Dispatchers.Main,
        )
    }

    private class FakeArtistDao(
        artists: List<ArtistEntity>,
    ) : ArtistDao {
        private val artistsFlow = MutableStateFlow(artists)

        val currentArtists: List<ArtistEntity> get() = artistsFlow.value

        override fun observeAll(): Flow<List<ArtistEntity>> = artistsFlow

        override fun observeById(id: String): Flow<ArtistEntity?> = artistsFlow.map { list -> list.find { it.id == id } }

        override fun observeFavorites(): Flow<List<ArtistEntity>> = artistsFlow.map { list -> list.filter { it.isFavorited } }

        override fun searchByName(query: String): Flow<List<ArtistEntity>> =
            artistsFlow.map { list -> list.filter { it.name.contains(query, ignoreCase = true) } }

        override suspend fun upsertAll(artists: List<ArtistEntity>) {
            artistsFlow.update { current ->
                val byId = current.associateBy { it.id }.toMutableMap()
                artists.forEach { byId[it.id] = it }
                byId.values.toList()
            }
        }

        override suspend fun setFavorited(
            id: String,
            isFavorited: Boolean,
        ) {
            artistsFlow.update { list -> list.map { if (it.id == id) it.copy(isFavorited = isFavorited) else it } }
        }

        override suspend fun deleteAll() {
            artistsFlow.value = emptyList()
        }
    }
}
