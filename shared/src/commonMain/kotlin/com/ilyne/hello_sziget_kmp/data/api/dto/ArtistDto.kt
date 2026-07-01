package com.ilyne.hello_sziget_kmp.data.api.dto

import com.ilyne.hello_sziget_kmp.domain.model.Artist
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val bio: String? = null,
    @SerialName("favorite_count") val favoriteCount: Int,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
)

fun ArtistDto.toDomain() = Artist(
    id = id,
    name = name,
    bio = bio,
    isFavorited = isFavorited
)
