package com.ilyne.hello_sziget_kmp.util.datetime

import com.ilyne.hello_sziget_kmp.FESTIVAL_TIME_ZONE_ID
import com.ilyne.hello_sziget_kmp.util.datetime.DateTimeUtils.FESTIVAL_TIME_ZONE
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateTimeUtils {
    val FESTIVAL_TIME_ZONE = TimeZone.of(zoneId = FESTIVAL_TIME_ZONE_ID)
    const val FESTIVAL_DAY_CUTOFF_HOUR = 6
}

fun Long.toLocalDateTime(timeZone: TimeZone = FESTIVAL_TIME_ZONE) =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(timeZone = timeZone)

/**
 * Hours before the festival-day cutoff (e.g. 2am) belong conceptually to the previous
 * day's lineup, so they're pushed past 24 to sort/lay out after that day's evening hours.
 */
fun normalizedFestivalHour(hour: Int): Int =
    if (hour < DateTimeUtils.FESTIVAL_DAY_CUTOFF_HOUR) hour + 24 else hour

fun normalizedFestivalHourFraction(hour: Int, minute: Int): Double {
    val fraction = hour + minute / 60.0
    return if (hour < DateTimeUtils.FESTIVAL_DAY_CUTOFF_HOUR) fraction + 24 else fraction
}

fun Long.normalizedFestivalHour(timeZone: TimeZone = FESTIVAL_TIME_ZONE): Int =
    normalizedFestivalHour(toLocalDateTime(timeZone).hour)

fun Long.normalizedFestivalHourFraction(timeZone: TimeZone = FESTIVAL_TIME_ZONE): Double {
    val dateTime = toLocalDateTime(timeZone)
    return normalizedFestivalHourFraction(dateTime.hour, dateTime.minute)
}