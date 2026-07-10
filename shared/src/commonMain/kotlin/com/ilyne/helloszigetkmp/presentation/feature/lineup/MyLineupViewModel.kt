package com.ilyne.helloszigetkmp.presentation.feature.lineup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.util.datetime.formatDate
import com.ilyne.helloszigetkmp.util.datetime.toFestivalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.format

data class MyLineupUiState(
    val favoritesGroupedByDay: Map<String, List<SetTimeWithArtistStageSummary>> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val status: Status = Status.LOADING
) {

    sealed class Status {
        object SUCCESS : Status()
        object LOADING : Status()
        data class ERROR(val message: String?) : Status()
    }
}

class MyLineupViewModel(
    private val artistRepository: ArtistRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyLineupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.observeFavoriteSetTimes().collect { setTimes ->
                _uiState.update {
                    it.copy(
                        favoritesGroupedByDay = setTimes.groupByDay(),
                        status = MyLineupUiState.Status.SUCCESS
                    )
                }
            }
        }
    }

    private fun List<SetTimeWithArtistStageSummary>.groupByDay(): Map<String, List<SetTimeWithArtistStageSummary>> {
        return groupBy { setTime ->
            val festivalDate = setTime.startTime.toFestivalDate()
            festivalDate.formatDate()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                scheduleRepository.refresh()
                artistRepository.refresh()
            } catch (_: Exception) {
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                artistRepository.toggleFavorite(artistId, isFavorited = false)
            } catch (_: Exception) {
            }
        }
    }
}
