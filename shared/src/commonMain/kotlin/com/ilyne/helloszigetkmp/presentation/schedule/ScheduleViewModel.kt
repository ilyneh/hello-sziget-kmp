package com.ilyne.helloszigetkmp.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimesForDayUseCase
import com.ilyne.helloszigetkmp.util.Logger
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHourFraction
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

enum class ViewMode {
    GRID,
    SWIMLANE,
    LIST,
}

data class FestivalDay(
    val label: String,
    val startMillis: Long,
    val endMillis: Long,
)

data class ScheduleUiState(
    val days: List<SetTimeDay> = emptyList(),
    val selectedDay: SetTimeDay? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val stages: List<Stage> = emptyList(),
    val setTimes: List<SetTime> = emptyList(),
    val gridMinHour: Int = 0,
    val gridMaxHour: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    class SetTime(
        val id: String,
        val artistId: String,
        val stageId: String?,
        val startTime: Long, // epoch millis
        val endTime: Long, // epoch millis
        val hideEndTime: Boolean,
        val artist: Artist? = null,
        val stage: Stage? = null,
        val isInThePast: Boolean,
        val startHourFraction: Double,
        val endHourFraction: Double,
    )
}

sealed class ScheduleIntent {
    data class SelectDay(
        val day: SetTimeDay,
    ) : ScheduleIntent()

    data class ChangeViewMode(
        val mode: ViewMode,
    ) : ScheduleIntent()

    data class ToggleFavorite(
        val artistId: String?,
        val current: Boolean,
    ) : ScheduleIntent()
}

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val artistRepository: ArtistRepository,
    private val getSetTimeDaysUseCase: GetSetTimeDaysUseCase,
    private val getSetTimesForDayUseCase: GetSetTimesForDayUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    private var observeSelectedDayJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                scheduleRepository.refresh()
                artistRepository.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
        observeStages()
        observeSetTimeDays()
        observeSelectedDay()
    }

    fun onIntent(intent: ScheduleIntent) {
        when (intent) {
            is ScheduleIntent.SelectDay -> {
                _uiState.update { it.copy(selectedDay = intent.day) }
                observeSelectedDay()
            }

            is ScheduleIntent.ChangeViewMode -> {
                _uiState.update { it.copy(viewMode = intent.mode) }
            }

            is ScheduleIntent.ToggleFavorite -> {
                if (intent.artistId == null) {
                    Logger.e("findme", "Clicked on item with null artist id")
                    throw IllegalStateException("item with null artistId")
                }
                viewModelScope.launch {
                    try {
                        artistRepository.toggleFavorite(intent.artistId, isFavorited = !intent.current)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
        }
    }

    private fun observeSelectedDay() {
        val day = _uiState.value.selectedDay ?: return
        observeSelectedDayJob?.cancel()
        observeSelectedDayJob = viewModelScope.launch {
            val currentTimeMillis = Clock.System.now().toEpochMilliseconds()
            getSetTimesForDayUseCase(day.dayStartMillis, day.dayEndMillis).collect { data ->
                val setTimesUiModel = data.setTimes.map {
                    ScheduleUiState.SetTime(
                        id = it.id,
                        artistId = it.artistId,
                        stageId = it.stageId,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        hideEndTime = it.hideEndTime,
                        artist = it.artist,
                        stage = it.stage,
                        isInThePast = it.endTime < currentTimeMillis,
                        startHourFraction = it.startTime.normalizedFestivalHourFraction(),
                        endHourFraction = it.endTime.normalizedFestivalHourFraction(),
                    )
                }

                _uiState.update {
                    it.copy(
                        setTimes = setTimesUiModel,
                        gridMinHour = data.gridMinHour,
                        gridMaxHour = data.gridMaxHour,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun observeStages() {
        viewModelScope.launch {
            scheduleRepository.observeStages().collect { stages ->
                _uiState.update { it.copy(stages = stages) }
            }
        }
    }

    private fun observeSetTimeDays() {
        viewModelScope.launch {
            getSetTimeDaysUseCase().collect { setTimeDays ->
                _uiState.update { it.copy(days = setTimeDays.days) }
            }
        }
    }
}
