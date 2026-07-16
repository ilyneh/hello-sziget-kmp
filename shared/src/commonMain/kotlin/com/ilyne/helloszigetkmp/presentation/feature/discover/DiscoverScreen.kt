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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.presentation.component.FavoriteIconButton
import com.ilyne.helloszigetkmp.presentation.component.filter.FilterBar
import com.ilyne.helloszigetkmp.presentation.component.filter.FilterBarData
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.component.pulltorefresh.PullToRefreshContent
import com.ilyne.helloszigetkmp.presentation.component.search.SearchTextField
import com.ilyne.helloszigetkmp.presentation.component.status.ErrorState
import com.ilyne.helloszigetkmp.presentation.component.status.LoadingBox
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
import com.ilyne.helloszigetkmp.presentation.feature.contentBottomInset
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilterScreen
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(modifier: Modifier = Modifier, onArtistClick: (String) -> Unit = {}) {
    val viewModel = koinViewModel<DiscoverViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiscoverEffect.LaunchFilterDialog -> showFilterDialog = true
            }
        }
    }

    PullToRefreshContent(
        isRefreshing = uiState.status == DiscoverUiState.Status.Loading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MainHeader(text = "Discover")
            SearchTextField(
                value = uiState.searchQuery,
                placeHolderText = "Search artists...",
                onValueChange = { viewModel.onIntent(DiscoverIntent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            FilterBar(
                data = FilterBarData(
                    filterCount = uiState.filterCount,
                    filterTexts = uiState.filterTexts,
                ),
                onFilterButtonClicked = { viewModel.onIntent(DiscoverIntent.OpenFilterDialog) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val status = uiState.status) {
                is DiscoverUiState.Status.Loading if uiState.artists.isEmpty() -> {
                    LoadingBox()
                }

                is DiscoverUiState.Status.Error -> {
                    ErrorState(
                        message = status.message ?: "Something went wrong",
                        onRetry = { viewModel.refresh() },
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = LocalBottomBarPadding.contentBottomInset,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.artists, key = { it.id }) { artist ->
                            ArtistCard(
                                artist = artist,
                                onFavoriteToggle = { viewModel.toggleFavorite(artist.id, artist.isFavorited) },
                                onClick = { onArtistClick(artist.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        DiscoverFilterScreen(
            initialFilter = uiState.filter,
            onSave = { filter ->
                viewModel.onIntent(DiscoverIntent.ApplyFilter(filter))
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit = {},
) {
    val coral = AppTheme.colors.coral
    val gradientEnd = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = coral)
                .padding(6.dp)
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = "${artist.name} image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(size = 8.dp))
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, gradientEnd),
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
                    color = coral,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,        // Minimum allowable size
                        maxFontSize = 14.sp,        // Maximum allowable size
                        stepSize = 0.5.sp             // Granularity of adjustment
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            FavoriteIconButton(
                enabled = artist.isFavorited,
                onClick = onFavoriteToggle,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
