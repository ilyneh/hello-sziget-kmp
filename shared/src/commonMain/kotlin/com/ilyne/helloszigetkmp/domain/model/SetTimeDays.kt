package com.ilyne.helloszigetkmp.domain.model

/**
 * Dates of the month (August) that are considered "extra" festival days.
 * These are hidden from the schedule by default, with an option to re-enable them
 * via the schedule filter's "Show extra days" toggle.
 */
val EXTRA_FESTIVAL_DAYS_OF_MONTH: List<Int> = listOf(9, 10)

data class SetTimeDay(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val dateOfMonth: Int,
    val dayOfWeek: Int,
) {
    val isExtraDay: Boolean
        get() = dateOfMonth in EXTRA_FESTIVAL_DAYS_OF_MONTH
}

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
