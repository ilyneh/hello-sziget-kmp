package com.ilyne.helloszigetkmp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ilyne.helloszigetkmp.data.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    // Plain INSERT-OR-REPLACE would delete-then-reinsert conflicting rows, cascading
    // onDelete = CASCADE on current_user's FK and wiping the signed-in user pointer.
    // Insert-or-ignore + update instead, so existing rows are updated in place.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(users: List<UserEntity>): List<Long>

    @Update
    suspend fun updateAll(users: List<UserEntity>)

    @Transaction
    suspend fun upsertAll(users: List<UserEntity>) {
        val insertResults = insertIgnoring(users)
        val existing = users.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (existing.isNotEmpty()) updateAll(existing)
    }


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
