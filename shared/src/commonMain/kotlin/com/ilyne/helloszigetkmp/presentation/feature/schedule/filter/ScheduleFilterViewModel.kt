package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

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


data class ScheduleFilterUiState(
    val showFavoritesOnly: Boolean = false,
    val showFriendsGoing: Boolean = false,
    val hideEmptyStages: Boolean = true,
    val showExtraDays: Boolean = false,
    val performanceTypes: List<PerformanceTypeUiState> = emptyList(),
)

data class PerformanceTypeUiState(
    val type: PerformanceType,
    val isChecked: Boolean,
    val isExpanded: Boolean,
    val genres: List<GenreUiState>,
)

data class GenreUiState(
    val group: GenreGroup,
    val isChecked: Boolean,
)

sealed class FilterEffect {
    data class UpdateFilter(
        val filter: ScheduleFilter
    ) : FilterEffect()
}

sealed class FilterIntent {
    data class Initialize(val filter: ScheduleFilter) : FilterIntent()
    data class ToggleFavoritesOnly(val value: Boolean) : FilterIntent()
    data class ToggleFriendsGoing(val value: Boolean) : FilterIntent()
    data class ToggleHideEmptyStages(val value: Boolean) : FilterIntent()
    data class ToggleShowExtraDays(val value: Boolean) : FilterIntent()
    data class TogglePerformanceType(val type: PerformanceType, val value: Boolean) : FilterIntent()
    data class ToggleGenreGroup(val group: GenreGroup, val value: Boolean) : FilterIntent()
    data class ToggleGenreDropdown(val type: PerformanceType) : FilterIntent()
    object Save : FilterIntent()
}


class ScheduleFilterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleFilterUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<FilterEffect>()
    val effects = _effects.asSharedFlow()

    private var filter = ScheduleFilter()
    private var expandedTypes: Set<PerformanceType> = emptySet()

    init {
        updateUiState()
    }

    fun onIntent(intent: FilterIntent) {
        when (intent) {
            is FilterIntent.Initialize -> {
                filter = intent.filter
                updateUiState()
            }
            is FilterIntent.ToggleFavoritesOnly -> {
                filter = filter.copy(showFavorites = intent.value)
                updateUiState()
            }
            is FilterIntent.ToggleFriendsGoing -> {
                filter = filter.copy(showFriendsGoing = intent.value)
                updateUiState()
            }
            is FilterIntent.ToggleHideEmptyStages -> {
                filter = filter.copy(hideEmptyStages = intent.value)
                updateUiState()
            }
            is FilterIntent.ToggleShowExtraDays -> {
                filter = filter.copy(showExtraDays = intent.value)
                updateUiState()
            }
            is FilterIntent.TogglePerformanceType -> {
                filter = filter.copy(
                    selectedPerformanceTypes = if (intent.value) {
                        filter.selectedPerformanceTypes + intent.type
                    } else {
                        filter.selectedPerformanceTypes - intent.type
                    }
                )
                updateUiState()
            }
            is FilterIntent.ToggleGenreGroup -> {
                filter = filter.copy(
                    selectedGenreGroups = if (intent.value) {
                        filter.selectedGenreGroups + intent.group
                    } else {
                        filter.selectedGenreGroups - intent.group
                    }
                )
                updateUiState()
            }
            is FilterIntent.ToggleGenreDropdown -> {
                expandedTypes = if (intent.type in expandedTypes) {
                    expandedTypes - intent.type
                } else {
                    expandedTypes + intent.type
                }
                updateUiState()
            }
            FilterIntent.Save -> {
                viewModelScope.launch {
                    val effect = FilterEffect.UpdateFilter(filter)
                    _effects.emit(effect)
                }
            }
        }
    }

    private fun updateUiState() {
        _uiState.update {
            it.copy(
                showFavoritesOnly = filter.showFavorites,
                showFriendsGoing = filter.showFriendsGoing,
                hideEmptyStages = filter.hideEmptyStages,
                showExtraDays = filter.showExtraDays,
                performanceTypes = PerformanceType.entries.map { type ->
                    PerformanceTypeUiState(
                        type = type,
                        isChecked = type in filter.selectedPerformanceTypes,
                        isExpanded = type in expandedTypes,
                        genres = genreGroupsFor(type).map { group ->
                            GenreUiState(
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
