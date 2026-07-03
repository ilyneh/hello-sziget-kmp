package com.ilyne.hello_sziget_kmp.util.datetime

import com.ilyne.hello_sziget_kmp.FESTIVAL_TIME_ZONE_ID
import com.ilyne.hello_sziget_kmp.util.datetime.DateTimeUtils.FESTIVAL_TIME_ZONE
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateTimeUtils {
    val FESTIVAL_TIME_ZONE = TimeZone.of(zoneId = FESTIVAL_TIME_ZONE_ID)
}

fun Long.toLocalDateTime(timeZone: TimeZone = FESTIVAL_TIME_ZONE) =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(timeZone = FESTIVAL_TIME_ZONE)