package com.ilyne.helloszigetkmp.presentation.schedule.components.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import com.ilyne.helloszigetkmp.util.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime


// ── List View ─────────────────────────────────────────────────────────────────

@Composable
fun SetTimeListView(
    setTimes: List<ScheduleUiState.SetTime>,
    onToggleFavorite: (artistId: String?, current: Boolean) -> Unit,
) {
    val grouped = setTimes.groupBy { setTime ->
        formatTimeForHeader(epochMillis = setTime.startTime)
    }

    LazyColumn {
        grouped.forEach { (header, setTimes) ->
            stickyHeader {
                SetTimeListHeader(
                    text = header,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val lastIndex = setTimes.lastIndex
            itemsIndexed(setTimes) { index, setTime ->
                SetTimeListItem(
                    setTime = setTime,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth()
                )

                if (index < lastIndex) {
                    HorizontalDivider()
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
