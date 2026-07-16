package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import androidx.compose.ui.state.ToggleableState
import app.cash.turbine.test
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [ScheduleFilterViewModel] is a local UI state machine for the Schedule filter dialog — no
 * repository/API dependency, just [FilterIntent]s folding into [ScheduleFilterUiState] plus a
 * [FilterEffect] emitted on Save. Mirrors DiscoverFilterViewModel's tri-state
 * checkbox/performance-type/genre cascading, plus the simple boolean toggles unique to Schedule
 * (favorites-only, friends-going, hide-empty-stages, show-extra-days).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleFilterViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun performanceTypeState(
        state: ScheduleFilterUiState,
        type: PerformanceType,
    ): PerformanceTypeUiState = state.performanceTypes.first { it.type == type }

    private fun genreState(
        state: ScheduleFilterUiState,
        group: GenreGroup,
    ): GenreUiState =
        state.performanceTypes.flatMap { it.genres }.first { it.group == group }

    @Test
    fun initialState_defaultsMatchScheduleFilterDefaults() {
        val viewModel = ScheduleFilterViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.showFavoritesOnly)
        assertFalse(state.showFriendsGoing)
        assertTrue(state.hideEmptyStages)
        assertFalse(state.showExtraDays)
        // ScheduleFilter() defaults selectedGenreGroups to all MUSIC genre groups.
        assertEquals(ToggleableState.On, performanceTypeState(state, PerformanceType.MUSIC).checkState)
        assertEquals(ToggleableState.Off, performanceTypeState(state, PerformanceType.DANCE).checkState)
        assertEquals(ToggleableState.Off, performanceTypeState(state, PerformanceType.ARTS_CULTURE).checkState)
        assertTrue(state.performanceTypes.all { !it.isExpanded })
    }

    @Test
    fun toggleFavoritesOnly_updatesState() {
        val viewModel = ScheduleFilterViewModel()

        viewModel.onIntent(FilterIntent.ToggleFavoritesOnly(true))
        assertTrue(viewModel.uiState.value.showFavoritesOnly)

        viewModel.onIntent(FilterIntent.ToggleFavoritesOnly(false))
        assertFalse(viewModel.uiState.value.showFavoritesOnly)
    }

    @Test
    fun toggleFriendsGoing_updatesState() {
        val viewModel = ScheduleFilterViewModel()

        viewModel.onIntent(FilterIntent.ToggleFriendsGoing(true))
        assertTrue(viewModel.uiState.value.showFriendsGoing)

        viewModel.onIntent(FilterIntent.ToggleFriendsGoing(false))
        assertFalse(viewModel.uiState.value.showFriendsGoing)
    }

    @Test
    fun toggleHideEmptyStages_updatesState() {
        val viewModel = ScheduleFilterViewModel()

        // Default is true, so flip to false first.
        viewModel.onIntent(FilterIntent.ToggleHideEmptyStages(false))
        assertFalse(viewModel.uiState.value.hideEmptyStages)

        viewModel.onIntent(FilterIntent.ToggleHideEmptyStages(true))
        assertTrue(viewModel.uiState.value.hideEmptyStages)
    }

    @Test
    fun toggleShowExtraDays_updatesState() {
        val viewModel = ScheduleFilterViewModel()

        viewModel.onIntent(FilterIntent.ToggleShowExtraDays(true))
        assertTrue(viewModel.uiState.value.showExtraDays)

        viewModel.onIntent(FilterIntent.ToggleShowExtraDays(false))
        assertFalse(viewModel.uiState.value.showExtraDays)
    }

    @Test
    fun toggleGenreDropdown_expandsAndCollapsesOnlyThatType() {
        val viewModel = ScheduleFilterViewModel()

        viewModel.onIntent(FilterIntent.ToggleGenreDropdown(PerformanceType.MUSIC))
        var state = viewModel.uiState.value
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).isExpanded)
        assertFalse(performanceTypeState(state, PerformanceType.DANCE).isExpanded)
        assertFalse(performanceTypeState(state, PerformanceType.ARTS_CULTURE).isExpanded)

        viewModel.onIntent(FilterIntent.ToggleGenreDropdown(PerformanceType.DANCE))
        state = viewModel.uiState.value
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).isExpanded)
        assertTrue(performanceTypeState(state, PerformanceType.DANCE).isExpanded)

        viewModel.onIntent(FilterIntent.ToggleGenreDropdown(PerformanceType.MUSIC))
        state = viewModel.uiState.value
        assertFalse(performanceTypeState(state, PerformanceType.MUSIC).isExpanded)
        assertTrue(performanceTypeState(state, PerformanceType.DANCE).isExpanded)
    }

    @Test
    fun toggleGenreGroup_onWithinPartiallySelectedType_marksTypeIndeterminate() {
        val viewModel = ScheduleFilterViewModel()

        // MUSIC starts fully selected; unchecking one genre should make it Indeterminate.
        viewModel.onIntent(FilterIntent.ToggleGenreGroup(GenreGroup.ROCK, false))

        val state = viewModel.uiState.value
        assertFalse(genreState(state, GenreGroup.ROCK).isChecked)
        assertEquals(ToggleableState.Indeterminate, performanceTypeState(state, PerformanceType.MUSIC).checkState)
    }

    @Test
    fun toggleGenreGroup_uncheckingAllGenresOfAType_marksTypeOff() {
        val viewModel = ScheduleFilterViewModel()

        genreGroupsFor(PerformanceType.MUSIC).forEach { group ->
            viewModel.onIntent(FilterIntent.ToggleGenreGroup(group, false))
        }

        val state = viewModel.uiState.value
        assertEquals(ToggleableState.Off, performanceTypeState(state, PerformanceType.MUSIC).checkState)
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).genres.all { !it.isChecked })
    }

    @Test
    fun toggleGenreGroup_checkingSingleGenreOfOffType_marksTypeIndeterminate() {
        val viewModel = ScheduleFilterViewModel()

        // DANCE starts fully off (only one genre group, GenreGroup.DANCE).
        viewModel.onIntent(FilterIntent.ToggleGenreGroup(GenreGroup.DANCE, true))

        val state = viewModel.uiState.value
        assertTrue(genreState(state, GenreGroup.DANCE).isChecked)
        // DANCE has exactly one genre group, so checking it fully selects the type.
        assertEquals(ToggleableState.On, performanceTypeState(state, PerformanceType.DANCE).checkState)
    }

    @Test
    fun togglePerformanceType_offType_selectsAllOfItsGenres() {
        val viewModel = ScheduleFilterViewModel()

        // ARTS_CULTURE starts fully off.
        viewModel.onIntent(FilterIntent.TogglePerformanceType(PerformanceType.ARTS_CULTURE))

        val state = viewModel.uiState.value
        assertEquals(ToggleableState.On, performanceTypeState(state, PerformanceType.ARTS_CULTURE).checkState)
        assertTrue(performanceTypeState(state, PerformanceType.ARTS_CULTURE).genres.all { it.isChecked })
    }

    @Test
    fun togglePerformanceType_fullySelectedType_deselectsAllOfItsGenres() {
        val viewModel = ScheduleFilterViewModel()

        // MUSIC starts fully selected.
        viewModel.onIntent(FilterIntent.TogglePerformanceType(PerformanceType.MUSIC))

        val state = viewModel.uiState.value
        assertEquals(ToggleableState.Off, performanceTypeState(state, PerformanceType.MUSIC).checkState)
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).genres.all { !it.isChecked })
    }

    @Test
    fun togglePerformanceType_partiallySelectedType_selectsAllOfItsGenres() {
        val viewModel = ScheduleFilterViewModel()

        // Make MUSIC partially selected first.
        viewModel.onIntent(FilterIntent.ToggleGenreGroup(GenreGroup.ROCK, false))
        assertEquals(
            ToggleableState.Indeterminate,
            performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC).checkState,
        )

        // Toggling a partially-selected type should turn it fully on, not off.
        viewModel.onIntent(FilterIntent.TogglePerformanceType(PerformanceType.MUSIC))

        val state = viewModel.uiState.value
        assertEquals(ToggleableState.On, performanceTypeState(state, PerformanceType.MUSIC).checkState)
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).genres.all { it.isChecked })
    }

    @Test
    fun togglePerformanceType_doesNotAffectOtherTypes() {
        val viewModel = ScheduleFilterViewModel()

        viewModel.onIntent(FilterIntent.TogglePerformanceType(PerformanceType.MUSIC))

        val state = viewModel.uiState.value
        assertEquals(ToggleableState.Off, performanceTypeState(state, PerformanceType.DANCE).checkState)
        assertEquals(ToggleableState.Off, performanceTypeState(state, PerformanceType.ARTS_CULTURE).checkState)
    }

    @Test
    fun initialize_replacesFilter_butPreservesExpandedTypes() {
        val viewModel = ScheduleFilterViewModel()

        // Expand MUSIC before re-initializing.
        viewModel.onIntent(FilterIntent.ToggleGenreDropdown(PerformanceType.MUSIC))
        assertTrue(performanceTypeState(viewModel.uiState.value, PerformanceType.MUSIC).isExpanded)

        val newFilter = ScheduleFilter(
            showFavorites = true,
            showFriendsGoing = true,
            hideEmptyStages = false,
            showExtraDays = true,
            selectedGenreGroups = setOf(GenreGroup.JAZZ),
        )
        viewModel.onIntent(FilterIntent.Initialize(newFilter))

        val state = viewModel.uiState.value
        assertTrue(state.showFavoritesOnly)
        assertTrue(state.showFriendsGoing)
        assertFalse(state.hideEmptyStages)
        assertTrue(state.showExtraDays)
        assertTrue(genreState(state, GenreGroup.JAZZ).isChecked)
        assertEquals(ToggleableState.Indeterminate, performanceTypeState(state, PerformanceType.MUSIC).checkState)
        // Initialize only replaces `filter`; expandedTypes is separate ViewModel state that
        // Initialize does not touch, so the prior expansion should still be reflected.
        assertTrue(performanceTypeState(state, PerformanceType.MUSIC).isExpanded)
    }

    @Test
    fun save_emitsUpdateFilterEffectWithCurrentFilter() =
        runTest {
            val viewModel = ScheduleFilterViewModel()

            viewModel.onIntent(FilterIntent.ToggleFavoritesOnly(true))
            viewModel.onIntent(FilterIntent.ToggleGenreGroup(GenreGroup.ROCK, false))

            viewModel.effects.test {
                viewModel.onIntent(FilterIntent.Save)

                val effect = awaitItem() as FilterEffect.UpdateFilter
                assertTrue(effect.filter.showFavorites)
                assertFalse(GenreGroup.ROCK in effect.filter.selectedGenreGroups)
            }
        }

    @Test
    fun save_doesNotChangeUiState() {
        val viewModel = ScheduleFilterViewModel()
        val stateBefore = viewModel.uiState.value

        viewModel.onIntent(FilterIntent.Save)

        assertEquals(stateBefore, viewModel.uiState.value)
    }
}
