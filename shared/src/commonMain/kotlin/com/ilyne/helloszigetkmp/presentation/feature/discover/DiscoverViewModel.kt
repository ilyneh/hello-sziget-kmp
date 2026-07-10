package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.passesGenreFilter
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilter
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.usecase.GetActiveDiscoverFiltersTextUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
        data class Error(val message: String?) : Status()
    }
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

    /** Called from pull-to-refresh: always forces a fresh API fetch. */
    fun refresh() {
        refreshArtists(force = true)
    }

    private fun refreshArtists(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = DiscoverUiState.Status.Loading) }
            try {
                artistRepository.refresh(force = force)
            } catch (e: Exception) {
                _uiState.update { it.copy(status = DiscoverUiState.Status.Error(message = e.message)) }
            }
            _uiState.update { it.copy(status = DiscoverUiState.Status.Success) }
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
                artistRepository.toggleFavorite(artistId, !current)
            } catch (e: Exception) {
                _uiState.update { it.copy(status = DiscoverUiState.Status.Error(message = e.message)) }
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
            searchQuery.flatMapLatest { query ->
                if (query.isBlank()) {
                    artistRepository.observeArtists()
                } else {
                    artistRepository.searchArtists(query)
                }
            }.combine(filter) { artists, discoverFilter ->
                artists.filter { passesGenreFilter(it.tags, discoverFilter.selectedGenreGroups) }
            }.collect { artists ->
                _uiState.update { it.copy(artists = artists, status = DiscoverUiState.Status.Success) }
            }
        }
    }
}
