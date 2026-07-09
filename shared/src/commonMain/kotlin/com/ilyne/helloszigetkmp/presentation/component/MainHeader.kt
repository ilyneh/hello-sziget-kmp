package com.ilyne.helloszigetkmp.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette

@Composable
fun MainHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(color = SzigetPalette.Navy)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val offset = Offset(x = 8.0f, y = 6.0f)
        Text(
            text = text,
            color = SzigetPalette.SunshineYellow,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = SzigetPalette.Magenta,
                    offset = offset
                )
            ),
            modifier = Modifier.weight(1f),
        )
        trailingContent?.invoke()
    }
}

@Composable
fun SubHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        trailingContent?.invoke()
    }
}
