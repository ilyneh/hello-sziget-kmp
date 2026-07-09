package com.ilyne.helloszigetkmp.presentation.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.FriendRepository
import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase.GetFilteredScheduleContentUseCase
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val activeFilterCount: Int = 0,
    val status: Status = Status.Success,
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

    sealed class Status {
        object Success : Status()
        object Loading : Status()
        data class Error(val message: String) : Status()
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

    data class ApplyFilter(
        val filter: ScheduleFilter,
    ) : ScheduleIntent()
}

sealed class ScheduleEffect {
    data class NavigateToFilter(val filter: ScheduleFilter) : ScheduleEffect()
}

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val artistRepository: ArtistRepository,
    private val friendRepository: FriendRepository,
    private val getSetTimeDaysUseCase: GetSetTimeDaysUseCase,
    private val getFilteredScheduleContentUseCase: GetFilteredScheduleContentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ScheduleEffect>()
    val effects = _effects.asSharedFlow()

    private val selectedDay = MutableStateFlow<SetTimeDay?>(null)

    private val filter = MutableStateFlow(ScheduleFilter())

    init {
        viewModelScope.launch {
            try {
                scheduleRepository.refresh()
                artistRepository.refresh()
                friendRepository.refresh()
            } catch (e: Exception) {
                e.message?.let { message ->
                    _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
                }
            }
        }
        observeSetTimeDays()
        observeSelectedDay()
        observeFilter()
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
                        e.message?.let { message ->
                            _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
                        }
                    }
                }
            }
            is ScheduleIntent.OpenFilter -> {
                viewModelScope.launch {
                    _effects.emit(value = ScheduleEffect.NavigateToFilter(filter.value))
                }
            }

            is ScheduleIntent.ApplyFilter -> {
                filter.update { intent.filter }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedDay() {
        viewModelScope.launch {
            try {
                val setTimesForDay = selectedDay.filterNotNull()
                    .flatMapLatest { day ->
                        scheduleRepository.observeSetTimesForDay(day.dayStartMillis, day.dayEndMillis)
                    }

                val artistsFriendsFavorited = friendRepository.observeArtistsFriendsFavorited()
                    .map { favorited ->
                        favorited.associateBy { it.artist.id }
                    }

                data class CombinedData(
                    val filter: ScheduleFilter,
                    val setTimesForDay: ScheduleRepository.SetTimesForDay,
                    val favoritedByArtistId: Map<String, ArtistFriendsFavorited>,
                )

                combine(
                    flow = filter,
                    flow2 = setTimesForDay,
                    flow3 = artistsFriendsFavorited,
                ) { filter, setTimesForDay, favoritedByArtistId ->
                    CombinedData(filter, setTimesForDay, favoritedByArtistId)
                }.collect { data ->
                    val filteredData = getFilteredScheduleContentUseCase(
                        data.filter,
                        data.setTimesForDay.setTimes,
                        data.favoritedByArtistId,
                        data.setTimesForDay.stages,
                    )
                    _uiState.update {
                        it.copy(
                            setTimes = filteredData.setTimes,
                            stages = filteredData.stages,
                            gridMinHour = filteredData.gridMinHour,
                            gridMaxHour = filteredData.gridMaxHour,
                            status = ScheduleUiState.Status.Success
                        )
                    }
                }
            } catch (e: Exception) {
                e.message?.let { message ->
                    _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
                }
            }
        }
    }

    private fun observeFilter() {
        viewModelScope.launch {
            filter.collect { filter ->
                _uiState.update {
                    it.copy(activeFilterCount = filter.activeCount())
                }
            }
        }
    }

    private fun observeSetTimeDays() {
        viewModelScope.launch {
            try {
                getSetTimeDaysUseCase().collect { setTimeDays ->
                    _uiState.update { it.copy(days = setTimeDays.days) }
                }
            } catch (e: Exception) {
                e.message?.let { message ->
                    _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
                }
            }
        }
    }
}
