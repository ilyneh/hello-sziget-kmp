package com.ilyne.helloszigetkmp.presentation.component.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPill
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPillDefaults
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme


data class FilterBarData(
    val filterCount: Int = 0,
    val filterTexts: List<String> = emptyList(),
    val trailingText: String? = null
)

@Composable
fun FilterBar(
    data: FilterBarData,
    onFilterButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppTheme.colors.navy
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterButtonChip(
            filterCount = data.filterCount,
            onClick = onFilterButtonClick
        )

        if (data.trailingText != null) {
            Text(
                text = data.trailingText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (data.filterTexts.isNotEmpty()) {
            data.filterTexts.forEach {
                TextPill(
                    text = it,
                    style = TextPillDefaults.style.copy(fontSize = 12.sp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun FilterBarPreview() {
    AppTheme {
        FilterBar(
            data = FilterBarData(
                filterCount = 100,
                filterTexts = listOf(
                    "Electronic",
                    "Techno",
                    "Hip-Hop",
                )
            ),
            onFilterButtonClick = {},
        )
    }
}

@Preview
@Composable
private fun FilterBarWithTrailingTextPreview() {
    AppTheme {
        FilterBar(
            data = FilterBarData(
                filterCount = 100,
                trailingText = "1000 Sets",
                filterTexts = listOf(
                    "Electronic",
                    "Techno",
                    "Hip-Hop",
                )
            ),
            onFilterButtonClick = {},
        )
    }
}
