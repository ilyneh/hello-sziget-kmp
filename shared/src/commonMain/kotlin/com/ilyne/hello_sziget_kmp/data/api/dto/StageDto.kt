package com.ilyne.hello_sziget_kmp.data.api.dto

import com.ilyne.hello_sziget_kmp.domain.model.Stage
import kotlinx.serialization.Serializable

@Serializable
data class StageDto(
    val id: String,
    val name: String,
    val color: Long = 0xFF6C63FF,
)

fun StageDto.toDomain() = Stage(
    id = id,
    name = name,
    color = color,
)
