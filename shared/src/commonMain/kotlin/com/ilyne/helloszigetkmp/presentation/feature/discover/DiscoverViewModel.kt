package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class DiscoverIntent {
    data class SearchQueryChanged(
        val query: String,
    ) : DiscoverIntent()

    data object OpenFilterDialog : DiscoverIntent()
}

sealed class DiscoverEffect {
    data object LaunchFilterDialog : DiscoverEffect()
}

class DiscoverViewModel(
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DiscoverEffect>()
    val effects = _effects.asSharedFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        observeArtists()
        refreshArtists(force = false)
    }

    /** Called from pull-to-refresh: always forces a fresh API fetch. */
    fun refresh() {
        refreshArtists(force = true)
    }

    private fun refreshArtists(force: Boolean) {
        viewModelScope.launch {
            try {
                artistRepository.refresh(force = force)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: DiscoverIntent) {
        when (intent) {
            is DiscoverIntent.SearchQueryChanged -> {
                searchQuery.update { intent.query }
                _uiState.update { it.copy(searchQuery = intent.query) }
            }

            is DiscoverIntent.OpenFilterDialog -> {
                viewModelScope.launch {
                    _effects.emit(value = DiscoverEffect.LaunchFilterDialog)
                }
            }
        }
    }

    fun toggleFavorite(
        artistId: String,
        current: Boolean,
    ) {
        viewModelScope.launch {
            try {
                artistRepository.toggleFavorite(artistId, !current)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeArtists() {
        viewModelScope.launch {
            searchQuery.flatMapLatest { query ->
                if (query.isBlank()) {
                    artistRepository.observeArtists()
                } else {
                    artistRepository.searchArtists(query)
                }
            }.collect { artists ->
                _uiState.update { it.copy(artists = artists, isLoading = false) }
            }
        }
    }
}
