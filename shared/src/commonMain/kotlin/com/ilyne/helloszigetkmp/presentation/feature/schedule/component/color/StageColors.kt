package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color

import androidx.compose.ui.graphics.Color
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette

private val stagePalette = listOf(
    SzigetPalette.HotPink,
    SzigetPalette.PrimaryBlue,
    SzigetPalette.Magenta,
    SzigetPalette.TealGreen,
    SzigetPalette.Red,
    SzigetPalette.RedOrange,
    SzigetPalette.DarkTeal,
    SzigetPalette.Navy
)

fun stageColor(stageId: String?): Color =
    if (stageId != null) {
        stagePalette[stageId.hashCode().mod(stagePalette.size)]
    } else {
        Color.DarkGray
    }
