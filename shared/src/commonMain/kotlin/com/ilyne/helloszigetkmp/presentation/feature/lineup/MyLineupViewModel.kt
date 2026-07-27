package com.ilyne.helloszigetkmp.presentation.feature.lineup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.util.Logger
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
    val status: Status = Status.Loading,
) {
    sealed class Status {
        object Success : Status()

        object Loading : Status()

        data class Error(
            val reason: MyLineupErrorReason,
        ) : Status()
    }
}

enum class MyLineupErrorReason {
    REFRESH_FAILED,
    REMOVE_FAVORITE_FAILED,
}

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
                            status = MyLineupUiState.Status.Success,
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
            _uiState.update { it.copy(status = MyLineupUiState.Status.Loading) }
            try {
                scheduleRepository.refresh(force = true)
                artistRepository.refresh(force = true)
                _uiState.update { it.copy(status = MyLineupUiState.Status.Success) }
            } catch (e: Exception) {
                // Log the real exception rather than surfacing e.message directly: for a
                // network/auth failure the message can carry a raw backend HTTP response body,
                // which isn't meant for end users, instead of user-facing copy.
                Logger.e("MyLineupViewModel", "refresh(): failed to refresh lineup", e)
                _uiState.update {
                    it.copy(status = MyLineupUiState.Status.Error(MyLineupErrorReason.REFRESH_FAILED))
                }
            }
        }
    }

    fun removeFavorite(artistId: String) {
        viewModelScope.launch {
            try {
                artistRepository.toggleFavorite(artistId, isFavorited = false)
            } catch (e: Exception) {
                Logger.e("MyLineupViewModel", "removeFavorite(): failed to remove favorite for $artistId", e)
                _uiState.update {
                    it.copy(status = MyLineupUiState.Status.Error(MyLineupErrorReason.REMOVE_FAVORITE_FAILED))
                }
            }
        }
    }
}
