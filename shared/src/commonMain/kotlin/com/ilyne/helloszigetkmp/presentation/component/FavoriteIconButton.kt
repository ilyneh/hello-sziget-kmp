package com.ilyne.helloszigetkmp.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

@Composable
fun FavoriteIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        HeartIcon(
            enabled = enabled,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun FavoriteIconButtonEnabledPreview() {
    AppTheme {
        FavoriteIconButton(enabled = true, onClick = {})
    }
}

@Preview
@Composable
private fun FavoriteIconButtonDisabledPreview() {
    AppTheme {
        FavoriteIconButton(enabled = false, onClick = {})
    }
}
