package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [ArtistRepository.toggleFavorite] applies the favorite/unfavorite state optimistically to the local DAO before the
 * network call, then rolls the DAO back to its prior state (and rethrows) if the API call fails.
 */
class ArtistRepositoryTest {
    @Test
    fun toggleFavorite_success_updatesDaoOptimisticallyAndDoesNotRollBack() =
        runTest {
            val dao = FakeArtistDao()
            val repository = repository(dao, success = Unit)

            repository.toggleFavorite(artistId = "artist-1", isFavorited = true)

            assertEquals(
                listOf(FakeArtistDao.SetFavoritedCall("artist-1", true)),
                dao.setFavoritedCalls,
            )
        }

    @Test
    fun toggleFavorite_apiFailure_rollsBackDaoAndRethrows() =
        runTest {
            val dao = FakeArtistDao()
            val failure = RuntimeException("network failure")
            val repository = repository(dao, throwing = failure)

            val thrown =
                assertFailsWith<RuntimeException> {
                    repository.toggleFavorite(artistId = "artist-1", isFavorited = true)
                }
            // kotlinx.coroutines' stack-trace recovery copies exceptions that cross a suspend
            // boundary, preserving the original as `.cause` — so `thrown` isn't reference-equal
            // to `failure` even though it's the same underlying failure. Compare by message
            // (and confirm the original survives as the cause) rather than by instance identity.
            assertEquals(failure.message, thrown.message)
            assertTrue(thrown === failure || thrown.cause === failure)

            // Optimistic update happened first, then rollback to the opposite value.
            assertEquals(
                listOf(
                    FakeArtistDao.SetFavoritedCall("artist-1", true),
                    FakeArtistDao.SetFavoritedCall("artist-1", false),
                ),
                dao.setFavoritedCalls,
            )
        }

    @Test
    fun toggleFavorite_unfavorite_apiFailure_rollsBackToFavorited() =
        runTest {
            val dao = FakeArtistDao()
            val repository = repository(dao, throwing = RuntimeException("boom"))

            assertFailsWith<RuntimeException> {
                repository.toggleFavorite(artistId = "artist-2", isFavorited = false)
            }

            assertEquals(
                listOf(
                    FakeArtistDao.SetFavoritedCall("artist-2", false),
                    FakeArtistDao.SetFavoritedCall("artist-2", true),
                ),
                dao.setFavoritedCalls,
            )
            assertEquals(dao.setFavoritedCalls.size, 2)
        }

    private fun repository(
        dao: ArtistDao,
        success: Unit? = null,
        throwing: Throwable? = null,
    ): ArtistRepository {
        val engine =
            MockEngine { _ ->
                if (throwing != null) throw throwing
                respondOk()
            }
        val api = SzigetApiService(
            client = HttpClient(engine),
            baseUrl = "https://unused.test",
        )
        return ArtistRepository(api = api, dao = dao, settings = MapSettings())
    }

    private class FakeArtistDao : ArtistDao {
        data class SetFavoritedCall(val id: String, val isFavorited: Boolean)

        val setFavoritedCalls = mutableListOf<SetFavoritedCall>()

        override fun observeAll(): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<ArtistEntity?> = flowOf(null)

        override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override suspend fun upsertAll(artists: List<ArtistEntity>) {}

        override suspend fun setFavorited(
            id: String,
            isFavorited: Boolean,
        ) {
            setFavoritedCalls.add(SetFavoritedCall(id, isFavorited))
        }

        override fun searchByName(query: String): Flow<List<ArtistEntity>> = flowOf(emptyList())

        override suspend fun deleteAll() {}
    }
}
