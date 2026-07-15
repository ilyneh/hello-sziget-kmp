package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserDaoTest {
    private lateinit var database: SzigetDatabase
    private lateinit var userDao: UserDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<SzigetDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        userDao = database.userDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun user(
        id: String,
        name: String = id,
        imageUrl: String? = null,
    ) = UserEntity(id = id, name = name, imageUrl = imageUrl)

    @Test
    fun `getCurrentUser returns null when no current user has been set`() =
        runTest {
            userDao.upsertAll(listOf(user("user1")))

            assertNull(userDao.getCurrentUser())
        }

    @Test
    fun `observeCurrentUser emits null when no current user has been set`() =
        runTest {
            userDao.upsertAll(listOf(user("user1")))

            assertNull(userDao.observeCurrentUser().first())
        }

    @Test
    fun `setCurrentUser then getCurrentUser returns that user`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice")))

            userDao.setCurrentUser("user1")

            assertEquals(user("user1", "Alice"), userDao.getCurrentUser())
        }

    @Test
    fun `setCurrentUser then observeCurrentUser emits that user`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice")))

            userDao.setCurrentUser("user1")

            assertEquals(user("user1", "Alice"), userDao.observeCurrentUser().first())
        }

    @Test
    fun `switching current user updates getCurrentUser to the latest user`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice"), user("user2", "Bob")))

            userDao.setCurrentUser("user1")
            userDao.setCurrentUser("user2")

            assertEquals(user("user2", "Bob"), userDao.getCurrentUser())
        }

    @Test
    fun `switching current user updates observeCurrentUser to the latest user`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice"), user("user2", "Bob")))

            userDao.setCurrentUser("user1")
            userDao.setCurrentUser("user2")

            assertEquals(user("user2", "Bob"), userDao.observeCurrentUser().first())
        }

    @Test
    fun `clearCurrentUser resets getCurrentUser to null`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice")))
            userDao.setCurrentUser("user1")

            userDao.clearCurrentUser()

            assertNull(userDao.getCurrentUser())
        }

    @Test
    fun `upsertAll inserts new users and observeAll returns them ordered by name`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Zed"), user("user2", "Amy")))

            assertEquals(
                listOf(user("user2", "Amy"), user("user1", "Zed")),
                userDao.observeAll().first(),
            )
        }

    @Test
    fun `upsertAll updates existing users in place`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice")))

            userDao.upsertAll(listOf(user("user1", "Alicia")))

            assertEquals(listOf(user("user1", "Alicia")), userDao.observeAll().first())
        }

    @Test
    fun `observeById emits the matching user`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice"), user("user2", "Bob")))

            assertEquals(user("user1", "Alice"), userDao.observeById("user1").first())
        }

    @Test
    fun `observeById emits null for an unknown id`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice")))

            assertNull(userDao.observeById("unknown").first())
        }

    @Test
    fun `deleteAll removes every user and clears current user via cascade`() =
        runTest {
            userDao.upsertAll(listOf(user("user1", "Alice")))
            userDao.setCurrentUser("user1")

            userDao.deleteAll()

            assertEquals(emptyList(), userDao.observeAll().first())
            assertNull(userDao.getCurrentUser())
        }
}
