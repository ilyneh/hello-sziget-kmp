package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.ArtistDto
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ArtistRepository(
    private val api: SzigetApiService,
    private val dao: ArtistDao,
    settings: Settings,
) {
    private val softRefreshGate = SoftRefreshGate(settings, key = "ArtistRepository")

    // Serializes toggleFavorite per artistId so a rapid double-tap on the same artist resolves
    // in submission order instead of interleaving the two calls' optimistic-write/revert cycles
    // (which could otherwise leave the local cache reflecting neither tap's actual outcome).
    // Toggling different artists is unaffected - each artistId gets its own lock. toggleLocksGate
    // only guards the brief get-or-create step on toggleLocks, not the toggle itself.
    private val toggleLocksGate = Mutex()
    private val toggleLocks = mutableMapOf<String, Mutex>()

    fun observeArtists(): Flow<List<Artist>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeArtist(id: String): Flow<Artist?> = dao.observeById(id).map { it?.toDomain() }

    fun observeFavorites(): Flow<List<Artist>> = dao.observeFavorites().map { entities -> entities.map { it.toDomain() } }

    fun searchArtists(query: String): Flow<List<Artist>> = dao.searchByName(query).map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh(force: Boolean = false) {
        softRefreshGate.refreshIfStale(force) {
            val dtos = api.getArtists()
            dao.upsertAll(dtos.map { it.toEntity() })
        }
    }

    suspend fun toggleFavorite(
        artistId: String,
        isFavorited: Boolean,
    ) {
        lockFor(artistId).withLock {
            dao.setFavorited(artistId, isFavorited)
            try {
                if (isFavorited) api.favoriteArtist(artistId) else api.unfavoriteArtist(artistId)
            } catch (e: Exception) {
                dao.setFavorited(artistId, !isFavorited)
                throw e
            }
        }
    }

    private suspend fun lockFor(artistId: String): Mutex =
        toggleLocksGate.withLock { toggleLocks.getOrPut(artistId) { Mutex() } }
}

private fun ArtistDto.toEntity() =
    ArtistEntity(
        id = id,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        isFavorited = isFavorited,
        tags = tags,
    )

fun ArtistEntity.toDomain() =
    Artist(
        id = id,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        isFavorited = isFavorited,
        tags = tags,
    )
