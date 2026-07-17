package com.ilyne.helloszigetkmp.core.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity

data class ArtistFriendsFavoritedSummary(
    @Embedded
    val artist: ArtistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ArtistFriendFavoritedEntity::class,
            parentColumn = "artistId",
            entityColumn = "friendId",
        ),
        entity = UserEntity::class,
    )
    val friends: List<UserEntity>,
)
