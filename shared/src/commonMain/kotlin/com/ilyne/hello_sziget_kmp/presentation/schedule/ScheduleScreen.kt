package com.ilyne.hello_sziget_kmp.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.hello_sziget_kmp.domain.model.SetTime
import com.ilyne.hello_sziget_kmp.domain.model.SetTimeDay
import com.ilyne.hello_sziget_kmp.domain.model.Stage
import com.ilyne.hello_sziget_kmp.presentation.schedule.components.SetTimeCard
import com.ilyne.hello_sziget_kmp.presentation.schedule.components.stageColor
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleScreen() {
    val viewModel = koinViewModel<ScheduleViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        ScheduleHeader(
            viewMode = uiState.viewMode,
            onViewModeChange = { viewModel.onIntent(ScheduleIntent.ChangeViewMode(it)) },
        )

        // Day selector
        DaySelector(
            days = uiState.days,
            selected = uiState.selectedDay,
            onDaySelect = { viewModel.onIntent(ScheduleIntent.SelectDay(it)) },
        )

        // Content
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> when (uiState.viewMode) {
                ViewMode.GRID -> TimelineGridView(uiState.setTimes, uiState.stages)
                ViewMode.SWIMLANE -> SwimLaneView(uiState.setTimes, uiState.stages)
                ViewMode.LIST -> SetTimeListView(uiState.setTimes)
            }
        }
    }
}

@Composable
private fun ScheduleHeader(viewMode: ViewMode, onViewModeChange: (ViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Schedule", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        ViewModeToggle(current = viewMode, onChange = onViewModeChange)
    }
}

@Composable
private fun ViewModeToggle(current: ViewMode, onChange: (ViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
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
                    text = when (mode) { ViewMode.GRID -> "⊞"; ViewMode.SWIMLANE -> "☰"; ViewMode.LIST -> "≡" },
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
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onDaySelect(day) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = day.dayOfWeekLabel(),
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = day.dateOfMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun SetTimeDay.dayOfWeekLabel(): String = when (dayOfWeek) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    7 -> "Sun"
    else -> ""
}

// ── Grid View (Y = time, X = stage columns) ──────────────────────────────────

private const val HOUR_HEIGHT_DP = 120
private const val COLUMN_WIDTH_DP = 120
private const val HEADER_HEIGHT_DP = 40
private const val TIME_LABEL_WIDTH_DP = 50

@Composable
private fun TimelineGridView(setTimes: List<SetTime>, stages: List<Stage>) {
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    if (setTimes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sets scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val minHour = setTimes.minOf { (it.startTime / 3_600_000) % 24 }.toInt().coerceAtLeast(0)
    val maxHour = (setTimes.maxOf { (it.endTime / 3_600_000) % 24 }.toInt() + 1).coerceAtMost(29)

    Box(modifier = Modifier.fillMaxSize()) {
        // Stage column headers (scroll horizontally, fixed at top)
        Row(modifier = Modifier.horizontalScroll(horizScroll)) {
            Spacer(modifier = Modifier.width(TIME_LABEL_WIDTH_DP.dp))
            stages.forEach { stage ->
                Box(
                    modifier = Modifier
                        .width(COLUMN_WIDTH_DP.dp)
                        .height(HEADER_HEIGHT_DP.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stage.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stageColor(stage.id),
                    )
                }
            }
        }

        // Scrollable grid body
        Box(
            modifier = Modifier
                .padding(top = HEADER_HEIGHT_DP.dp)
                .fillMaxSize()
                .verticalScroll(vertScroll)
                .horizontalScroll(horizScroll),
        ) {
            val totalHeight = ((maxHour - minHour) * HOUR_HEIGHT_DP).dp
            val totalWidth = (TIME_LABEL_WIDTH_DP + stages.size * COLUMN_WIDTH_DP).dp

            Box(modifier = Modifier.size(totalWidth, totalHeight)) {
                // Hour lines + labels
                for (h in minHour..maxHour) {
                    val y = ((h - minHour) * HOUR_HEIGHT_DP).dp
                    Box(modifier = Modifier.offset(y = y).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Text(
                        text = "${h % 24}:00",
                        fontSize = 10.sp,
                        modifier = Modifier.offset(x = 4.dp, y = y + 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Set time cards
                setTimes.forEach { setTime ->
                    val stageIndex = stages.indexOfFirst { it.id == setTime.stageId }
                    if (stageIndex < 0) return@forEach
                    val startHourFraction = (setTime.startTime / 3_600_000.0) % 24
                    val endHourFraction = (setTime.endTime / 3_600_000.0) % 24
                    val topDp = ((startHourFraction - minHour) * HOUR_HEIGHT_DP).dp
                    val heightDp = ((endHourFraction - startHourFraction) * HOUR_HEIGHT_DP).dp
                    val leftDp = (TIME_LABEL_WIDTH_DP + stageIndex * COLUMN_WIDTH_DP + 2).dp

                    SetTimeCard(
                        setTime = setTime,
                        modifier = Modifier
                            .offset(x = leftDp, y = topDp)
                            .width((COLUMN_WIDTH_DP - 4).dp)
                            .height(heightDp.coerceAtLeast(40.dp)),
                    )
                }
            }
        }
    }
}

// ── Swimlane View (Y = stage rows, X = time axis) ────────────────────────────

@Composable
private fun SwimLaneView(setTimes: List<SetTime>, stages: List<Stage>) {
    val horizScroll = rememberScrollState()
    val vertScroll = rememberScrollState()

    if (setTimes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sets scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val minHour = setTimes.minOf { (it.startTime / 3_600_000) % 24 }.toInt().coerceAtLeast(0)
    val maxHour = (setTimes.maxOf { (it.endTime / 3_600_000) % 24 }.toInt() + 1).coerceAtMost(29)

    Column(modifier = Modifier.fillMaxSize()) {
        // Time axis header
        Row(modifier = Modifier.horizontalScroll(horizScroll)) {
            Spacer(modifier = Modifier.width(80.dp))
            for (h in minHour until maxHour) {
                Box(modifier = Modifier.width(HOUR_HEIGHT_DP.dp).height(HEADER_HEIGHT_DP.dp), contentAlignment = Alignment.CenterStart) {
                    Text("${h % 24}:00", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(modifier = Modifier.verticalScroll(vertScroll)) {
            stages.forEach { stage ->
                val stageSets = setTimes.filter { it.stageId == stage.id }
                Row(
                    modifier = Modifier
                        .height(80.dp)
                        .horizontalScroll(horizScroll),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(80.dp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        Text(stage.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = stageColor(stage.id), modifier = Modifier.padding(start = 8.dp))
                    }

                    val totalWidth = ((maxHour - minHour) * HOUR_HEIGHT_DP).dp
                    Box(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
                        stageSets.forEach { setTime ->
                            val startFraction = (setTime.startTime / 3_600_000.0) % 24
                            val endFraction = (setTime.endTime / 3_600_000.0) % 24
                            val leftDp = ((startFraction - minHour) * HOUR_HEIGHT_DP).dp
                            val widthDp = ((endFraction - startFraction) * HOUR_HEIGHT_DP).dp

                            SetTimeCard(
                                setTime = setTime,
                                modifier = Modifier
                                    .offset(x = leftDp)
                                    .width(widthDp.coerceAtLeast(60.dp))
                                    .fillMaxHeight()
                                    .padding(2.dp),
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

// ── List View ─────────────────────────────────────────────────────────────────

@Composable
private fun SetTimeListView(setTimes: List<SetTime>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(setTimes, key = { it.id }) { setTime ->
            SetTimeCard(setTime = setTime, modifier = Modifier.fillMaxWidth().height(72.dp))
        }
    }
}
