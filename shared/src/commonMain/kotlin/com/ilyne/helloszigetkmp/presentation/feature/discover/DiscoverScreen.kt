package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit = {},
) {
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
        modifier = modifier,
    ) {
        DiscoverScreenContent(
            uiState = uiState,
            viewModel = viewModel,
            onArtistClick = onArtistClick,
        )
    }

    if (showFilterDialog) {
        DiscoverFilterScreen(
            initialFilter = uiState.filter,
            onSave = { filter ->
                viewModel.onIntent(DiscoverIntent.ApplyFilter(filter))
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }
}

@Composable
private fun DiscoverScreenContent(
    uiState: DiscoverUiState,
    viewModel: DiscoverViewModel,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "Discover")
        SearchTextField(
            value = uiState.searchQuery,
            placeHolderText = "Search artists...",
            onValueChange = { viewModel.onIntent(DiscoverIntent.SearchQueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        FilterBar(
            data = FilterBarData(
                filterCount = uiState.filterCount,
                filterTexts = uiState.filterTexts,
            ),
            onFilterButtonClick = { viewModel.onIntent(DiscoverIntent.OpenFilterDialog) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
