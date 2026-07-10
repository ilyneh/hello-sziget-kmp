package com.ilyne.helloszigetkmp.core.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val picture: String?,
)
