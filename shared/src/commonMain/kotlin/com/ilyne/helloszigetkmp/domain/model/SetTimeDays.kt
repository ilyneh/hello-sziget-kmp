package com.ilyne.helloszigetkmp.domain.model

data class SetTimeDay(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val dateOfMonth: Int,
    val dayOfWeek: Int,
)

data class SetTimeDays(
    val days: List<SetTimeDay>,
)

fun SetTimeDay.dayOfWeekLabel(): String =
    when (dayOfWeek) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
