package com.ilyne.helloszigetkmp.presentation.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.presentation.component.filter.FilterBar
import com.ilyne.helloszigetkmp.presentation.component.filter.FilterBarData
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.component.pulltorefresh.PullToRefreshContent
import com.ilyne.helloszigetkmp.presentation.component.status.ErrorState
import com.ilyne.helloszigetkmp.presentation.component.status.LoadingBox
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.header.DaySelector
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.header.ViewModeToggle
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list.SetTimeListView
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.swimlane.SwimLaneView
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.timeline.TimelineGridView
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase.ActiveFilterItem
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.util.LoadStatus
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.schedule_active_filter_friends_going
import hello_sziget_kmp.shared.generated.resources.schedule_error_load_day
import hello_sziget_kmp.shared.generated.resources.schedule_error_load_days
import hello_sziget_kmp.shared.generated.resources.schedule_error_refresh
import hello_sziget_kmp.shared.generated.resources.schedule_error_update_favorite
import hello_sziget_kmp.shared.generated.resources.schedule_filter_favorites
import hello_sziget_kmp.shared.generated.resources.schedule_no_set_times
import hello_sziget_kmp.shared.generated.resources.schedule_sets_count
import hello_sziget_kmp.shared.generated.resources.schedule_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleScreen(
    openFilterScreen: (ScheduleFilter) -> Unit,
    appliedFilter: ScheduleFilter?,
    onConsumeAppliedFilter: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ScheduleViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOpenFilterScreen by rememberUpdatedState(openFilterScreen)
    val currentOnConsumeAppliedFilter by rememberUpdatedState(onConsumeAppliedFilter)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ScheduleEffect.NavigateToFilter -> currentOpenFilterScreen(effect.filter)
            }
        }
    }

    LaunchedEffect(appliedFilter) {
        appliedFilter?.let {
            viewModel.onIntent(ScheduleIntent.ApplyFilter(it))
            currentOnConsumeAppliedFilter()
        }
    }

    ScheduleContent(
        uiState = uiState,
        onViewModeChange = { viewModel.onIntent(ScheduleIntent.ChangeViewMode(it)) },
        onDaySelect = { viewModel.onIntent(ScheduleIntent.SelectDay(it)) },
        onFilterButtonClick = { viewModel.onIntent(ScheduleIntent.OpenFilter) },
        onToggleFavorite = { artistId, current ->
            viewModel.onIntent(ScheduleIntent.ToggleFavorite(artistId, current))
        },
        onArtistClick = onArtistClick,
        onRefresh = { viewModel.refresh() },
        modifier = modifier,
    )
}

@Composable
private fun ScheduleContent(
    uiState: ScheduleUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onDaySelect: (SetTimeDay) -> Unit,
    onFilterButtonClick: () -> Unit,
    onToggleFavorite: (String?, Boolean) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PullToRefreshContent(
        isRefreshing = uiState.status == LoadStatus.Loading,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface)
                .testTag("schedule_screen"),
        ) {
            MainHeader(text = stringResource(Res.string.schedule_title)) {
                ViewModeToggle(
                    current = uiState.viewMode,
                    onChange = onViewModeChange,
                    modifier = Modifier.testTag("schedule_view_mode_toggle"),
                )
            }

            DaySelector(
                days = uiState.days,
                selected = uiState.selectedDay,
                onDaySelect = onDaySelect,
                modifier = Modifier.testTag("schedule_day_selector"),
            )

            FilterBar(
                data = FilterBarData(
                    filterCount = uiState.activeFilterCount,
                    filterTexts = uiState.activeFilterItems.map { item ->
                        when (item) {
                            ActiveFilterItem.Favorites -> stringResource(Res.string.schedule_filter_favorites)
                            ActiveFilterItem.FriendsGoing -> stringResource(Res.string.schedule_active_filter_friends_going)
                            is ActiveFilterItem.Custom -> item.text
                        }
                    },
                    trailingText = if (uiState.setTimes.isNotEmpty()) {
                        stringResource(Res.string.schedule_sets_count, uiState.setTimes.size)
                    } else {
                        null
                    },
                ),
                onFilterButtonClick = onFilterButtonClick,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
                    .testTag("schedule_filter_bar"),
                backgroundColor = MaterialTheme.colorScheme.surface,
            )

            // Content
            // Loading only replaces the screen when there's no cached data to show yet
            // (e.g. first load); a pull-to-refresh with existing data keeps rendering that
            // data underneath PullToRefreshBox's own refresh indicator instead of flashing empty.
            when (val status = uiState.status) {
                is LoadStatus.Loading -> {
                    if (uiState.setTimes.isEmpty()) {
                        LoadingBox(modifier = Modifier.fillMaxSize().testTag("schedule_loading_state"))
                    } else {
                        ScheduleSetTimesContent(
                            uiState = uiState,
                            onToggleFavorite = onToggleFavorite,
                            onArtistClick = onArtistClick,
                        )
                    }
                }

                is LoadStatus.Error -> {
                    val errorMessageRes = when (status.reason) {
                        ScheduleErrorReason.REFRESH_FAILED -> Res.string.schedule_error_refresh
                        ScheduleErrorReason.UPDATE_FAVORITE_FAILED -> Res.string.schedule_error_update_favorite
                        ScheduleErrorReason.LOAD_DAY_FAILED -> Res.string.schedule_error_load_day
                        ScheduleErrorReason.LOAD_DAYS_FAILED -> Res.string.schedule_error_load_days
                    }
                    ErrorState(
                        message = stringResource(errorMessageRes),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .testTag("schedule_error_state"),
                    )
                }

                else -> {
                    if (uiState.setTimes.isEmpty()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .testTag("schedule_empty_state"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(Res.string.schedule_no_set_times))
                        }
                    } else {
                        key(uiState.selectedDay) {
                            ScheduleSetTimesContent(
                                uiState = uiState,
                                onToggleFavorite = onToggleFavorite,
                                onArtistClick = onArtistClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSetTimesContent(
    uiState: ScheduleUiState,
    onToggleFavorite: (String?, Boolean) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    when (uiState.viewMode) {
        ViewMode.GRID -> {
            TimelineGridView(
                setTimes = uiState.setTimes,
                stages = uiState.stages,
                gridMinHour = uiState.gridMinHour,
                gridMaxHour = uiState.gridMaxHour,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.testTag("schedule_grid_view"),
            )
        }

        ViewMode.SWIMLANE -> {
            SwimLaneView(
                setTimes = uiState.setTimes,
                stages = uiState.stages,
                gridMinHour = uiState.gridMinHour,
                gridMaxHour = uiState.gridMaxHour,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.testTag("schedule_swimlane_view"),
            )
        }

        ViewMode.LIST -> {
            SetTimeListView(
                uiState.setTimes,
                onToggleFavorite = onToggleFavorite,
                onArtistClick = onArtistClick,
                modifier = Modifier.fillMaxSize().testTag("schedule_list_view"),
            )
        }
    }
}

@Preview
@Composable
private fun ScheduleContentPreview() {
    AppTheme {
        ScheduleContent(
            uiState = ScheduleUiState(),
            onViewModeChange = {},
            onDaySelect = {},
            onFilterButtonClick = {},
            onToggleFavorite = { _, _ -> },
        )
    }
}
