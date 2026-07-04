package com.ilyne.helloszigetkmp.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.schedule.components.color.artistColor
import com.ilyne.helloszigetkmp.presentation.schedule.model.ScheduleUiModel
import com.ilyne.helloszigetkmp.util.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format

@Composable
fun SetTimeCard(
    setTime: ScheduleUiModel.SetTime,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val color = setTime.artist?.let(::artistColor) ?: Color.Gray
    val backgroundAlpha = if (setTime.isInThePast) 0.55f else 1f
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = backgroundAlpha))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column {
            Text(
                text = setTime.artist?.name ?: "Unknown",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                lineHeight = 18.sp,
            )
            Text(
                text = formatTime(setTime.startTime),
                fontSize = 11.sp,
                color = Color.White,
            )
        }
    }
}

private fun formatTime(epochMillis: Long): String {
    val timeFormat = LocalDateTime.Format {
        hour()
        chars(":")
        minute()
    }
    return epochMillis.toLocalDateTime().format(timeFormat)
}
