package com.ilyne.helloszigetkmp.presentation.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.FriendRepository
import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimesForDayUseCase
import com.ilyne.helloszigetkmp.util.Logger
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHourFraction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

enum class ViewMode {
    GRID,
    SWIMLANE,
    LIST,
}

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
    data class SetTime(
        val id: String,
        val startTime: Long, // epoch millis
        val endTime: Long, // epoch millis
        val hideEndTime: Boolean,
        val artist: Artist? = null,
        val artistFriendsFavorited: ArtistFriendsFavorited? = null,
        val stage: Stage? = null,
        val isInThePast: Boolean,
        val startHourFraction: Double,
        val endHourFraction: Double,
    ) {
        val artistId: String? = artist?.id
        val stageId: String? = stage?.id
    }
}

sealed class ScheduleIntent {
    object OpenFilter : ScheduleIntent()
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
    private val friendRepository: FriendRepository,
    private val getSetTimeDaysUseCase: GetSetTimeDaysUseCase,
    private val getSetTimesForDayUseCase: GetSetTimesForDayUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    private val selectedDay = MutableStateFlow<SetTimeDay?>(null)

    init {
        viewModelScope.launch {
            try {
                scheduleRepository.refresh()
                artistRepository.refresh()
                friendRepository.refresh()
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
                selectedDay.update { intent.day }
                _uiState.update { it.copy(selectedDay = selectedDay.value) }
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
            is ScheduleIntent.OpenFilter -> {

            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedDay() {
        viewModelScope.launch {
            val setTimesForDay = selectedDay.filterNotNull().flatMapLatest { day ->
                getSetTimesForDayUseCase(day.dayStartMillis, day.dayEndMillis)
            }
            val artistsFriendsFavorited = friendRepository.observeArtistsFriendsFavorited().map { favorited ->
                favorited.associateBy { it.artist.id }
            }

            combine(setTimesForDay, artistsFriendsFavorited) { data, favoritedByArtistId ->
                data to favoritedByArtistId
            }.collect { (data, favoritedByArtistId) ->
                val currentTimeMillis = Clock.System.now().toEpochMilliseconds()
                val setTimesUiModel = data.setTimes.map { setTime ->
                    ScheduleUiState.SetTime(
                        id = setTime.id,
                        startTime = setTime.startTime,
                        endTime = setTime.endTime,
                        hideEndTime = setTime.hideEndTime,
                        artist = setTime.artist,
                        artistFriendsFavorited = setTime.artist?.let { artist ->
                            favoritedByArtistId[artist.id]
                                ?: ArtistFriendsFavorited(
                                    artist = artist,
                                    friendsFavorited = emptyList()
                                )
                        },
                        stage = setTime.stage,
                        isInThePast = setTime.endTime < currentTimeMillis,
                        startHourFraction = setTime.startTime.normalizedFestivalHourFraction(),
                        endHourFraction = setTime.endTime.normalizedFestivalHourFraction(),
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
