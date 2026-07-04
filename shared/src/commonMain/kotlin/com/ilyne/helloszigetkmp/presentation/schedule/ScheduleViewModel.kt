package com.ilyne.helloszigetkmp.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.presentation.schedule.model.ScheduleUiModel
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHour
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHourFraction
import com.ilyne.helloszigetkmp.util.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val setTimes: List<ScheduleUiModel.SetTime> = emptyList(),
    val gridMinHour: Int = 0,
    val gridMaxHour: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class ScheduleIntent {
    data class SelectDay(
        val day: SetTimeDay,
    ) : ScheduleIntent()

    data class ChangeViewMode(
        val mode: ViewMode,
    ) : ScheduleIntent()

    data class ToggleFavorite(
        val artistId: String,
        val current: Boolean,
    ) : ScheduleIntent()
}

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                scheduleRepository.refresh()
                artistRepository.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
        observeSelectedDay()
        observeSetTimeDays()
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
                viewModelScope.launch {
                    try {
                        artistRepository.toggleFavorite(intent.artistId, !intent.current)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
        }
    }

    private fun observeSelectedDay() {
        val day = _uiState.value.selectedDay ?: return
        viewModelScope.launch {
            val currentTimeMillis = Clock.System.now().toEpochMilliseconds()
            combine(
                scheduleRepository.observeSetTimesForDay(day.dayStartMillis, day.dayEndMillis),
                scheduleRepository.observeStages(),
            ) { setTimes, stages ->
                setTimes to stages
            }.collect { (setTimes, stages) ->
                val validSetTimes = setTimes.filter { it.startTime != it.endTime }
                val setTimesUiModel = validSetTimes.map {
                    ScheduleUiModel.SetTime(
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

                val gridMinHour = validSetTimes.minOfOrNull {
                    it.startTime
                        .toLocalDateTime()
                        .hour
                        .let(::normalizedFestivalHour)
                } ?: 0
                val gridMaxHour = validSetTimes
                    .maxOfOrNull {
                        it.endTime
                            .toLocalDateTime()
                            .hour
                            .let(::normalizedFestivalHour)
                    }?.plus(1) ?: 0

                _uiState.update {
                    it.copy(
                        setTimes = setTimesUiModel,
                        stages = stages,
                        gridMinHour = gridMinHour,
                        gridMaxHour = gridMaxHour,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun observeSetTimeDays() {
        viewModelScope.launch {
            scheduleRepository.observeSetTimeDays().collect { setTimeDays ->
                _uiState.update { it.copy(days = setTimeDays.days) }
            }
        }
    }

    private fun buildFestivalDays(): List<FestivalDay> {
        // Sziget 2026: Aug 6–11 (placeholder epoch values — replace with real dates)
        val dayLabels = listOf("WED 6", "THU 7", "FRI 8", "SAT 9", "SUN 10", "MON 11")
        val baseMillis = 1754524800000L // Aug 6 2026 00:00 UTC approximate
        val dayMs = 86_400_000L
        return dayLabels.mapIndexed { i, label ->
            FestivalDay(
                label = label,
                startMillis = baseMillis + i * dayMs,
                endMillis = baseMillis + (i + 1) * dayMs,
            )
        }
    }
}
