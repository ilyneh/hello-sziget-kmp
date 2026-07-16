package com.ilyne.helloszigetkmp.presentation.component.pill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

object TextPillDefaults {
    val style: TextStyle
        @Composable get() = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
}

@Composable
fun TextPill(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextPillDefaults.style,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(size = 6.dp),
            ),
    ) {
        Text(
            text = text,
            style = style,
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Preview
@Composable
private fun TextPillPreview() {
    AppTheme {
        TextPill(
            text = "12:00",
            style = TextPillDefaults.style.copy(
                fontSize = 12.sp,
            ),
        )
    }
}
