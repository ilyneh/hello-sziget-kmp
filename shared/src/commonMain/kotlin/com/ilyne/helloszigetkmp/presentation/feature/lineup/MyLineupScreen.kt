package com.ilyne.helloszigetkmp.presentation.feature.lineup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.lineup.LineupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyLineupScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<LineupViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "My Lineup")

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.favorites.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("♡", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No favorites yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap the heart on any artist to add them",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.favorites, key = { it.id }) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = artist.bio?.let { bio -> { Text(bio) } },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeFavorite(artist.id) }) {
                                    Text("♥", fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
