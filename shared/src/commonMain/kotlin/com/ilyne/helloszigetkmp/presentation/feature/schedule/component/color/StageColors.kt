package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color

import androidx.compose.ui.graphics.Color

private val stagePalette = listOf(
    Color(0xFFE63950),
    Color(0xFF3B9AE1),
    Color(0xFF8C63E6),
    Color(0xFFE6A23B),
    Color(0xFF2FAE7C),
    Color(0xFFE057A0),
)

fun stageColor(stageId: String?): Color =
    if (stageId != null) {
        stagePalette[stageId.hashCode().mod(stagePalette.size)]
    } else {
        Color.DarkGray
    }
