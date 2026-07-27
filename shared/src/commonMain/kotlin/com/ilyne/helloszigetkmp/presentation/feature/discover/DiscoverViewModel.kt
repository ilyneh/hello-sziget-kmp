package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.passesGenreFilter
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilter
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.usecase.GetActiveDiscoverFiltersTextUseCase
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val filter: DiscoverFilter = DiscoverFilter(),
    val filterCount: Int = 0,
    val filterTexts: List<String> = emptyList(),
    val status: Status = Status.Loading,
) {
    sealed class Status {
        object Success : Status()

        object Loading : Status()

        data class Error(
            val reason: DiscoverErrorReason,
        ) : Status()
    }
}

enum class DiscoverErrorReason {
    REFRESH_FAILED,
}

sealed class DiscoverIntent {
    data class SearchQueryChanged(
        val query: String,
    ) : DiscoverIntent()

    data object OpenFilterDialog : DiscoverIntent()

    data class ApplyFilter(
        val filter: DiscoverFilter,
    ) : DiscoverIntent()
}

sealed class DiscoverEffect {
    data object LaunchFilterDialog : DiscoverEffect()
}

class DiscoverViewModel(
    private val artistRepository: ArtistRepository,
    private val getActiveDiscoverFiltersTextUseCase: GetActiveDiscoverFiltersTextUseCase,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DiscoverEffect>()
    val effects = _effects.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(DiscoverFilter())

    init {
        observeArtists()
        observeFilter()
        refreshArtists(force = false)
    }

    fun refresh() {
        refreshArtists(force = true)
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

            is DiscoverIntent.ApplyFilter -> {
                filter.update { intent.filter }
            }
        }
    }

    fun toggleFavorite(
        artistId: String,
        current: Boolean,
    ) {
        viewModelScope.launch {
            try {
                artistRepository.toggleFavorite(
                    artistId = artistId,
                    isFavorited = !current,
                )
            } catch (e: Exception) {
                // silently fail here, do not disrupt the view with error screen
                Logger.e("DiscoverViewModel", "Failed to toggle favorite for $artistId", e)
            }
        }
    }

    private fun observeFilter() {
        viewModelScope.launch {
            filter.collect { discoverFilter ->
                _uiState.update {
                    it.copy(
                        filter = discoverFilter,
                        filterCount = discoverFilter.activeCount(),
                        filterTexts = getActiveDiscoverFiltersTextUseCase(discoverFilter),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeArtists() {
        viewModelScope.launch {
            searchQuery
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        artistRepository.observeArtists()
                    } else {
                        artistRepository.searchArtists(query)
                    }
                }.combine(filter) { artists, discoverFilter ->
                    artists.filter { passesGenreFilter(it.tags, discoverFilter.selectedGenreGroups) }
                }.flowOn(backgroundDispatcher)
                .collect { artists ->
                    // Offloading this filtering onto backgroundDispatcher means the resulting emission
                    // can now land after a concurrent operation (e.g. toggleFavorite) has already
                    // set an Error status; don't let a routine list refresh silently clobber it.
                    _uiState.update {
                        val nextStatus = it.status as? DiscoverUiState.Status.Error ?: DiscoverUiState.Status.Success
                        it.copy(artists = artists, status = nextStatus)
                    }
                }
        }
    }

    private fun refreshArtists(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = DiscoverUiState.Status.Loading) }
            try {
                artistRepository.refresh(force = force)
                _uiState.update { it.copy(status = DiscoverUiState.Status.Success) }
            } catch (e: Exception) {
                // Log the real exception rather than surfacing e.message directly: for a
                // network/auth failure the message can carry a raw backend HTTP response body,
                // which isn't meant for end users, instead of user-facing copy.
                Logger.e("DiscoverViewModel", "refreshArtists(): failed to refresh artists", e)
                _uiState.update {
                    it.copy(status = DiscoverUiState.Status.Error(DiscoverErrorReason.REFRESH_FAILED))
                }
            }
        }
    }
}
