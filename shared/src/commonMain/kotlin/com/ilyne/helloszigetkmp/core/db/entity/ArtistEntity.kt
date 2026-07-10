package com.ilyne.helloszigetkmp.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val isFavorited: Boolean,
    val tags: List<String>?,
)

