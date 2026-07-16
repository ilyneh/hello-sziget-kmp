package com.ilyne.helloszigetkmp.core.sync

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.UserDto
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * [UsersSyncService.fetchAllUsers] is fire-and-forget: it launches on the service's own
 * internal `Dispatchers.Default` scope, not on the caller's coroutine. That means `runTest`'s
 * virtual-time scheduler never sees that work, so tests can't rely on `advanceUntilIdle()` to
 * observe completion - instead they coordinate via [CompletableDeferred] gates in the fake
 * [SzigetApiService] responses (mirroring the approach in `SzigetAuthServiceTest`) and await
 * terminal state through a real dispatcher with [withTimeout].
 */
class UsersSyncServiceTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun fetchAllUsers_success_transitionsIdleToInProgressToSuccess() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val service = UsersSyncService(UserRepository(dao = FakeUserDao()))
            assertIs<UsersSyncService.SyncStatus.Idle>(service.status.value)

            service.fetchAllUsers(apiService(users = listOf(sampleUser), gate = gate))

            // fetchAllUsers() sets InProgress synchronously, before the launched fetch has a
            // chance to run - observable immediately even though the fetch itself is still
            // blocked on the gate.
            assertIs<UsersSyncService.SyncStatus.InProgress>(service.status.value)

            gate.complete(Unit)
            val succeeded =
                withContext(Dispatchers.Default) { withTimeout(2.seconds) { service.awaitSuccessfulSync() } }

            assertTrue(succeeded)
            assertIs<UsersSyncService.SyncStatus.Success>(service.status.value)
        }

    @Test
    fun fetchAllUsers_failure_transitionsToFailureWithError() =
        runTest {
            val service = UsersSyncService(UserRepository(dao = FakeUserDao()))

            service.fetchAllUsers(apiService(shouldFail = true))

            val succeeded =
                withContext(Dispatchers.Default) { withTimeout(2.seconds) { service.awaitSuccessfulSync() } }

            assertFalse(succeeded)
            val failure = assertIs<UsersSyncService.SyncStatus.Failure>(service.status.value)
            assertTrue(failure.error is Exception)
        }

    @Test
    fun fetchAllUsers_calledAgainWhileInFlight_isDroppedNotQueued() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            var callCount = 0
            val dao = FakeUserDao()
            val service = UsersSyncService(UserRepository(dao = dao))
            val api = apiService(users = listOf(sampleUser), gate = gate, onCalled = { callCount++ })

            service.fetchAllUsers(api)
            assertIs<UsersSyncService.SyncStatus.InProgress>(service.status.value)

            // Second call arrives while the first fetch is still blocked on the gate - tryLock()
            // fails, so this must be a no-op rather than queuing a second fetch.
            service.fetchAllUsers(api)

            gate.complete(Unit)
            withContext(Dispatchers.Default) { withTimeout(2.seconds) { service.awaitSuccessfulSync() } }

            assertEquals(1, callCount, "an overlapping call must be dropped, not queued")
            assertEquals(1, dao.upsertCallCount)
        }

    @Test
    fun awaitSuccessfulSync_suspendsUntilTerminalState_thenReturnsResult() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val service = UsersSyncService(UserRepository(dao = FakeUserDao()))
            service.fetchAllUsers(apiService(users = listOf(sampleUser), gate = gate))

            val awaiting = CompletableDeferred<Boolean>()
            val awaiter =
                launch(Dispatchers.Default) {
                    awaiting.complete(service.awaitSuccessfulSync())
                }

            // Give the awaiter a chance to actually start suspending before we assert it hasn't
            // completed - it must still be waiting since the fetch is blocked on the gate.
            withContext(Dispatchers.Default) { yield() }
            assertFalse(awaiting.isCompleted, "awaitSuccessfulSync must still be suspended while fetch is in flight")

            gate.complete(Unit)
            val result = withContext(Dispatchers.Default) { withTimeout(2.seconds) { awaiting.await() } }

            assertTrue(result)
            awaiter.cancel()
        }

    private val sampleUser = UserDto(id = "u1", name = "User One", imageUrl = null)

    private fun apiService(
        users: List<UserDto> = emptyList(),
        shouldFail: Boolean = false,
        gate: CompletableDeferred<Unit>? = null,
        onCalled: (() -> Unit)? = null,
    ): SzigetApiService {
        val engine = MockEngine {
            onCalled?.invoke()
            gate?.await()
            if (shouldFail) {
                respondError(HttpStatusCode.InternalServerError)
            } else {
                respond(json.encodeToString(users), headers = jsonHeaders)
            }
        }
        val client =
            HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) { json(json) }
            }
        return SzigetApiService(client = client, baseUrl = "https://unused.test")
    }

    private class FakeUserDao(
        private val currentUser: UserEntity? = null,
    ) : UserDao {
        var upsertCallCount = 0
            private set

        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>) {
            upsertCallCount++
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {
            throw NotImplementedError("unused in this test")
        }

        override suspend fun getCurrentUser(): UserEntity? = currentUser

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {
            throw NotImplementedError("unused in this test")
        }

        override suspend fun deleteAll() {
            throw NotImplementedError("unused in this test")
        }
    }
}
