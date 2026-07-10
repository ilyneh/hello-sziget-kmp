package com.ilyne.helloszigetkmp.presentation.component.actionbutton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

/**
 * Reusable "sticker" style text button: the same flat-fill, bold-border squircle chrome as
 * [ActionButton], sized for a text (or icon + text) label instead of a single icon.
 *
 * Defaults to the app's sunshine-yellow fill with a navy outline via [ActionButtonDefaults],
 * but every color/shape parameter can be overridden.
 */
@Composable
fun AppButton(
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
            disabledContainerColor = ActionButtonDefaults.disabledContainerColor,
            disabledContentColor = ActionButtonDefaults.disabledContentColor,
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
fun AppButton(
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
    AppButton(
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
        AppButton(text = "Continue", onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
