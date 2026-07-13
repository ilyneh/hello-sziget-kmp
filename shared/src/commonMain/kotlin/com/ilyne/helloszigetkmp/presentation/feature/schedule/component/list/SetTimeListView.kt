package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
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
    onArtistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val grouped = setTimes.groupBy { setTime ->
        formatTimeForHeader(epochMillis = setTime.startTime)
    }

    Column(modifier = modifier
        .background(color = SzigetPalette.LightBlue)
    ) {
        HorizontalDivider(
            thickness = 4.dp,
            color = SzigetPalette.Magenta
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = LocalBottomBarPadding.current + 16.dp),
        ) {
            grouped.forEach { (header, setTimes) ->
                stickyHeader {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        SetTimeListHeader(text = header,)
                    }
                }

                items(
                    items = setTimes,
                    key = { it.id }
                ) { setTime ->
                    SetTimeListItem(
                        setTime = setTime,
                        onToggleFavorite = onToggleFavorite,
                        onArtistClick = onArtistClick,
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
