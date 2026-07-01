package com.ilyne.hello_sziget_kmp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SetTime(
    val id: String,
    val artistId: String,
    val stageId: String,
    val startTime: Long,  // epoch millis
    val endTime: Long,    // epoch millis
    val artist: Artist? = null,
    val stage: Stage? = null,
)
