package com.ilyne.helloszigetkmp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bio: String?,
    val isFavorited: Boolean,
    val tags: List<String>?,
)

@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
)

@Entity(tableName = "set_times")
data class SetTimeEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val stageId: String?,
    val startTime: Long,
    val endTime: Long,
    val hideEndTime: Boolean,
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val picture: String?
)

@Entity(
    tableName = "users_friends",
    primaryKeys = ["userId", "friendId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("userId"),
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
        Index(value = ["userId"]),
        Index(value = ["friendId"]),
    ]
)
data class UserFriendEntity(
    val userId: String,
    val friendId: String,
    val status: Status
) {
    enum class Status {
        ACCEPTED, REQUESTED, SENT
    }
}

@Entity(
    tableName = "current_user",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("userId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class CurrentUserEntity(
    @PrimaryKey val id: Int = 0,
    val userId: String
)
