package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.dayOfWeekLabel

@Composable
fun DaySelector(
    days: List<SetTimeDay>,
    selected: SetTimeDay?,
    onDaySelect: (SetTimeDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                    .run {
                        if (isSelected) {
                            border(
                                width = 2.dp,
                                shape = RoundedCornerShape(11.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                            )
                        } else {
                            this
                        }
                    }.clickable { onDaySelect(day) }
                    .padding(vertical = 6.dp)
                    .testTag("schedule_day_item_${day.dateOfMonth}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = day.dayOfWeekLabel().uppercase(),
                    fontSize = 10.sp,
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = day.dateOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
