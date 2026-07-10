package com.ilyne.helloszigetkmp.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(
    tableName = "artist_friend_favorites",
    primaryKeys = ["artistId", "friendId"],
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("artistId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("friendId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["artistId"]),
        Index(value = ["friendId"])
    ]
)
data class ArtistFriendFavoritedEntity(
    val artistId: String,
    val friendId: String
)
