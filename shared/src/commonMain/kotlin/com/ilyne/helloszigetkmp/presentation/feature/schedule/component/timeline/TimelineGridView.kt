package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HEADER_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HOUR_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared.SetTimeCard
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.stageColor
import com.ilyne.helloszigetkmp.presentation.schedule.components.timeline.HOUR_LABEL_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.schedule.components.timeline.StickyHourColumn


// ── Grid View (Y = time, X = stage columns) ──────────────────────────────────

private const val MIN_COLUMN_WIDTH_DP = 120
private const val TIME_LABEL_WIDTH_DP = 48

@Composable
fun TimelineGridView(
    setTimes: List<ScheduleUiState.SetTime>,
    stages: List<Stage>,
    gridMinHour: Int,
    gridMaxHour: Int,
) {
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    if (setTimes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sets scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val totalGridHeight = ((gridMaxHour - gridMinHour) * HOUR_HEIGHT_DP).dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Columns stretch evenly to fill the available width when there are few stages,
        // but fall back to a fixed minimum (and let the row scroll) once that would
        // squeeze columns narrower than is readable.
        val availableGridWidth = maxWidth - TIME_LABEL_WIDTH_DP.dp
        val columnWidth = if (stages.isEmpty()) {
            MIN_COLUMN_WIDTH_DP.dp
        } else {
            maxOf(MIN_COLUMN_WIDTH_DP.dp, availableGridWidth / stages.size)
        }
        val totalGridWidth = columnWidth * stages.size

        Column(modifier = Modifier.fillMaxSize()) {
            // Header row: a static spacer reserves space for the sticky hour column below,
            // stage names scroll horizontally in lockstep with the grid via horizScroll.
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = TIME_LABEL_WIDTH_DP.dp)
                    .horizontalScroll(horizScroll)
            ) {
                stages.forEach { stage ->
                    val stageColor = stageColor(stageId = stage.id)
                    Column {
                        Box(
                            modifier = Modifier
                                .size(width = columnWidth, height = HEADER_HEIGHT_DP.dp)
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stage.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = stageColor,
                                lineHeight = 11.sp,
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                StickyHourColumn(
                    totalGridHeight = totalGridHeight,
                    gridMinHour = gridMinHour,
                    gridMaxHour = gridMaxHour,
                    modifier = Modifier
                        .width(TIME_LABEL_WIDTH_DP.dp)
                        .fillMaxHeight()
                        .verticalScroll(state = vertScroll)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(vertScroll)
                        .horizontalScroll(horizScroll),
                ) {

                    // Stage column color
                    Row(modifier = Modifier.fillMaxHeight()) {
                        stages.forEach { stage ->
                            Box(
                                modifier = Modifier.width(columnWidth)
                                    .height(totalGridHeight)
                                    .background(
                                        color = stageColor(stageId = stage.id)
                                            .copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(totalGridWidth, totalGridHeight)
                            .background(color = Color.Transparent)
                    ) {
                        // Hour gridlines span the full scrollable grid width
                        for (hour in gridMinHour..gridMaxHour) {
                            val y = ((hour - gridMinHour) * HOUR_HEIGHT_DP).dp
                            Box(
                                modifier = Modifier
                                    .offset(y = y + (HOUR_LABEL_HEIGHT_DP / 2).dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                            )
                        }

                        // Set time cards
                        setTimes.forEach { setTime ->
                            val stageIndex = stages.indexOfFirst { it.id == setTime.stageId }
                            if (stageIndex < 0) return@forEach
                            val startHourFraction = setTime.startHourFraction
                            val endHourFraction = setTime.endHourFraction
                            val topDp = ((startHourFraction - gridMinHour) * HOUR_HEIGHT_DP).dp
                            val topOffset = (2 + HOUR_LABEL_HEIGHT_DP / 2).dp
                            val heightDp = ((endHourFraction - startHourFraction) * HOUR_HEIGHT_DP - 3).dp
                            val leftDp = columnWidth * stageIndex + 2.dp

                            SetTimeCard(
                                setTime = setTime,
                                modifier = Modifier
                                    .offset(x = leftDp, y = topDp + topOffset)
                                    .width(columnWidth - 4.dp)
                                    .height(heightDp),
                            )
                        }
                    }
                }
            }
        }
    }
}
