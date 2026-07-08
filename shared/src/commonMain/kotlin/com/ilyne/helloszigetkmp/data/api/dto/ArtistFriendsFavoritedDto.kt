package com.ilyne.helloszigetkmp.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ArtistFriendsFavoritedDto(
    val id: String,
    @SerialName("friends_favorited") val friendsFavorited: List<String>
)
