package com.ilyne.helloszigetkmp.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.MainHeader
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.dayOfWeekLabel
import com.ilyne.helloszigetkmp.presentation.schedule.components.list.SetTimeListView
import com.ilyne.helloszigetkmp.presentation.schedule.components.swimlane.SwimLaneView
import com.ilyne.helloszigetkmp.presentation.schedule.components.timeline.TimelineGridView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ScheduleViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "Schedule") {
            ViewModeToggle(
                current = uiState.viewMode,
                onChange = { viewModel.onIntent(ScheduleIntent.ChangeViewMode(it)) },
            )
        }

        // Day selector
        DaySelector(
            days = uiState.days,
            selected = uiState.selectedDay,
            onDaySelect = { viewModel.onIntent(ScheduleIntent.SelectDay(it)) },
        )

        // Content
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            else -> {
                when (uiState.viewMode) {
                    ViewMode.GRID -> {
                        TimelineGridView(
                            setTimes = uiState.setTimes,
                            stages = uiState.stages,
                            gridMinHour = uiState.gridMinHour,
                            gridMaxHour = uiState.gridMaxHour,
                        )
                    }

                    ViewMode.SWIMLANE -> {
                        SwimLaneView(
                            setTimes = uiState.setTimes,
                            stages = uiState.stages,
                            gridMinHour = uiState.gridMinHour,
                            gridMaxHour = uiState.gridMaxHour,
                        )
                    }

                    ViewMode.LIST -> {
                        SetTimeListView(
                            uiState.setTimes,
                            onToggleFavorite = { artistId, current ->
                                viewModel.onIntent(ScheduleIntent.ToggleFavorite(artistId, current))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    current: ViewMode,
    onChange: (ViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ViewMode.entries.forEach { mode ->
            val selected = current == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (mode) {
                        ViewMode.GRID -> "⊞"
                        ViewMode.SWIMLANE -> "☰"
                        ViewMode.LIST -> "≡"
                    },
                    fontSize = 16.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DaySelector(
    days: List<SetTimeDay>,
    selected: SetTimeDay?,
    onDaySelect: (SetTimeDay) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onDaySelect(day) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = day.dayOfWeekLabel().uppercase(),
                    fontSize = 11.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = day.dateOfMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


