package com.ilyne.helloszigetkmp.presentation.component.actionbutton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
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

/** Default chrome for [ActionIconButton]: a sticker-like squircle — flat sunshine-yellow fill with a bold navy outline. */
object ActionButtonDefaults {
    val Size: Dp = 48.dp
    val BorderWidth: Dp = 2.dp
    val Shape: Shape = RoundedCornerShape(16.dp)
    val ContainerColor: Color = SzigetPalette.SunshineYellow
    val BorderColor: Color = SzigetPalette.Navy
    val ContentColor: Color = SzigetPalette.Navy
    val DisabledContainerColor = SzigetPalette.CreamCanvas
    val DisabledContentColor = SzigetPalette.Hairline
}

/**
 * Reusable "sticker" style text button: the same flat-fill, bold-border squircle chrome as
 * [ActionIconButton], sized for a text (or icon + text) label instead of a single icon.
 *
 * Defaults to the app's sunshine-yellow fill with a navy outline via [ActionButtonDefaults],
 * but every color/shape parameter can be overridden.
 */
@Composable
fun ActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ActionButtonDefaults.Shape,
    containerColor: Color = ActionButtonDefaults.ContainerColor,
    borderColor: Color = ActionButtonDefaults.BorderColor,
    contentColor: Color = ActionButtonDefaults.ContentColor,
    borderWidth: Dp = ActionButtonDefaults.BorderWidth,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = ActionButtonDefaults.DisabledContainerColor,
            disabledContentColor = ActionButtonDefaults.DisabledContentColor,
        ),
        border = BorderStroke(borderWidth, borderColor),
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        content()
    }
}

/** Convenience overload for the common case of a plain text label. */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ActionButtonDefaults.Shape,
    containerColor: Color = ActionButtonDefaults.ContainerColor,
    borderColor: Color = ActionButtonDefaults.BorderColor,
    contentColor: Color = ActionButtonDefaults.ContentColor,
    borderWidth: Dp = ActionButtonDefaults.BorderWidth,
    enabled: Boolean = true,
) {
    ActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor,
        contentColor = contentColor,
        borderWidth = borderWidth,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

@Preview
@Composable
private fun AppButtonPreview() {
    AppTheme {
        ActionButton(text = "Continue", onClick = {}, modifier = Modifier.padding(16.dp))
    }
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
fun ActionIconButton(
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
private fun ActionIconButtonPreview() {
    AppTheme {
        Box(contentAlignment = Alignment.Center) {
            ActionIconButton(onClick = {}) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                )
            }
        }
    }
}
