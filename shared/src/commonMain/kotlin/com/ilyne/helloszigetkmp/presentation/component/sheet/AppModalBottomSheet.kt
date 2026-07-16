package com.ilyne.helloszigetkmp.presentation.component.sheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Thin wrapper around [ModalBottomSheet] for the repeated
 * `rememberModalBottomSheetState(skipPartiallyExpanded = true)` +
 * `ModalBottomSheet(containerColor = MaterialTheme.colorScheme.surface, ...)` shape used by every
 * bottom-sheet screen (add friend, artist detail, discover/schedule filters). Centralizes the
 * `@OptIn(ExperimentalMaterial3Api::class)` opt-in so call sites don't each need their own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}
