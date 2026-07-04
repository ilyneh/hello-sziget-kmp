package com.ilyne.helloszigetkmp.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun MainHeaderLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier,
    )
}
