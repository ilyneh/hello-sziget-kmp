package com.ilyne.helloszigetkmp.domain.usecase

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [GetLikedArtistCountUseCase.invoke] counts how many artists in the observed artist list are favorited.
 */
class GetLikedArtistCountUseCaseTest {
    @Test
    fun emptyArtistList_producesZero() =
        runTest {
            val result = useCase(emptyList())().first()

            assertEquals(0, result)
        }

    @Test
    fun noFavoritedArtists_producesZero() =
        runTest {
            val artists = listOf(
                artist(id = "1", isFavorited = false),
                artist(id = "2", isFavorited = false),
                artist(id = "3", isFavorited = false),
            )

            val result = useCase(artists)().first()

            assertEquals(0, result)
        }

    @Test
    fun someFavoritedArtists_producesCorrectCount() =
        runTest {
            val artists = listOf(
                artist(id = "1", isFavorited = true),
                artist(id = "2", isFavorited = false),
                artist(id = "3", isFavorited = true),
                artist(id = "4", isFavorited = false),
            )

            val result = useCase(artists)().first()

            assertEquals(2, result)
        }

    @Test
    fun allFavoritedArtists_producesFullCount() =
        runTest {
            val artists = listOf(
                artist(id = "1", isFavorited = true),
                artist(id = "2", isFavorited = true),
                artist(id = "3", isFavorited = true),
            )

            val result = useCase(artists)().first()

            assertEquals(3, result)
        }

    private fun artist(
        id: String,
        isFavorited: Boolean,
    ) = ArtistEntity(
        id = id,
        name = "Artist $id",
        bio = null,
        imageUrl = null,
        isFavorited = isFavorited,
        tags = null,
    )

    private fun useCase(artists: List<ArtistEntity>): GetLikedArtistCountUseCase = GetLikedArtistCountUseCase(repository(artists))

    private fun repository(artists: List<ArtistEntity>): ArtistRepository {
        val artistDao = object : ArtistDao {
            override fun observeAll(): Flow<List<ArtistEntity>> = flowOf(artists)

            override fun observeById(id: String): Flow<ArtistEntity?> = flowOf(null)

            override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(emptyList())

            override suspend fun upsertAll(artists: List<ArtistEntity>) {}

            override suspend fun setFavorited(
                id: String,
                isFavorited: Boolean,
            ) {}

            override fun searchByName(query: String): Flow<List<ArtistEntity>> = flowOf(emptyList())

            override suspend fun deleteAll() {}
        }
        val api = SzigetApiService(
            client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
            baseUrl = "https://unused.test",
        )
        return ArtistRepository(
            api = api,
            dao = artistDao,
            settings = MapSettings(),
        )
    }
}
