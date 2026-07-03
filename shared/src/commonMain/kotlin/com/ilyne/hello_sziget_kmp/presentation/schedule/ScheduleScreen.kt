package com.ilyne.hello_sziget_kmp.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.hello_sziget_kmp.domain.model.SetTime
import com.ilyne.hello_sziget_kmp.domain.model.SetTimeDay
import com.ilyne.hello_sziget_kmp.domain.model.Stage
import com.ilyne.hello_sziget_kmp.presentation.schedule.components.SetTimeCard
import com.ilyne.hello_sziget_kmp.presentation.schedule.components.color.stageColor
import com.ilyne.hello_sziget_kmp.presentation.schedule.model.ScheduleUiModel
import com.ilyne.hello_sziget_kmp.util.datetime.toLocalDateTime
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
                ViewMode.GRID -> TimelineGridView(uiState.selectedDay?.dayStartMillis, uiState.setTimes, uiState.stages)
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
                    text = day.dayOfWeekLabel().uppercase(),
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
private const val HEADER_HEIGHT_DP = 50
private const val TIME_LABEL_WIDTH_DP = 48
private const val HOUR_LABEL_HEIGHT_DP = 16

private fun getNormalizedHour(hour: Int): Int = if (hour < 6) hour + 24 else hour

private fun getNormalizedHourFraction(hour: Int, minute: Int): Double {
    val fraction = hour + minute / 60.0
    return if (hour < 6) fraction + 24 else fraction
}

@Composable
private fun TimelineGridView(dayMillis: Long?, setTimes: List<ScheduleUiModel.SetTime>, stages: List<Stage>) {
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    if (setTimes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sets scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val minSetStart = setTimes.minOf { it.startTime }.toLocalDateTime()
    val maxSetEnd = setTimes.maxOf { it.endTime }.toLocalDateTime()

    val minHour = getNormalizedHour(minSetStart.hour)
    val maxHour = getNormalizedHour(maxSetEnd.hour) + 1

    val totalGridHeight = ((maxHour - minHour) * HOUR_HEIGHT_DP).dp
    val totalGridWidth = (stages.size * COLUMN_WIDTH_DP).dp

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row: a static spacer reserves space for the sticky hour column below,
        // stage names scroll horizontally in lockstep with the grid via horizScroll.
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(TIME_LABEL_WIDTH_DP.dp).height(HEADER_HEIGHT_DP.dp))
            Row(modifier = Modifier.horizontalScroll(horizScroll)) {
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
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Sticky hour column: only scrolls vertically (shares vertScroll with the grid),
            // never scrolls horizontally, so it stays pinned to the left edge.
            Box(
                modifier = Modifier
                    .width(TIME_LABEL_WIDTH_DP.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(vertScroll)
            ) {
                Box(modifier = Modifier.height(totalGridHeight).fillMaxWidth()) {
                    for (hour in minHour..maxHour) {
                        val y = ((hour - minHour) * HOUR_HEIGHT_DP).dp
                        Box(
                            modifier = Modifier
                                .offset(x = 8.dp, y = y)
                                .height(HOUR_LABEL_HEIGHT_DP.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = "${hour % 24}:00",
                                fontSize = 12.sp,
                                lineHeight = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(vertScroll)
                .horizontalScroll(horizScroll)
            ) {
                Box(modifier = Modifier
                    .size(totalGridWidth, totalGridHeight)
                    .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Hour gridlines span the full scrollable grid width
                    for (hour in minHour..maxHour) {
                        val y = ((hour - minHour) * HOUR_HEIGHT_DP).dp
                        Box(
                            modifier = Modifier.offset(y = y + (HOUR_LABEL_HEIGHT_DP / 2).dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    // Set time cards
                    setTimes.forEach { setTime ->
                        val stageIndex = stages.indexOfFirst { it.id == setTime.stageId }
                        if (stageIndex < 0) return@forEach
                        val startDateTime = setTime.startTime.toLocalDateTime()
                        val endDateTime = setTime.endTime.toLocalDateTime()
                        val startHourFraction = getNormalizedHourFraction(
                            startDateTime.hour,
                            startDateTime.minute
                        )
                        val endHourFraction = getNormalizedHourFraction(
                            endDateTime.hour,
                            endDateTime.minute
                        )
                        val topDp = ((startHourFraction - minHour) * HOUR_HEIGHT_DP).dp
                        val topOffset = (3 + HOUR_LABEL_HEIGHT_DP / 2).dp
                        val heightDp = ((endHourFraction - startHourFraction) * HOUR_HEIGHT_DP - 3).dp
                        val leftDp = (stageIndex * COLUMN_WIDTH_DP + 2).dp

                        SetTimeCard(
                            setTime = setTime,
                            modifier = Modifier
                                .offset(x = leftDp, y = topDp + topOffset)
                                .width((COLUMN_WIDTH_DP - 4).dp)
                                .height(heightDp),
                        )
                    }
                }
            }
        }
    }
}

// ── Swimlane View (Y = stage rows, X = time axis) ────────────────────────────

@Composable
private fun SwimLaneView(setTimes: List<ScheduleUiModel.SetTime>, stages: List<Stage>) {
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
                            val startFraction = getNormalizedHour(setTime.startTime.toLocalDateTime().hour)
                            val endFraction = getNormalizedHour(setTime.endTime.toLocalDateTime().hour)
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
private fun SetTimeListView(setTimes: List<ScheduleUiModel.SetTime>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(setTimes, key = { it.id }) { setTime ->
            SetTimeCard(setTime = setTime, modifier = Modifier.fillMaxWidth().height(72.dp))
        }
    }
}
