package com.ilyne.helloszigetkmp.domain.model

data class ArtistFriendsFavorited(
    val artist: Artist,
    val friendsFavorited: List<User>,
)
