package com.ilyne.helloszigetkmp.presentation.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.header.DaySelector
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.header.ViewModeToggle
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.component.ScheduleFilterBar
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.component.ScheduleFilterBarData
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list.SetTimeListView
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.swimlane.SwimLaneView
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.timeline.TimelineGridView
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleScreen(
    openFilterScreen: (ScheduleFilter) -> Unit,
    appliedFilter: ScheduleFilter?,
    onAppliedFilterConsumed: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<ScheduleViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ScheduleEffect.NavigateToFilter -> openFilterScreen(effect.filter)
            }
        }
    }

    LaunchedEffect(appliedFilter) {
        appliedFilter?.let {
            viewModel.onIntent(ScheduleIntent.ApplyFilter(it))
            onAppliedFilterConsumed()
        }
    }

    ScheduleContent(
        uiState = uiState,
        onViewModeChanged = { viewModel.onIntent(ScheduleIntent.ChangeViewMode(it)) },
        onDaySelected = { viewModel.onIntent(ScheduleIntent.SelectDay(it)) },
        onFilterButtonClicked = { viewModel.onIntent(ScheduleIntent.OpenFilter) },
        onToggleFavorite = { artistId, current ->
            viewModel.onIntent(ScheduleIntent.ToggleFavorite(artistId, current))
        },
        onArtistClick = onArtistClick,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
    )
}

@Composable
private fun ScheduleContent(
    uiState: ScheduleUiState,
    onViewModeChanged: (ViewMode) -> Unit,
    onDaySelected: (SetTimeDay) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onToggleFavorite: (String?, Boolean) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.status == ScheduleUiState.Status.Loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            MainHeader(text = "Schedule") {
                ViewModeToggle(
                    current = uiState.viewMode,
                    onChange = onViewModeChanged,
                )
            }

            DaySelector(
                days = uiState.days,
                selected = uiState.selectedDay,
                onDaySelect = onDaySelected,
            )

            ScheduleFilterBar(
                data = ScheduleFilterBarData(
                    filterCount = uiState.activeFilterCount,
                    setCount = uiState.setTimes.size,
                    filterTexts = uiState.activeFilterItemsText,
                ),
                onFilterButtonClicked = onFilterButtonClicked,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content
            // Loading only replaces the screen when there's no cached data to show yet
            // (e.g. first load); a pull-to-refresh with existing data keeps rendering that
            // data underneath PullToRefreshBox's own refresh indicator instead of flashing empty.
            when (val status = uiState.status) {
                is ScheduleUiState.Status.Loading -> if (uiState.setTimes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ScheduleSetTimesContent(
                        uiState = uiState,
                        onToggleFavorite = onToggleFavorite,
                        onArtistClick = onArtistClick,
                    )
                }

                is ScheduleUiState.Status.Error -> {
                    Box(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(status.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                else -> if (uiState.setTimes.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Set Times")
                    }
                } else {
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
            )
        }

        ViewMode.SWIMLANE -> {
            SwimLaneView(
                setTimes = uiState.setTimes,
                stages = uiState.stages,
                gridMinHour = uiState.gridMinHour,
                gridMaxHour = uiState.gridMaxHour,
                onArtistClick = onArtistClick,
            )
        }

        ViewMode.LIST -> {
            SetTimeListView(
                uiState.setTimes,
                onToggleFavorite = onToggleFavorite,
                onArtistClick = onArtistClick,
                modifier = Modifier.fillMaxSize()
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
            onViewModeChanged = {},
            onDaySelected = {},
            onFilterButtonClicked = {},
            onToggleFavorite = { _,_ ->}
        )
    }
}
