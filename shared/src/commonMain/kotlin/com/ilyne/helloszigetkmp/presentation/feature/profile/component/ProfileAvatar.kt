package com.ilyne.helloszigetkmp.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ProfileAvatar(elevated: Boolean = false, modifier: Modifier = Modifier) {
    val modifier = if (elevated) modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 32)) else modifier
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(percent = 32))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center).padding(4.dp),
            text = "IH",
            color = Color.White,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 16.sp,        // Minimum allowable size
                maxFontSize = 80.sp,        // Maximum allowable size
                stepSize = 1.sp             // Granularity of adjustment
            ),
            fontWeight = FontWeight.ExtraBold
        )
    }
}
