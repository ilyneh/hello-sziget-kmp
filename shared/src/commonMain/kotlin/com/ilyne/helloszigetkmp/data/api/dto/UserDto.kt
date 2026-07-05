package com.ilyne.helloszigetkmp.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val picture: String?
)
