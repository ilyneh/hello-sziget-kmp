package com.ilyne.helloszigetkmp.presentation.feature.discover.filter

import androidx.compose.ui.state.ToggleableState
import app.cash.turbine.test
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverFilterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun performanceTypeState(
        state: DiscoverFilterUiState,
        type: PerformanceType,
    ): DiscoverPerformanceTypeUiState = state.performanceTypes.first { it.type == type }

    @Test
    fun `initial state has all performance types fully checked and collapsed`() = runTest {
        val viewModel = DiscoverFilterViewModel()

        val state = viewModel.uiState.value

        assertEquals(PerformanceType.entries.size, state.performanceTypes.size)
        state.performanceTypes.forEach { typeState ->
            assertEquals(ToggleableState.On, typeState.checkState)
            assertFalse(typeState.isExpanded)
            assertTrue(typeState.genres.all { it.isChecked })
        }
    }

    @Test
    fun `Initialize applies the given filter to ui state`() = runTest {
        val viewModel = DiscoverFilterViewModel()
        val onlyRock = setOf(GenreGroup.ROCK)

        viewModel.onIntent(DiscoverFilterIntent.Initialize(DiscoverFilter(selectedGenreGroups = onlyRock)))

        val musicState = performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC)
        assertEquals(ToggleableState.Indeterminate, musicState.checkState)
        assertTrue(musicState.genres.first { it.group == GenreGroup.ROCK }.isChecked)
        assertTrue(musicState.genres.filterNot { it.group == GenreGroup.ROCK }.none { it.isChecked })

        val danceState = performanceTypeState(viewModel.uiState.value, PerformanceType.DANCE)
        assertEquals(ToggleableState.Off, danceState.checkState)
    }

    @Test
    fun `TogglePerformanceType turns a fully selected type off along with its genres`() = runTest {
        val viewModel = DiscoverFilterViewModel()

        viewModel.onIntent(DiscoverFilterIntent.TogglePerformanceType(PerformanceType.DANCE))

        val danceState = performanceTypeState(viewModel.uiState.value, PerformanceType.DANCE)
        assertEquals(ToggleableState.Off, danceState.checkState)
        assertTrue(danceState.genres.none { it.isChecked })

        // Other types remain untouched.
        val musicState = performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC)
        assertEquals(ToggleableState.On, musicState.checkState)
    }

    @Test
    fun `TogglePerformanceType turns an off type fully on`() = runTest {
        val viewModel = DiscoverFilterViewModel()
        viewModel.onIntent(DiscoverFilterIntent.TogglePerformanceType(PerformanceType.DANCE))
        assertEquals(
            ToggleableState.Off,
            performanceTypeState(viewModel.uiState.value, PerformanceType.DANCE).checkState,
        )

        viewModel.onIntent(DiscoverFilterIntent.TogglePerformanceType(PerformanceType.DANCE))

        val danceState = performanceTypeState(viewModel.uiState.value, PerformanceType.DANCE)
        assertEquals(ToggleableState.On, danceState.checkState)
        assertTrue(danceState.genres.all { it.isChecked })
    }

    @Test
    fun `TogglePerformanceType selects all genres when partially selected`() = runTest {
        val viewModel = DiscoverFilterViewModel()
        viewModel.onIntent(
            DiscoverFilterIntent.Initialize(DiscoverFilter(selectedGenreGroups = setOf(GenreGroup.ROCK))),
        )
        assertEquals(
            ToggleableState.Indeterminate,
            performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC).checkState,
        )

        viewModel.onIntent(DiscoverFilterIntent.TogglePerformanceType(PerformanceType.MUSIC))

        val musicState = performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC)
        assertEquals(ToggleableState.On, musicState.checkState)
        assertTrue(musicState.genres.all { it.isChecked })
    }

    @Test
    fun `ToggleGenreGroup unchecking a single genre makes the type indeterminate`() = runTest {
        val viewModel = DiscoverFilterViewModel()

        viewModel.onIntent(DiscoverFilterIntent.ToggleGenreGroup(GenreGroup.ROCK, false))

        val musicState = performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC)
        assertEquals(ToggleableState.Indeterminate, musicState.checkState)
        assertFalse(musicState.genres.first { it.group == GenreGroup.ROCK }.isChecked)
        assertTrue(musicState.genres.filterNot { it.group == GenreGroup.ROCK }.all { it.isChecked })
    }

    @Test
    fun `ToggleGenreGroup unchecking every genre of a type turns it fully off`() = runTest {
        val viewModel = DiscoverFilterViewModel()

        genreGroupsFor(PerformanceType.DANCE).forEach { group ->
            viewModel.onIntent(DiscoverFilterIntent.ToggleGenreGroup(group, false))
        }

        val danceState = performanceTypeState(viewModel.uiState.value, PerformanceType.DANCE)
        assertEquals(ToggleableState.Off, danceState.checkState)
    }

    @Test
    fun `ToggleGenreGroup checking a genre back on can restore fully checked state`() = runTest {
        val viewModel = DiscoverFilterViewModel()
        viewModel.onIntent(DiscoverFilterIntent.ToggleGenreGroup(GenreGroup.ROCK, false))
        assertEquals(
            ToggleableState.Indeterminate,
            performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC).checkState,
        )

        viewModel.onIntent(DiscoverFilterIntent.ToggleGenreGroup(GenreGroup.ROCK, true))

        val musicState = performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC)
        assertEquals(ToggleableState.On, musicState.checkState)
        assertTrue(musicState.genres.all { it.isChecked })
    }

    @Test
    fun `ToggleGenreDropdown expands and collapses a performance type`() = runTest {
        val viewModel = DiscoverFilterViewModel()

        viewModel.uiState.test {
            assertFalse(performanceTypeState(awaitItem(), PerformanceType.MUSIC).isExpanded)

            viewModel.onIntent(DiscoverFilterIntent.ToggleGenreDropdown(PerformanceType.MUSIC))
            assertTrue(performanceTypeState(awaitItem(), PerformanceType.MUSIC).isExpanded)

            viewModel.onIntent(DiscoverFilterIntent.ToggleGenreDropdown(PerformanceType.MUSIC))
            assertFalse(performanceTypeState(awaitItem(), PerformanceType.MUSIC).isExpanded)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ToggleGenreDropdown tracks multiple expanded types independently`() = runTest {
        val viewModel = DiscoverFilterViewModel()

        viewModel.onIntent(DiscoverFilterIntent.ToggleGenreDropdown(PerformanceType.MUSIC))
        viewModel.onIntent(DiscoverFilterIntent.ToggleGenreDropdown(PerformanceType.DANCE))

        val state = viewModel.uiState.value
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).isExpanded)
        assertTrue(performanceTypeState(state, PerformanceType.DANCE).isExpanded)
        assertFalse(performanceTypeState(state, PerformanceType.ARTS_CULTURE).isExpanded)
    }

    @Test
    fun `Save emits UpdateFilter effect with the current filter`() = runTest {
        val viewModel = DiscoverFilterViewModel()
        viewModel.onIntent(DiscoverFilterIntent.ToggleGenreGroup(GenreGroup.ROCK, false))

        viewModel.effects.test {
            viewModel.onIntent(DiscoverFilterIntent.Save)

            val effect = awaitItem()
            assertTrue(effect is DiscoverFilterEffect.UpdateFilter)
            assertFalse(GenreGroup.ROCK in effect.filter.selectedGenreGroups)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
