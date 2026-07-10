package com.ilyne.helloszigetkmp.presentation.feature.discover.filter.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPill
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPillDefaults
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette


data class DiscoverFilterBarData(
    val filterCount: Int = 0,
    val filterTexts: List<String> = emptyList()
)

@Composable
fun DiscoverFilterBar(
    data: DiscoverFilterBarData,
    onFilterButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = SzigetPalette.Navy)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiscoverFilterButtonChip(
            filterCount = data.filterCount,
            onClick = onFilterButtonClicked
        )

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
private fun DiscoverFilterBarPreview() {
    AppTheme {
        DiscoverFilterBar(
            data = DiscoverFilterBarData(
                filterCount = 100,
                filterTexts = listOf(
                    "Electronic",
                    "Techno",
                    "Hip-Hop",
                )
            ),
            onFilterButtonClicked = {},
        )
    }
}
