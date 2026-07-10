package com.ilyne.helloszigetkmp.presentation.feature.discover.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.component.header.SubHeader
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverFilterScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        DiscoverFilterContent()
    }
}

@Composable
private fun DiscoverFilterContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        SubHeader(text = "Filters")
    }
}

@Preview
@Composable
private fun DiscoverFilterContentPreview() {
    AppTheme {
        DiscoverFilterContent()
    }
}
