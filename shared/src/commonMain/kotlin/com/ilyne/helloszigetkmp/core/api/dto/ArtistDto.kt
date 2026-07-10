package com.ilyne.helloszigetkmp.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val bio: String? = null,
    @SerialName("favorite_count") val favoriteCount: Int,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    val tags: List<String>?,
)
