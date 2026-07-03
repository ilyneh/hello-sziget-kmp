package com.ilyne.hello_sziget_kmp.domain.model

data class SetTimeDay(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val dateOfMonth: Int,
    val dayOfWeek: Int
)

data class SetTimeDays(
    val days: List<SetTimeDay>
)
