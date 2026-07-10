package com.ilyne.helloszigetkmp.presentation.feature.addfriend

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.component.AddFriendUserItem
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.component.search.SearchTextField
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_carat_right
import hello_sziget_kmp.shared.generated.resources.ic_discover
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddFriendScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<AddFriendViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { MainHeader(text = "Add Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_carat_right),
                            contentDescription = "Back",
                            modifier = Modifier.graphicsLayer { rotationZ = 180f },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SearchTextField(
                        value = uiState.searchQuery,
                        placeHolderText = "Search users...",
                        onValueChange = { viewModel.onIntent(AddFriendIntent.SearchQueryChanged(query = it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                if (uiState.results.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            text = "No users found",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                } else {
                    items(uiState.results, key = { it.user.id }) { result ->
                        AddFriendUserItem(
                            name = result.user.name,
                            status = result.status,
                            onAdd = { viewModel.onIntent(AddFriendIntent.SendFriendRequest(result.user.id)) },
                            onAccept = { viewModel.onIntent(AddFriendIntent.AcceptFriendRequest(result.user.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AddFriendScreenPreview() {
    AppTheme {
        AddFriendScreen(onBack = {})
    }
}
