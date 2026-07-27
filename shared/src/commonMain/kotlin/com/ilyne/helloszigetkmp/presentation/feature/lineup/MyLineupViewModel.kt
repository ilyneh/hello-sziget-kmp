package com.ilyne.helloszigetkmp.presentation.feature.lineup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.presentation.util.LoadStatus
import com.ilyne.helloszigetkmp.util.datetime.formatDate
import com.ilyne.helloszigetkmp.util.datetime.toFestivalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyLineupUiState(
    val favoritesGroupedByDay: Map<String, List<SetTimeWithArtistStageSummary>> = emptyMap(),
    val status: LoadStatus<String?> = LoadStatus.Loading,
)

class MyLineupViewModel(
    private val artistRepository: ArtistRepository,
    private val scheduleRepository: ScheduleRepository,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyLineupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository
                .observeFavoriteSetTimes()
                .map { setTimes -> setTimes.groupByDay() }
                .flowOn(backgroundDispatcher)
                .collect { groupedByDay ->
                    _uiState.update {
                        it.copy(
                            favoritesGroupedByDay = groupedByDay,
                            status = LoadStatus.Success,
                        )
                    }
                }
        }
    }

    private fun List<SetTimeWithArtistStageSummary>.groupByDay(): Map<String, List<SetTimeWithArtistStageSummary>> =
        groupBy { setTime ->
            val festivalDate = setTime.startTime.toFestivalDate()
            festivalDate.formatDate()
        }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = LoadStatus.Loading) }
            try {
                scheduleRepository.refresh(force = true)
                artistRepository.refresh(force = true)
                _uiState.update { it.copy(status = LoadStatus.Success) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = LoadStatus.Error(reason = e.message)) }
            }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                artistRepository.toggleFavorite(artistId, isFavorited = false)
            } catch (e: Exception) {
                _uiState.update { it.copy(status = LoadStatus.Error(reason = e.message)) }
            }
        }
    }
}
