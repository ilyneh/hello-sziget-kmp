package com.ilyne.helloszigetkmp.util.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

val FESTIVAL_DAY_CUTOFF_HOURS = 6.hours

/**
 * Maps an epoch millis timestamp to its "festival date" — a day spans 6am to 6am local time, so a set at 2am is still considered part
 * of the previous calendar day.
 */
fun Long.toFestivalDate(): LocalDate =
    (Instant.fromEpochMilliseconds(this) - FESTIVAL_DAY_CUTOFF_HOURS)
        .toLocalDateTime(DateTimeUtils.FESTIVAL_TIME_ZONE)
        .date
