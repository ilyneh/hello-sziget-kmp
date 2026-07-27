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
import androidx.compose.ui.platform.testTag
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
import com.ilyne.helloszigetkmp.presentation.util.LoadStatus
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.common_something_went_wrong
import hello_sziget_kmp.shared.generated.resources.discover_search_placeholder
import hello_sziget_kmp.shared.generated.resources.discover_title
import org.jetbrains.compose.resources.stringResource
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
        isRefreshing = uiState.status == LoadStatus.Loading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.testTag("discover_screen"),
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
        MainHeader(text = stringResource(Res.string.discover_title))
        SearchTextField(
            value = uiState.searchQuery,
            placeHolderText = stringResource(Res.string.discover_search_placeholder),
            onValueChange = { viewModel.onIntent(DiscoverIntent.SearchQueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("discover_search_field"),
        )
        FilterBar(
            data = FilterBarData(
                filterCount = uiState.filterCount,
                filterTexts = uiState.filterTexts,
            ),
            onFilterButtonClick = { viewModel.onIntent(DiscoverIntent.OpenFilterDialog) },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
                .testTag("discover_filter_bar"),
        )

        when (val status = uiState.status) {
            is LoadStatus.Loading if uiState.artists.isEmpty() -> {
                LoadingBox(modifier = Modifier.fillMaxSize().testTag("discover_loading"))
            }

            is LoadStatus.Error -> {
                ErrorState(
                    message = status.reason ?: stringResource(Res.string.common_something_went_wrong),
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().testTag("discover_error_state"),
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
                    modifier = Modifier.testTag("discover_artist_grid"),
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
