package com.ilyne.hello_sziget_kmp.data.api.dto

import com.ilyne.hello_sziget_kmp.domain.model.SetTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetTimeDto(
    val id: String,
    @SerialName("artist_id") val artistId: String,
    @SerialName("stage_id") val stageId: String?,
    @Serializable(with = EpochMillisSerializer::class)
    @SerialName("start_time") val startTime: Long,
    @Serializable(with = EpochMillisSerializer::class)
    @SerialName("end_time") val endTime: Long,
    @SerialName("hide_end_time") val hideEndTime: Boolean = false
)

fun SetTimeDto.toDomain() = SetTime(
    id = id,
    artistId = artistId,
    stageId = stageId,
    startTime = startTime,
    endTime = endTime,
    hideEndTime = hideEndTime
)
