package com.ilyne.helloszigetkmp.presentation.component.actionbutton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource

/** Default chrome for [ActionButton]: a sticker-like squircle — flat sunshine-yellow fill with a bold navy outline. */
object ActionButtonDefaults {
    val Size: Dp = 48.dp
    val BorderWidth: Dp = 2.dp
    val Shape: Shape = RoundedCornerShape(16.dp)
    val ContainerColor: Color = SzigetPalette.SunshineYellow
    val BorderColor: Color = SzigetPalette.Navy
    val ContentColor: Color = SzigetPalette.Navy
}

/**
 * Reusable "sticker" style icon button: a rounded-square (squircle) shape with a solid flat-color
 * fill and a bold, solid-color border — a neo-brutalist look rather than a soft elevation shadow.
 *
 * Defaults to the app's sunshine-yellow fill with a navy outline, but every color/shape/size
 * parameter can be overridden so this stays a single generic building block rather than a
 * one-off styled for a specific screen.
 */
@Composable
fun ActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ActionButtonDefaults.Size,
    shape: Shape = ActionButtonDefaults.Shape,
    containerColor: Color = ActionButtonDefaults.ContainerColor,
    borderColor: Color = ActionButtonDefaults.BorderColor,
    contentColor: Color = ActionButtonDefaults.ContentColor,
    borderWidth: Dp = ActionButtonDefaults.BorderWidth,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = IconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        modifier = modifier
            .size(size)
            .border(BorderStroke(borderWidth, borderColor), shape),
    ) {
        content()
    }
}

@Preview
@Composable
private fun ActionButtonPreview() {
    AppTheme {
        Box(contentAlignment = Alignment.Center) {
            ActionButton(onClick = {}) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                )
            }
        }
    }
}
