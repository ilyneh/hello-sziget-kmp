package com.ilyne.helloszigetkmp.util.datetime

import com.ilyne.helloszigetkmp.core.config.FESTIVAL_TIME_ZONE_ID
import com.ilyne.helloszigetkmp.util.datetime.DateTimeUtils.FESTIVAL_TIME_ZONE
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateTimeUtils {
    val FESTIVAL_TIME_ZONE = TimeZone.of(zoneId = FESTIVAL_TIME_ZONE_ID)
    const val FESTIVAL_DAY_CUTOFF_HOUR = 6
}

fun Long.toLocalDateTime(timeZone: TimeZone = FESTIVAL_TIME_ZONE) = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone = timeZone)

/**
 * Hours before the festival-day cutoff (e.g. 2am) belong conceptually to the previous day's lineup, so they're pushed past 24 to sort/lay
 * out after that day's evening hours.
 */
fun normalizedFestivalHour(hour: Int): Int = if (hour < DateTimeUtils.FESTIVAL_DAY_CUTOFF_HOUR) hour + 24 else hour

fun normalizedFestivalHourFraction(
    hour: Int,
    minute: Int,
): Double {
    val fraction = hour + minute / 60.0
    return if (hour < DateTimeUtils.FESTIVAL_DAY_CUTOFF_HOUR) fraction + 24 else fraction
}

fun Long.normalizedFestivalHour(timeZone: TimeZone = FESTIVAL_TIME_ZONE): Int = normalizedFestivalHour(toLocalDateTime(timeZone).hour)

fun Long.normalizedFestivalHourFraction(timeZone: TimeZone = FESTIVAL_TIME_ZONE): Double {
    val dateTime = toLocalDateTime(timeZone)
    return normalizedFestivalHourFraction(dateTime.hour, dateTime.minute)
}

/**
 * Normalizes a set's start/end into a single monotonic timeline for rendering (grid layout, hour-fraction positions, etc).
 *
 * [normalizedFestivalHour]/[normalizedFestivalHourFraction] decide whether to push an individual hour past 24 by comparing that hour,
 * in isolation, against the cutoff. That's correct for most sets, but breaks when a set starts before the cutoff and ends AT or AFTER
 * it (e.g. 4:30-6:00, or a long early-morning set running to 7:15): the start gets pushed past 24 (since 4 < 6) but the end doesn't
 * (since 6 >= 6), leaving the normalized end BEFORE the normalized start — a negative "duration" once end - start is taken.
 *
 * To keep this correct for every case (fully pre-cutoff, fully post-cutoff, crossing midnight into the cutoff window, and crossing the
 * cutoff itself), each endpoint is still normalized independently, but if the result would make the end come before the start, the end
 * is additionally pushed past 24 — since no set is expected to run for close to a full extra day, "end < start" can only mean the pair
 * fell on opposite sides of the cutoff boundary and needs to be re-aligned onto the same day-number.
 */
fun normalizedFestivalHourRange(
    startEpochMillis: Long,
    endEpochMillis: Long,
    timeZone: TimeZone = FESTIVAL_TIME_ZONE,
): Pair<Int, Int> {
    val start = startEpochMillis.normalizedFestivalHour(timeZone)
    val end = endEpochMillis.normalizedFestivalHour(timeZone)
    return start to if (end < start) end + 24 else end
}

fun normalizedFestivalHourFractionRange(
    startEpochMillis: Long,
    endEpochMillis: Long,
    timeZone: TimeZone = FESTIVAL_TIME_ZONE,
): Pair<Double, Double> {
    val start = startEpochMillis.normalizedFestivalHourFraction(timeZone)
    val end = endEpochMillis.normalizedFestivalHourFraction(timeZone)
    return start to if (end < start) end + 24 else end
}
