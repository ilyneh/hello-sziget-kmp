package com.ilyne.helloszigetkmp.presentation.schedule.model

import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.Stage

class ScheduleUiModel {
    class SetTime(
        val id: String,
        val artistId: String,
        val stageId: String?,
        val startTime: Long, // epoch millis
        val endTime: Long, // epoch millis
        val hideEndTime: Boolean,
        val artist: Artist? = null,
        val stage: Stage? = null,
        val isInThePast: Boolean,
        val startHourFraction: Double,
        val endHourFraction: Double,
    )
}
