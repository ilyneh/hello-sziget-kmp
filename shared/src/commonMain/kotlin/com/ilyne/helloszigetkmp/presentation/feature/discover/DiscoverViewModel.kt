package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFilterDialog: Boolean = false,
)

class DiscoverViewModel(
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            artistRepository.observeArtists().collect { artists ->
                _uiState.update { it.copy(artists = artists, isLoading = false) }
            }
        }
        viewModelScope.launch {
            try {
                artistRepository.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
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
}
