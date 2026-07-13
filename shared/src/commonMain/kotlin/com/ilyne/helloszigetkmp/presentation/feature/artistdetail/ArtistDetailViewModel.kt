package com.ilyne.helloszigetkmp.presentation.feature.artistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class ArtistDetailViewModel(
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedArtistId: String? = null

    fun load(artistId: String) {
        if (loadedArtistId == artistId) return
        loadedArtistId = artistId
        viewModelScope.launch {
            try {
                artistRepository.observeArtist(artistId).collect { artist ->
                    _uiState.update { it.copy(artist = artist, isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load artist") }
            }
        }
    }
}
