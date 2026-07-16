package com.ilyne.helloszigetkmp.presentation.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterStorage
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase.GetActiveFiltersTextUseCase
import com.ilyne.helloszigetkmp.presentation.feature.schedule.usecase.GetFilteredScheduleContentUseCase
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
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
    val activeFilterItemsText: List<String> = emptyList(),
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
    private val getActiveFiltersTextUseCase: GetActiveFiltersTextUseCase,
    private val scheduleFilterStorage: ScheduleFilterStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private companion object {
        const val TAG = "ScheduleViewModel"
    }

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ScheduleEffect>()
    val effects = _effects.asSharedFlow()

    private val selectedDay = MutableStateFlow<SetTimeDay?>(null)

    private val filter = MutableStateFlow(scheduleFilterStorage.read() ?: ScheduleFilter())

    init {
        refreshData(force = false)
        observeSetTimeDays()
        observeSelectedDay()
        observeFilter()
    }

    /** Called from pull-to-refresh: always forces a fresh API fetch. */
    fun refresh() {
        refreshData(force = true)
    }

    private fun refreshData(force: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(status = ScheduleUiState.Status.Loading) }
                scheduleRepository.refresh(force = force)
                artistRepository.refresh(force = force)
                friendRepository.refresh(force = force)
                _uiState.update { it.copy(status = ScheduleUiState.Status.Success) }
            } catch (e: Exception) {
                // Log the real exception rather than surfacing e.message directly: for a
                // network/auth failure (e.g. an expired session after the app sat idle for a
                // while) the message can carry a raw backend HTTP response body, which isn't
                // meant for end users, instead of user-facing copy.
                Logger.e(TAG, "refreshData(): failed to refresh schedule", e)
                val message = "Couldn't refresh the schedule. Pull to refresh to try again."
                _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
            }
        }
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
                        Logger.e(TAG, "onIntent(): failed to toggle favorite", e)
                        val message = "Couldn't update favorite. Please try again."
                        _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
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
                scheduleFilterStorage.save(intent.filter)
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
                }.map { data ->
                    getFilteredScheduleContentUseCase(
                        data.filter,
                        data.setTimesForDay.setTimes,
                        data.favoritedByArtistId,
                        data.setTimesForDay.stages,
                    )
                }.flowOn(ioDispatcher)
                    .collect { filteredData ->
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
                Logger.e(TAG, "observeSelectedDay(): failed to load schedule for the selected day", e)
                val message = "Couldn't load the schedule for this day."
                _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
            }
        }
    }

    private fun observeFilter() {
        viewModelScope.launch {
            filter.collect { filter ->
                _uiState.update {
                    it.copy(
                        activeFilterCount = filter.activeCount(),
                        activeFilterItemsText = getActiveFiltersTextUseCase(filter)
                    )
                }
            }
        }
    }

    private fun observeSetTimeDays() {
        viewModelScope.launch {
            try {
                combine(
                    flow = getSetTimeDaysUseCase(),
                    flow2 = filter,
                ) { setTimeDays, filter ->
                    if (filter.showExtraDays) {
                        setTimeDays.days
                    } else {
                        setTimeDays.days.filterNot { it.isExtraDay }
                    }
                }.flowOn(ioDispatcher)
                    .collect { filteredDays ->
                    _uiState.update { it.copy(days = filteredDays) }
                    if (selectedDay.value == null || selectedDay.value !in filteredDays) {
                        selectedDay.update { filteredDays.firstOrNull() }
                        _uiState.update { it.copy(selectedDay = selectedDay.value) }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "observeSetTimeDays(): failed to load festival days", e)
                val message = "Couldn't load festival days."
                _uiState.update { it.copy(status = ScheduleUiState.Status.Error(message)) }
            }
        }
    }
}
