package com.ilyne.hello_sziget_kmp.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.hello_sziget_kmp.domain.model.SetTime

@Composable
fun SetTimeCard(
    setTime: SetTime,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val color = Color(setTime.stage?.color ?: 0xFF6C63FF)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
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
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTime(setTime.startTime),
                fontSize = 11.sp,
                color = color.copy(alpha = 0.8f),
            )
            setTime.artist?.genre?.let { genre ->
                Text(
                    text = genre,
                    fontSize = 11.sp,
                    color = color.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String {
    val totalMinutes = (epochMillis / 60_000) % (24 * 60)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
