package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.theme.LightAppColors
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_filter_sliders
import org.jetbrains.compose.resources.painterResource


@Composable
fun ScheduleFilterButtonChip(
    filterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        selected = true,
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        colors = FilterChipDefaults.filterChipColors(
            iconColor = LightAppColors.onSecondary,
            containerColor = LightAppColors.secondary,
            labelColor = LightAppColors.onSecondary,
            disabledContainerColor = LightAppColors.background,
            disabledLabelColor = LightAppColors.onBackground,
            disabledLeadingIconColor = LightAppColors.onBackground,
            selectedContainerColor = LightAppColors.secondary,
            selectedLabelColor = LightAppColors.onSecondary,
            selectedLeadingIconColor = LightAppColors.onSecondary,
        ),
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    fontWeight = FontWeight.SemiBold
                )

                if (filterCount > 0) {
                    FilterCountLabel(text = "$filterCount")
                }
            }
        },
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_filter_sliders),
                contentDescription = "Filter Chip",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun FilterCountLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = LightAppColors.onPrimary,
        fontWeight = FontWeight.SemiBold,
        style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            )
        ),
        modifier = modifier
            .background(color = LightAppColors.primary, shape = RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 2.dp),
    )
}

@Preview
@Composable
private fun ScheduleFilterButtonChipPreview() {
    AppTheme {
        ScheduleFilterButtonChip(
            filterCount = 5,
            onClick = {},
        )
    }
}
