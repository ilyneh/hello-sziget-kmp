package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.swimlane

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
import com.ilyne.helloszigetkmp.presentation.feature.contentBottomInset
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HEADER_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HOUR_HEIGHT_DP
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.stageColor
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared.SetTimeCard
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared.SetTimeCardViewMode


// ── Swimlane View (Y = stage rows, X = time axis) ────────────────────────────

@Composable
fun SwimLaneView(
    setTimes: List<ScheduleUiState.SetTime>,
    stages: List<Stage>,
    gridMinHour: Int,
    gridMaxHour: Int,
    onArtistClick: (String) -> Unit = {},
    onToggleFavorite: (artistId: String?, current: Boolean) -> Unit = { _, _ -> },
) {
    val horizScroll = rememberScrollState()
    val vertListState = rememberLazyListState()

    if (setTimes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sets scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Time axis header
        Row(modifier = Modifier.horizontalScroll(horizScroll)) {
            Spacer(modifier = Modifier.width(80.dp))
            for (h in gridMinHour until gridMaxHour) {
                Box(modifier = Modifier.width(HOUR_HEIGHT_DP.dp).height(HEADER_HEIGHT_DP.dp), contentAlignment = Alignment.CenterStart) {
                    Text("${h % 24}:00", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = vertListState,
            contentPadding = PaddingValues(bottom = LocalBottomBarPadding.contentBottomInset),
        ) {
            items(stages, key = { it.id }) { stage ->
                val stageSets = setTimes.filter { it.stageId == stage.id }
                Row(
                    modifier = Modifier
                        .height(80.dp)
                        .background(color = stageColor(stage.id).copy(alpha = 0.2f))
                        .horizontalScroll(horizScroll),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(80.dp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        Text(
                            stage.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = stageColor(stage.id),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    val totalWidth = ((gridMaxHour - gridMinHour) * HOUR_HEIGHT_DP).dp
                    Box(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
                        stageSets.forEach { setTime ->
                            val leftDp = ((setTime.startHourFraction - gridMinHour) * HOUR_HEIGHT_DP).dp
                            val widthDp = ((setTime.endHourFraction - setTime.startHourFraction) * HOUR_HEIGHT_DP).dp

                            SetTimeCard(
                                setTime = setTime,
                                viewMode = SetTimeCardViewMode.SWIMLANE,
                                modifier = Modifier
                                    .offset(x = leftDp)
                                    .width(widthDp.coerceAtLeast(60.dp))
                                    .fillMaxHeight()
                                    .padding(2.dp),
                                onClick = { setTime.artistId?.let(onArtistClick) },
                                onToggleFavorite = {
                                    onToggleFavorite(
                                        setTime.artistId,
                                        setTime.artist?.isFavorited ?: false
                                    )
                                },
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
