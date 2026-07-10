package com.ilyne.helloszigetkmp.core.db.model

data class SetTimeWithArtistStageSummary(
    val id: String,
    val startTime: Long,
    val endTime: Long,

    val artistId: String,
    val artistName: String,

    val stageId: String?,
    val stageName: String?
)
