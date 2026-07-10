package com.ilyne.helloszigetkmp.presentation.feature.discover.filter

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverFilterUiState(
    val performanceTypes: List<DiscoverPerformanceTypeUiState> = emptyList(),
)

data class DiscoverPerformanceTypeUiState(
    val type: PerformanceType,
    val checkState: ToggleableState,
    val isExpanded: Boolean,
    val genres: List<DiscoverGenreUiState>,
)

data class DiscoverGenreUiState(
    val group: GenreGroup,
    val isChecked: Boolean,
)

sealed class DiscoverFilterEffect {
    data class UpdateFilter(
        val filter: DiscoverFilter
    ) : DiscoverFilterEffect()
}

sealed class DiscoverFilterIntent {
    data class Initialize(val filter: DiscoverFilter) : DiscoverFilterIntent()
    data class TogglePerformanceType(val type: PerformanceType) : DiscoverFilterIntent()
    data class ToggleGenreGroup(val group: GenreGroup, val value: Boolean) : DiscoverFilterIntent()
    data class ToggleGenreDropdown(val type: PerformanceType) : DiscoverFilterIntent()
    object Save : DiscoverFilterIntent()
}

class DiscoverFilterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverFilterUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DiscoverFilterEffect>()
    val effects = _effects.asSharedFlow()

    private var filter = DiscoverFilter()
    private var expandedTypes: Set<PerformanceType> = emptySet()

    init {
        updateUiState()
    }

    fun onIntent(intent: DiscoverFilterIntent) {
        when (intent) {
            is DiscoverFilterIntent.Initialize -> {
                filter = intent.filter
                updateUiState()
            }
            is DiscoverFilterIntent.TogglePerformanceType -> {
                val typeGenres = genreGroupsFor(intent.type).toSet()
                val allSelected = typeGenres.isNotEmpty() && filter.selectedGenreGroups.containsAll(typeGenres)
                filter = filter.copy(
                    selectedGenreGroups = if (allSelected) {
                        // Fully checked -> turn the whole type off, taking its genres with it.
                        filter.selectedGenreGroups - typeGenres
                    } else {
                        // Off or partially checked -> select all of the type's genres.
                        filter.selectedGenreGroups + typeGenres
                    }
                )
                updateUiState()
            }
            is DiscoverFilterIntent.ToggleGenreGroup -> {
                filter = filter.copy(
                    selectedGenreGroups = if (intent.value) {
                        filter.selectedGenreGroups + intent.group
                    } else {
                        filter.selectedGenreGroups - intent.group
                    }
                )
                updateUiState()
            }
            is DiscoverFilterIntent.ToggleGenreDropdown -> {
                expandedTypes = if (intent.type in expandedTypes) {
                    expandedTypes - intent.type
                } else {
                    expandedTypes + intent.type
                }
                updateUiState()
            }
            DiscoverFilterIntent.Save -> {
                viewModelScope.launch {
                    val effect = DiscoverFilterEffect.UpdateFilter(filter)
                    _effects.emit(effect)
                }
            }
        }
    }

    private fun updateUiState() {
        _uiState.update {
            it.copy(
                performanceTypes = PerformanceType.entries.map { type ->
                    val typeGenres = genreGroupsFor(type)
                    val selectedCount = typeGenres.count { it in filter.selectedGenreGroups }
                    val checkState = when {
                        selectedCount == 0 -> ToggleableState.Off
                        selectedCount == typeGenres.size -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    }
                    DiscoverPerformanceTypeUiState(
                        type = type,
                        checkState = checkState,
                        isExpanded = type in expandedTypes,
                        genres = typeGenres.map { group ->
                            DiscoverGenreUiState(
                                group = group,
                                isChecked = group in filter.selectedGenreGroups,
                            )
                        },
                    )
                },
            )
        }
    }
}
