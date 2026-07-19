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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HEADER_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HOUR_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.stageColor
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared.SetTimeCard
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.schedule_no_sets_scheduled
import org.jetbrains.compose.resources.stringResource

// ── Grid View (Y = time, X = stage columns) ──────────────────────────────────

private const val MIN_COLUMN_WIDTH_DP = 120
private const val TIME_LABEL_WIDTH_DP = 48
private const val VISIBLE_HOUR_BUFFER = 1

@Composable
fun TimelineGridView(
    setTimes: List<ScheduleUiState.SetTime>,
    stages: List<Stage>,
    gridMinHour: Int,
    gridMaxHour: Int,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit = {},
    onToggleFavorite: (artistId: String?, current: Boolean) -> Unit = { _, _ -> },
) {
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    if (setTimes.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.schedule_no_sets_scheduled), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val stageIndexOf = remember(stages) { stages.withIndex().associate { (index, stage) -> stage.id to index } }

    val density = LocalDensity.current
    val hourHeightPx = with(density) { HOUR_HEIGHT_DP.dp.toPx() }
    var viewportSizePx by remember { mutableStateOf(IntSize.Zero) }

    val visibleHourRange = remember(gridMinHour, gridMaxHour) {
        derivedStateOf {
            val scrollHours = vertScroll.value / hourHeightPx
            val visibleHours = viewportSizePx.height / hourHeightPx
            val start = gridMinHour + scrollHours - VISIBLE_HOUR_BUFFER
            val end = gridMinHour + scrollHours + visibleHours + VISIBLE_HOUR_BUFFER
            start..end
        }
    }
    val visibleSetTimes by remember(setTimes, gridMinHour, gridMaxHour) {
        derivedStateOf {
            val range = visibleHourRange.value
            setTimes.filter { it.endHourFraction >= range.start && it.startHourFraction <= range.endInclusive }
        }
    }

    val totalGridHeight = ((gridMaxHour - gridMinHour) * HOUR_HEIGHT_DP).dp + LocalBottomBarPadding.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TIME_LABEL_WIDTH_DP.dp)
                    .horizontalScroll(horizScroll),
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
                        .verticalScroll(state = vertScroll),
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .onSizeChanged { viewportSizePx = it }
                        .verticalScroll(vertScroll)
                        .horizontalScroll(horizScroll),
                ) {
                    // Stage column color
                    Row(modifier = Modifier.fillMaxHeight()) {
                        stages.forEach { stage ->
                            Box(
                                modifier = Modifier
                                    .width(columnWidth)
                                    .height(totalGridHeight)
                                    .background(
                                        color = stageColor(stageId = stage.id)
                                            .copy(alpha = 0.2f),
                                    ),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(totalGridWidth, totalGridHeight)
                            .background(color = Color.Transparent),
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

                        // Set time cards — windowed to the visible (+buffer) hour range so we
                        // don't compose every set for the day at once.
                        visibleSetTimes.forEach { setTime ->
                            val stageIndex = stageIndexOf[setTime.stageId] ?: return@forEach
                            val startHourFraction = setTime.startHourFraction
                            val endHourFraction = setTime.endHourFraction
                            val topDp = ((startHourFraction - gridMinHour) * HOUR_HEIGHT_DP).dp
                            val topOffset = (2 + HOUR_LABEL_HEIGHT_DP / 2).dp
                            val heightDp = ((endHourFraction - startHourFraction) * HOUR_HEIGHT_DP - 3).dp
                            val leftDp = columnWidth * stageIndex + 2.dp

                            key(setTime.id) {
                                SetTimeCard(
                                    setTime = setTime,
                                    modifier = Modifier
                                        .offset(x = leftDp, y = topDp + topOffset)
                                        .width(columnWidth - 4.dp)
                                        .height(heightDp),
                                    onClick = { setTime.artistId?.let(onArtistClick) },
                                    onToggleFavorite = {
                                        onToggleFavorite(
                                            setTime.artistId,
                                            setTime.artist?.isFavorited ?: false,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
