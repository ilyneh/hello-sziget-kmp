package com.ilyne.hello_sziget_kmp.domain.model

data class SetTimeDay(
    val startDayMillis: Long,
    val endDayMillis: Long,
    val dateOfMonth: Int,
    val dayOfWeek: Int
)

data class SetTimeDays(
    val days: List<SetTimeDay>
)
