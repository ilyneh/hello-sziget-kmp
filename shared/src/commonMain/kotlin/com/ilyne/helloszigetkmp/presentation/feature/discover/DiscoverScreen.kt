package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.presentation.component.HeartIcon
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.component.search.SearchTextField
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilterScreen
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.component.DiscoverFilterBar
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.component.DiscoverFilterBarData
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<DiscoverViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "Discover")

        SearchTextField(
            value = "",
            placeHolderText = "Search artists...",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp)
        )

        DiscoverFilterBar(
            data = DiscoverFilterBarData(),
            onFilterButtonClicked = { viewModel.openFilterDialog() },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

    if (uiState.showFilterDialog) {
        DiscoverFilterScreen(
            onDismiss = { viewModel.dismissFilterDialog() }
        )
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onFavoriteToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = SzigetPalette.Coral)
                .padding(6.dp)
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = "${artist.name} image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(size = 8.dp))
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, SzigetPalette.PrimaryBlue),
                                startY = size.height * 0.5f, // Adjust where the shadow begins fading in
                                endY = size.height        // Ends perfectly at the bottom edge
                            )
                        )
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = artist.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SzigetPalette.Coral,
                    maxLines = 2,
                    lineHeight = 1.1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                HeartIcon(
                    enabled = artist.isFavorited,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
