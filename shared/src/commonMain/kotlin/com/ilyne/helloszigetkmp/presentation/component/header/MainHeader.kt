package com.ilyne.helloszigetkmp.presentation.component.header

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.util.TableInfo
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

@Composable
fun MainHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .height(72.dp)
            .background(color = AppTheme.colors.navy)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val offset = Offset(x = 8.0f, y = 6.0f)
        Text(
            text = text,
            color = AppTheme.colors.highlightYellow,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = AppTheme.colors.highlightMagenta,
                    offset = offset
                )
            ),
            modifier = Modifier.weight(1f),
        )
        trailingContent?.invoke()
    }
}

@Composable
fun ModalHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppTheme.colors.highlightMagenta,
                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
            )

            trailingContent?.invoke()
        }

        val primaryLineColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier.fillMaxWidth()
        ) {
            drawLine(
                color = primaryLineColor,
                start = Offset(x = 0f, y = size.height / 2),
                end = Offset(x = size.width, y = size.height / 2),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}


@Preview
@Composable
private fun ModalHeaderPreview() {
    AppTheme {
        ModalHeader(
            text = "Add Friends"
        )
    }
}


@Composable
fun SubHeader2(
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        trailingContent?.invoke()
    }
}
