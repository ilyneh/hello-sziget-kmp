package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ViewMode
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_view_toggle_grid
import hello_sziget_kmp.shared.generated.resources.ic_view_toggle_list
import hello_sziget_kmp.shared.generated.resources.ic_view_toggle_swimlane
import org.jetbrains.compose.resources.painterResource

@Composable
fun ViewModeToggle(
    current: ViewMode,
    onChange: (ViewMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(AppTheme.colors.navy)
            .border(width = 2.dp, color = AppTheme.colors.highlightMagenta, shape = RoundedCornerShape(9.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ViewMode.entries.forEach { mode ->
            val selected = current == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) AppTheme.colors.highlightMagenta else Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(horizontal = 6.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                val (resource, contentDescription) = when (mode) {
                    ViewMode.GRID -> Res.drawable.ic_view_toggle_grid to "Grid View"
                    ViewMode.SWIMLANE -> Res.drawable.ic_view_toggle_swimlane to "Swimlane View"
                    ViewMode.LIST -> Res.drawable.ic_view_toggle_list to "List View"
                }
                Icon(
                    painter = painterResource(resource),
                    contentDescription = contentDescription,
                    tint = if (selected) AppTheme.colors.highlightYellow else AppTheme.colors.hairline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ViewModelTogglePreview() {
    AppTheme {
        Box(modifier = Modifier
            .background(AppTheme.colors.navy)
            .padding(16.dp)
        ) {
            ViewModeToggle(
                current = ViewMode.SWIMLANE,
                onChange = {}
            )
        }
    }
}
