package com.ilyne.helloszigetkmp.util.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format

fun formatTime(epochMillis: Long): String = epochMillis.toLocalDateTime().formatTime()

fun LocalDateTime.formatTime(): String {
    val timeFormat = LocalDateTime.Format {
        hour()
        chars(":")
        minute()
    }
    return format(timeFormat)
}
