package com.ilyne.helloszigetkmp.presentation.lineup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LineupUiState(
    val favorites: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
)

class LineupViewModel(private val artistRepository: ArtistRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LineupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            artistRepository.observeFavorites().collect { artists ->
                _uiState.update { it.copy(favorites = artists, isLoading = false) }
            }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try { artistRepository.toggleFavorite(artistId, false) }
            catch (_: Exception) {}
        }
    }
}
