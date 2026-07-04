package com.ilyne.helloszigetkmp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SetTime(
    val id: String,
    val artistId: String,
    val stageId: String?,
    val startTime: Long,  // epoch millis
    val endTime: Long,    // epoch millis
    val hideEndTime: Boolean,
    val artist: Artist? = null,
    val stage: Stage? = null,
)
