package com.ilyne.helloszigetkmp.presentation.feature.artistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val friendsFavorited: List<User> = emptyList(),
    val status: Status = Status.Loading,
) {
    sealed class Status {
        data object Loading : Status()
        data object Success : Status()
        data class Error(val message: String?) : Status()
    }
}

class ArtistDetailViewModel(
    private val artistRepository: ArtistRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedArtistId: String? = null

    fun load(artistId: String) {
        if (loadedArtistId == artistId) return
        loadedArtistId = artistId
        viewModelScope.launch {
            try {
                combine(
                    artistRepository.observeArtist(artistId),
                    friendRepository.observeArtistsFriendsFavorited(),
                ) { artist, artistsFriendsFavorited ->
                    artist to (artistsFriendsFavorited.find { it.artist.id == artistId }?.friendsFavorited ?: emptyList())
                }.collect { (artist, friendsFavorited) ->
                    _uiState.update {
                        it.copy(
                            artist = artist,
                            friendsFavorited = friendsFavorited,
                            status = ArtistDetailUiState.Status.Success,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = ArtistDetailUiState.Status.Error(e.message ?: "Failed to load artist"))
                }
            }
        }
    }

    fun toggleFavorite() {
        val artist = _uiState.value.artist ?: return
        viewModelScope.launch {
            try {
                artistRepository.toggleFavorite(artist.id, !artist.isFavorited)
            } catch (e: Exception) {
                // silently fail here, do not disrupt the view with error screen
            }
        }
    }
}
