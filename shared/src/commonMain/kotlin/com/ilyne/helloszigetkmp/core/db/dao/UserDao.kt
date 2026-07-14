package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCurrentUser(currentUser: CurrentUserEntity)

    suspend fun setCurrentUser(userId: String) = setCurrentUser(CurrentUserEntity(userId = userId))

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN current_user ON users.id = current_user.userId
        LIMIT 1
        """
    )
    suspend fun getCurrentUser(): UserEntity?

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN current_user ON users.id = current_user.userId
        LIMIT 1
        """
    )
    fun observeCurrentUser(): Flow<UserEntity?>
}
