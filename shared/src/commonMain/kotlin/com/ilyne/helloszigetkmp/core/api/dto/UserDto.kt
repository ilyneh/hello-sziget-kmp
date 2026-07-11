package com.ilyne.helloszigetkmp.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String?,
)
