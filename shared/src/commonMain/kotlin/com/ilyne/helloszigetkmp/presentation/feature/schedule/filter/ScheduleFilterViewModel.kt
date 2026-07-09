package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class ScheduleFilterUiState(
    val showFavoritesOnly: Boolean = false,
    val showFriendsGoing: Boolean = false,
    val hideEmptyStages: Boolean = true
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
    object Save : FilterIntent()
}


class ScheduleFilterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleFilterUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<FilterEffect>()
    val effects = _effects.asSharedFlow()

    private var filter = ScheduleFilter()

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
                filter = filter.copy(showFavoritesOnly = intent.value)
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
            FilterIntent.Save -> {
                val effect = FilterEffect.UpdateFilter(filter)
                _effects.tryEmit(effect)
            }
        }
    }

    private fun updateUiState() {
        _uiState.update {
            it.copy(
                showFavoritesOnly = filter.showFavoritesOnly,
                showFriendsGoing = filter.showFriendsGoing,
                hideEmptyStages = filter.hideEmptyStages
            )
        }
    }
}
