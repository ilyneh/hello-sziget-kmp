package com.ilyne.helloszigetkmp.domain.usecase

import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.SetTimeDays
import com.ilyne.helloszigetkmp.util.datetime.DateTimeUtils
import com.ilyne.helloszigetkmp.util.datetime.FESTIVAL_DAY_CUTOFF_HOURS
import com.ilyne.helloszigetkmp.util.datetime.toFestivalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.hours

class GetSetTimeDaysUseCase(
    private val scheduleRepository: ScheduleRepository
) {

    private companion object {
        const val TAG = "GetSetTimeDaysUseCase"

    }

    operator fun invoke(): Flow<SetTimeDays> {
        return scheduleRepository.observeSetTimeRange().map { setTimeRange ->
            val startDate = setTimeRange.minStart.toFestivalDate()
            val endDate = setTimeRange.maxStart.toFestivalDate()

            val days = generateSequence(seed = startDate) { it.plus(1, DateTimeUnit.DAY) }
                .takeWhile { it <= endDate }
                .map { date ->
                    val dayStartMillis = date
                        .atTime(hour = FESTIVAL_DAY_CUTOFF_HOURS.inWholeHours.toInt(), minute = 0)
                        .toInstant(timeZone = DateTimeUtils.FESTIVAL_TIME_ZONE)
                        .toEpochMilliseconds()

                    SetTimeDay(
                        dayStartMillis = dayStartMillis,
                        dayEndMillis = dayStartMillis + 24.hours.inWholeMilliseconds,
                        dateOfMonth = date.day,
                        dayOfWeek = date.dayOfWeek.isoDayNumber,
                    )
                }.toList()

            SetTimeDays(days = days)
        }
    }
}
