package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme


data class ScheduleFilterBarData (
    val filterCount: Int = 2,
    val setCount: Int = 12,
    val filterItems: List<FilterItem> = emptyList()
) {
    data class FilterItem(
        val name: String
    )
}

@Composable
fun ScheduleFilterBar(
    data: ScheduleFilterBarData,
    onFilterButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScheduleFilterButtonChip(
            filterCount = data.filterCount,
            onClick = onFilterButtonClicked
        )

        if (data.setCount > 0) {
            Text(
                text = "${data.setCount} Sets",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (data.filterItems.isNotEmpty()) {
            data.filterItems.forEach { filterItem ->
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(100)
                        )

                ) {
                    Text(
                        text = filterItem.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

    }
}

@Preview
@Composable
private fun ScheduleFilterBarPreview() {
    AppTheme {
        ScheduleFilterBar(
            data = ScheduleFilterBarData(
                filterItems = listOf(
                    ScheduleFilterBarData.FilterItem("Electronic"),
                    ScheduleFilterBarData.FilterItem("Techno"),
                    ScheduleFilterBarData.FilterItem("Hip-Hop"),
                )
            ),
            onFilterButtonClicked = {},
        )
    }
}
