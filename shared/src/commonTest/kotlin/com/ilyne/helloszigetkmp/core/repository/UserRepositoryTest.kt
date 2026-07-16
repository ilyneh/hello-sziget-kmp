package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.UserDto
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.domain.model.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [UserRepository.refresh] fetches all users from the API and upserts them into the DAO.
 *
 * [UserRepository.syncCurrentUser] upserts a single [UserDto], marks it as the current user, and
 * returns the domain-mapped result.
 *
 * [UserRepository.uploadProfilePicture] uploads the image bytes then delegates to
 * [UserRepository.syncCurrentUser] with the API's response DTO.
 */
class UserRepositoryTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun refresh_success_upsertsMappedUsersIntoDao() =
        runTest {
            val dao = FakeUserDao()
            val users = listOf(
                UserDto(id = "u1", name = "Alice", imageUrl = "https://example.test/alice.png"),
                UserDto(id = "u2", name = "Bob", imageUrl = null),
            )
            val fixture = repository(dao, endpoints = FakeEndpoints(users = users))

            fixture.repository.refresh(fixture.api)

            assertEquals(
                listOf(
                    UserEntity(id = "u1", name = "Alice", imageUrl = "https://example.test/alice.png"),
                    UserEntity(id = "u2", name = "Bob", imageUrl = null),
                ),
                dao.users,
            )
        }

    @Test
    fun refresh_apiFailure_propagatesAndDoesNotUpsert() =
        runTest {
            val dao = FakeUserDao()
            val fixture = repository(dao, endpoints = FakeEndpoints(users = emptyList(), usersShouldFail = true))

            assertFailsWith<Exception> { fixture.repository.refresh(fixture.api) }

            assertTrue(dao.users.isEmpty())
        }

    @Test
    fun syncCurrentUser_success_upsertsSetsCurrentUserAndReturnsDomainMapping() =
        runTest {
            val dao = FakeUserDao()
            val fixture = repository(dao, endpoints = FakeEndpoints(users = emptyList()))
            val dto = UserDto(id = "u1", name = "Alice", imageUrl = "https://example.test/alice.png")

            val result = fixture.repository.syncCurrentUser(dto)

            assertEquals(listOf(UserEntity(id = "u1", name = "Alice", imageUrl = "https://example.test/alice.png")), dao.users)
            assertEquals("u1", dao.currentUserId)
            assertEquals(
                User(id = "u1", name = "Alice", imageUrl = "https://example.test/alice.png"),
                result,
            )
        }

    @Test
    fun uploadProfilePicture_success_delegatesToSyncCurrentUserWithUploadedDto() =
        runTest {
            val dao = FakeUserDao()
            val uploadedDto = UserDto(id = "u1", name = "Alice", imageUrl = "https://example.test/new.png")
            val fixture = repository(
                dao,
                endpoints = FakeEndpoints(users = emptyList(), uploadResponse = uploadedDto),
            )

            val result =
                fixture.repository.uploadProfilePicture(fixture.api, bytes = byteArrayOf(1, 2, 3), contentType = "image/png")

            assertEquals(listOf(UserEntity(id = "u1", name = "Alice", imageUrl = "https://example.test/new.png")), dao.users)
            assertEquals("u1", dao.currentUserId)
            assertEquals(
                User(id = "u1", name = "Alice", imageUrl = "https://example.test/new.png"),
                result,
            )
        }

    @Test
    fun uploadProfilePicture_apiFailure_propagatesAndDoesNotSync() =
        runTest {
            val dao = FakeUserDao()
            val fixture = repository(dao, endpoints = FakeEndpoints(users = emptyList(), uploadShouldFail = true))

            assertFailsWith<Exception> {
                fixture.repository.uploadProfilePicture(fixture.api, bytes = byteArrayOf(1, 2, 3), contentType = "image/png")
            }

            assertTrue(dao.users.isEmpty())
            assertEquals(null, dao.currentUserId)
        }

    private fun repository(
        dao: UserDao,
        endpoints: FakeEndpoints,
    ): TestFixture {
        val json = Json { ignoreUnknownKeys = true }
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/users/me/image") && endpoints.uploadShouldFail -> {
                    respondError(HttpStatusCode.InternalServerError)
                }

                path.endsWith("/users/me/image") -> {
                    respond(json.encodeToString(endpoints.uploadResponse), headers = jsonHeaders)
                }

                path.endsWith("/users") && endpoints.usersShouldFail -> {
                    respondError(HttpStatusCode.InternalServerError)
                }

                path.endsWith("/users") -> {
                    respond(json.encodeToString(endpoints.users), headers = jsonHeaders)
                }

                else -> {
                    respondError(HttpStatusCode.NotFound)
                }
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }
        val api = SzigetApiService(client = client, baseUrl = "https://unused.test")
        return TestFixture(repository = UserRepository(dao = dao), api = api)
    }

    /** Pairs a [UserRepository] with the [SzigetApiService] built for it, for test convenience. */
    private class TestFixture(
        val repository: UserRepository,
        val api: SzigetApiService,
    )

    private class FakeEndpoints(
        val users: List<UserDto>,
        val usersShouldFail: Boolean = false,
        val uploadResponse: UserDto = UserDto(id = "unused", name = "unused", imageUrl = null),
        val uploadShouldFail: Boolean = false,
    )

    private class FakeUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        var currentUserId: String? = null

        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>) {
            users.forEach { new ->
                this.users.removeAll { it.id == new.id }
                this.users.add(new)
            }
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {
            currentUserId = currentUser.userId
        }

        override suspend fun getCurrentUser(): UserEntity? = throw NotImplementedError("unused in this test")

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {
            currentUserId = null
        }

        override suspend fun deleteAll() {
            users.clear()
        }
    }
}
