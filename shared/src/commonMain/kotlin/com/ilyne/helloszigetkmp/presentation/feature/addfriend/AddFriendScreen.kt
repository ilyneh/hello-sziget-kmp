package com.ilyne.helloszigetkmp.presentation.feature.addfriend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.header.SubHeader
import com.ilyne.helloszigetkmp.presentation.component.search.SearchTextField
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.component.AddFriendUserItem
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_cancel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<AddFriendViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AddFriendContent(
            uiState = uiState,
            onDismiss = onDismiss,
            onSearchQueryChanged = { viewModel.onIntent(AddFriendIntent.SearchQueryChanged(query = it)) },
            onAdd = { viewModel.onIntent(AddFriendIntent.SendFriendRequest(it)) },
            onAccept = { viewModel.onIntent(AddFriendIntent.AcceptFriendRequest(it)) },
        )
    }
}

@Composable
private fun AddFriendContent(
    uiState: AddFriendUiState,
    onDismiss: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAdd: (String) -> Unit,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        SubHeader(
            text = "Add Friends",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(Res.drawable.ic_cancel),
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchTextField(
                value = uiState.searchQuery,
                placeHolderText = "Search users...",
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            if (uiState.results.isEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    text = "No users found",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                uiState.results.forEach { result ->
                    AddFriendUserItem(
                        name = result.user.name,
                        status = result.status,
                        onAdd = { onAdd(result.user.id) },
                        onAccept = { onAccept(result.user.id) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AddFriendScreenPreview() {
    AppTheme {
        AddFriendContent(
            uiState = AddFriendUiState(),
            onDismiss = {},
            onSearchQueryChanged = {},
            onAdd = {},
            onAccept = {},
        )
    }
}
