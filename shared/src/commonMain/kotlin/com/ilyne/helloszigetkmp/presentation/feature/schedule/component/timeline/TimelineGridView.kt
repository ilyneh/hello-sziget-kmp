package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.schedule.components.HEADER_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.schedule.components.HOUR_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared.SetTimeCard
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.stageColor
import kotlin.math.ceil
import kotlin.math.floor


// ── Grid View (Y = time, X = stage columns) ──────────────────────────────────

private const val COLUMN_WIDTH_DP = 120
private const val TIME_LABEL_WIDTH_DP = 48
private const val HOUR_BUFFER = 1
private const val COLUMN_BUFFER = 1

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
    val totalGridWidth = (stages.size * COLUMN_WIDTH_DP).dp

    val stageIndexOf = remember(stages) {
        stages.withIndex().associate { (index, stage) -> stage.id to index }
    }

    val density = LocalDensity.current
    var viewportSizePx by remember { mutableStateOf(IntSize.Zero) }

    val visibleHourRange by remember(gridMinHour, gridMaxHour) {
        derivedStateOf {
            if (viewportSizePx.height == 0) {
                gridMinHour..gridMaxHour
            } else {
                val topDp = with(density) { vertScroll.value.toDp() }.value
                val bottomDp = topDp + with(density) { viewportSizePx.height.toDp() }.value
                val first = (gridMinHour + floor(topDp / HOUR_HEIGHT_DP).toInt() - HOUR_BUFFER)
                    .coerceAtLeast(gridMinHour)
                val last = (gridMinHour + ceil(bottomDp / HOUR_HEIGHT_DP).toInt() + HOUR_BUFFER)
                    .coerceAtMost(gridMaxHour)
                first..last
            }
        }
    }

    val visibleColRange by remember(stages.size) {
        derivedStateOf {
            if (viewportSizePx.width == 0 || stages.isEmpty()) {
                0..stages.lastIndex.coerceAtLeast(0)
            } else {
                val leftDp = with(density) { horizScroll.value.toDp() }.value
                val rightDp = leftDp + with(density) { viewportSizePx.width.toDp() }.value
                val first = (floor(leftDp / COLUMN_WIDTH_DP).toInt() - COLUMN_BUFFER)
                    .coerceIn(0, stages.lastIndex)
                val last = (ceil(rightDp / COLUMN_WIDTH_DP).toInt() + COLUMN_BUFFER)
                    .coerceIn(0, stages.lastIndex)
                first..last
            }
        }
    }

    val visibleStagesIndexed by remember(stages) {
        derivedStateOf {
            stages.withIndex().filter { (index, _) -> index in visibleColRange }
        }
    }

    val visibleSetTimes by remember(setTimes) {
        derivedStateOf {
            setTimes.filter { st ->
                val col = stageIndexOf[st.stageId] ?: return@filter false
                col in visibleColRange &&
                    st.endHourFraction >= visibleHourRange.first &&
                    st.startHourFraction <= visibleHourRange.last
            }
        }
    }

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
                        color = stageColor,
                        lineHeight = 11.sp,
                    )
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
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(vertScroll)
                    .horizontalScroll(horizScroll)
                    .onSizeChanged { viewportSizePx = it },
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    visibleStagesIndexed.forEach { (_, stage) ->
                        Box(
                            modifier = Modifier.width(COLUMN_WIDTH_DP.dp)
                                .height(totalGridHeight)
                                .background(color = stageColor(stageId = stage.id).copy(alpha = 0.05f))
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

                    // Set time cards (only those intersecting the visible + buffered viewport)
                    visibleSetTimes.forEach { setTime ->
                        val stageIndex = stageIndexOf[setTime.stageId] ?: return@forEach
                        val startHourFraction = setTime.startHourFraction
                        val endHourFraction = setTime.endHourFraction
                        val topDp = ((startHourFraction - gridMinHour) * HOUR_HEIGHT_DP).dp
                        val topOffset = (2 + HOUR_LABEL_HEIGHT_DP / 2).dp
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
