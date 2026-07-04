package com.ilyne.hello_sziget_kmp.presentation.schedule.model

import com.ilyne.hello_sziget_kmp.domain.model.Artist
import com.ilyne.hello_sziget_kmp.domain.model.Stage

class ScheduleUiModel {
    class SetTime(
        val id: String,
        val artistId: String,
        val stageId: String?,
        val startTime: Long,  // epoch millis
        val endTime: Long,    // epoch millis
        val hideEndTime: Boolean,
        val artist: Artist? = null,
        val stage: Stage? = null,
        val isInThePast: Boolean,
        val startHourFraction: Double,
        val endHourFraction: Double,
    )
}