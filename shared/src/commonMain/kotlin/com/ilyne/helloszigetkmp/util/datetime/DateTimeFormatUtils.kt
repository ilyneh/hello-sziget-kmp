package com.ilyne.helloszigetkmp.util.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding


fun formatTime(epochMillis: Long): String = epochMillis.toLocalDateTime().formatTime()


fun LocalDateTime.formatTime(): String {
    val timeFormat = LocalDateTime.Format {
        hour()
        chars(":")
        minute()
    }
    return format(timeFormat)
}

fun LocalDate.formatDate(): String {
    val dateFormat = LocalDate.Format {
        day(Padding.SPACE)
        chars(" ")
        monthName(names = MonthNames.ENGLISH_FULL)
        chars(" - ")
        dayOfWeek(names = DayOfWeekNames.ENGLISH_FULL)
    }
    return format(dateFormat)
}
