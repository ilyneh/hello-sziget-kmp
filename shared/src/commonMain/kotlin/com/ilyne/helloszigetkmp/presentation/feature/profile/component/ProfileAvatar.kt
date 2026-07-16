package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.avatar.InitialsAvatar
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

@Composable
fun ProfileAvatar(
    avatarText: String,
    imageUrl: String? = null,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    InitialsAvatar(
        imageUrl = imageUrl,
        shape = RoundedCornerShape(percent = 32),
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.primary,
        shadowElevation = if (elevated) 8.dp else 0.dp,
        contentPadding = 2.dp,
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(4.dp),
            text = avatarText,
            color = AppTheme.colors.onAccent,
            maxLines = 1,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 16.sp, // Minimum allowable size
                maxFontSize = 80.sp, // Maximum allowable size
                stepSize = 1.sp, // Granularity of adjustment
            ),
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
