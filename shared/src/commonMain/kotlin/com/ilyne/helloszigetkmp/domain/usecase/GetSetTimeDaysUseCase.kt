package com.ilyne.helloszigetkmp.domain.usecase

import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.SetTimeDays
import com.ilyne.helloszigetkmp.util.datetime.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class GetSetTimeDaysUseCase(
    private val scheduleRepository: ScheduleRepository
) {

    private companion object {
        const val TAG = "GetSetTimeDaysUseCase"
        val FESTIVAL_DAY_CUTOFF_HOURS = 6.hours
    }

    suspend fun refresh() = scheduleRepository.refresh()

    fun observeSetTimeDays(): Flow<SetTimeDays> {
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

    /**
     * Maps an epoch millis timestamp to its "festival date" — a day spans 6am to 6am local time, so a set at 2am is still considered part
     * of the previous calendar day.
     */
    private fun Long.toFestivalDate(): LocalDate =
        (Instant.fromEpochMilliseconds(this) - FESTIVAL_DAY_CUTOFF_HOURS).toLocalDateTime(DateTimeUtils.FESTIVAL_TIME_ZONE).date
}
