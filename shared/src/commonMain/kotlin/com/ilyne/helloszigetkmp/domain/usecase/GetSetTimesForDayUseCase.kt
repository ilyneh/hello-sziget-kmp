package com.ilyne.helloszigetkmp.domain.usecase

import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHour
import com.ilyne.helloszigetkmp.util.datetime.toLocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetSetTimesForDayUseCase(
    private val scheduleRepository: ScheduleRepository
) {

    data class Data(
        val setTimes: List<SetTime>,
        val gridMinHour: Int,
        val gridMaxHour: Int,
    )

    fun observeSetTimesForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<Data> =
        scheduleRepository.observeSetTimesForDay(dayStartMillis, dayEndMillis).map { setTimes ->
            val validSetTimes = setTimes.filter { it.startTime != it.endTime }
            Data(
                setTimes = validSetTimes,
                gridMinHour = getGridMinHour(validSetTimes),
                gridMaxHour = getGridMaxHour(validSetTimes),
            )
        }

    private fun getGridMinHour(setTimes: List<SetTime>) =
        setTimes.minOfOrNull {
            it.startTime
                .toLocalDateTime()
                .hour
                .let(::normalizedFestivalHour)
        } ?: 0

    private fun getGridMaxHour(setTimes: List<SetTime>) =
        setTimes.maxOfOrNull {
            it.endTime
                .toLocalDateTime()
                .hour
                .let(::normalizedFestivalHour)
        }?.plus(1) ?: 0
}
