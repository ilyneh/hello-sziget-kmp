package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.HOUR_HEIGHT_DP

const val HOUR_LABEL_HEIGHT_DP = 16

@Composable
fun StickyHourColumn(
    totalGridHeight: Dp,
    gridMinHour: Int,
    gridMaxHour: Int,
    modifier: Modifier = Modifier,
) {
    // Sticky hour column: only scrolls vertically (shares vertScroll with the grid),
    // never scrolls horizontally, so it stays pinned to the left edge.
    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .height(totalGridHeight)
            .fillMaxWidth(),
    ) {
        for (hour in gridMinHour..gridMaxHour) {
            val y = ((hour - gridMinHour) * HOUR_HEIGHT_DP).dp
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
