package com.ilyne.helloszigetkmp.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.presentation.schedule.components.color.stageColor
import com.ilyne.helloszigetkmp.presentation.schedule.model.ScheduleUiModel

// ── Grid View (Y = time, X = stage columns) ──────────────────────────────────


private const val COLUMN_WIDTH_DP = 120
private const val TIME_LABEL_WIDTH_DP = 48
private const val HOUR_LABEL_HEIGHT_DP = 16
private const val HOUR_COLUMN_SHADOW_WIDTH_DP = 6

@Composable
fun TimelineGridView(
    setTimes: List<ScheduleUiModel.SetTime>,
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
    val totalGridWidth = (stages.size * COLUMN_WIDTH_DP).dp

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row: a static spacer reserves space for the sticky hour column below,
        // stage names scroll horizontally in lockstep with the grid via horizScroll.
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(TIME_LABEL_WIDTH_DP.dp).height(HEADER_HEIGHT_DP.dp))
            Row(modifier = Modifier.horizontalScroll(horizScroll)) {
                stages.forEach { stage ->
                    Box(
                        modifier = Modifier.width(COLUMN_WIDTH_DP.dp).height(HEADER_HEIGHT_DP.dp).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stage.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = stageColor(stage.id),
                            lineHeight = 11.sp,
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sticky hour column: only scrolls vertically (shares vertScroll with the grid),
                // never scrolls horizontally, so it stays pinned to the left edge.
                Box(
                    modifier = Modifier
                        .width(TIME_LABEL_WIDTH_DP.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(vertScroll),
                ) {
                    Box(modifier = Modifier.height(totalGridHeight).fillMaxWidth()) {
                        for (hour in gridMinHour..gridMaxHour) {
                            val y = ((hour - gridMinHour) * HOUR_HEIGHT_DP).dp
                            Box(
                                modifier = Modifier.offset(x = 8.dp, y = y).height(HOUR_LABEL_HEIGHT_DP.dp),
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(vertScroll)
                        .horizontalScroll(horizScroll),
                ) {
                    Box(modifier = Modifier.size(totalGridWidth, totalGridHeight).background(MaterialTheme.colorScheme.surface)) {
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
                            val leftDp = (stageIndex * COLUMN_WIDTH_DP + 2).dp

                            SetTimeCard(
                                setTime = setTime,
                                modifier = Modifier.offset(x = leftDp, y = topDp + topOffset).width((COLUMN_WIDTH_DP - 4).dp).height(heightDp),
                            )
                        }
                    }
                }
            }

            // Shadow cast by the sticky hour column onto the scrollable grid beside it.
            Box(
                modifier = Modifier
                    .offset(x = TIME_LABEL_WIDTH_DP.dp)
                    .width(HOUR_COLUMN_SHADOW_WIDTH_DP.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent),
                        ),
                    ),
            )
        }
    }
}
