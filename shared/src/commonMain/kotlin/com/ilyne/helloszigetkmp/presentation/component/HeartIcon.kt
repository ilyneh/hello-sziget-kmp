package com.ilyne.helloszigetkmp.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_heart_fluid
import hello_sziget_kmp.shared.generated.resources.ic_heart_fluid_filled
import org.jetbrains.compose.resources.painterResource

@Composable
fun HeartIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        val res = if (enabled) Res.drawable.ic_heart_fluid_filled else Res.drawable.ic_heart_fluid
        Icon(
            painter = painterResource(res),
            contentDescription = null,
            tint = Color.Unspecified,
        )
    }
}

@Preview
@Composable
private fun HeartIconEnabledPreview() {
    AppTheme {
        HeartIcon(enabled = true)
    }
}

@Preview
@Composable
private fun HeartIconDisabledPreview() {
    AppTheme {
        HeartIcon(enabled = false)
    }
}
