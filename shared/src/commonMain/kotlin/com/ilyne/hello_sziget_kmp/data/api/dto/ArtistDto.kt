package com.ilyne.hello_sziget_kmp.data.api.dto

import com.ilyne.hello_sziget_kmp.domain.model.Artist
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val genre: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
)

fun ArtistDto.toDomain() = Artist(
    id = id,
    name = name,
    genre = genre,
    imageUrl = imageUrl,
    bio = bio,
    isFavorited = isFavorited,
)
