package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import com.ilyne.helloszigetkmp.util.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime


// ── List View ─────────────────────────────────────────────────────────────────

@Composable
fun SetTimeListView(
    setTimes: List<ScheduleUiState.SetTime>,
    onToggleFavorite: (artistId: String?, current: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = setTimes.groupBy { setTime ->
        formatTimeForHeader(epochMillis = setTime.startTime)
    }

    Column(modifier = modifier) {
        HorizontalDivider(
            thickness = 4.dp,
            color = SzigetPalette.Magenta
        )

        LazyColumn(
            modifier = Modifier
                .background(color = SzigetPalette.LightBlue)
        ) {
            grouped.forEach { (header, setTimes) ->
                stickyHeader {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        SetTimeListHeader(
                            text = header,
                        )
                    }
                }

                val lastIndex = setTimes.lastIndex
                itemsIndexed(setTimes) { index, setTime ->
                    SetTimeListItem(
                        setTime = setTime,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 4.dp,
                        color = SzigetPalette.MediumBlue
                    )
                }
            }
        }
    }
}

private fun formatTimeForHeader(epochMillis: Long): String {
    val dateTime = epochMillis.toLocalDateTime()
    val roundedMinute = if (dateTime.minute < 30) 0 else 30
    val rounded = LocalDateTime(dateTime.year, dateTime.month, dateTime.day, dateTime.hour, roundedMinute)
    return rounded.formatTime()
}
