package com.ilyne.helloszigetkmp.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
