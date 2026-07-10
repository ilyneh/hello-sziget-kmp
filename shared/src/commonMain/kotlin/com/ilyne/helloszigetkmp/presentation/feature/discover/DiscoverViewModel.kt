package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFilterDialog: Boolean = false,
)

sealed class DiscoverIntent {
    data class SearchQueryChanged(
        val query: String,
    ) : DiscoverIntent()
}

class DiscoverViewModel(
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        observeArtists()
        viewModelScope.launch {
            try {
                artistRepository.refresh()
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

    fun openFilterDialog() {
        _uiState.update { it.copy(showFilterDialog = true) }
    }

    fun dismissFilterDialog() {
        _uiState.update { it.copy(showFilterDialog = false) }
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
