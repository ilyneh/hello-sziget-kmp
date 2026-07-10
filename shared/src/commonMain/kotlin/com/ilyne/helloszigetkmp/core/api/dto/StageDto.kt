package com.ilyne.helloszigetkmp.core.api.dto

import com.ilyne.helloszigetkmp.domain.model.Stage
import kotlinx.serialization.Serializable

@Serializable
data class StageDto(
    val id: String,
    val name: String,
    val description: String?,
)

fun StageDto.toDomain() =
    Stage(
        id = id,
        name = name,
        description = description,
    )
