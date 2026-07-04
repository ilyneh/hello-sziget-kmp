package com.ilyne.helloszigetkmp.presentation.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.compose.MainHeader
import com.ilyne.helloszigetkmp.domain.model.Artist
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<DiscoverViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "Discover")

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.artists, key = { it.id }) { artist ->
                        ArtistCard(
                            artist = artist,
                            onFavoriteToggle = { viewModel.toggleFavorite(artist.id, artist.isFavorited) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onFavoriteToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.8f)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column {
                Spacer(modifier = Modifier.weight(1f))
                Text(artist.name, fontWeight = FontWeight.Bold, maxLines = 1)
                artist.bio?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            IconButton(onClick = onFavoriteToggle, modifier = Modifier.align(Alignment.TopEnd)) {
                Text(if (artist.isFavorited) "♥" else "♡", fontSize = 20.sp)
            }
        }
    }
}
