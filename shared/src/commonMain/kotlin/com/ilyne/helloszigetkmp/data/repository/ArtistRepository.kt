package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.api.dto.ArtistDto
import com.ilyne.helloszigetkmp.data.db.ArtistDao
import com.ilyne.helloszigetkmp.data.db.ArtistEntity
import com.ilyne.helloszigetkmp.domain.model.Artist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArtistRepository(
    private val api: SzigetApiService,
    private val dao: ArtistDao,
) {
    fun observeArtists(): Flow<List<Artist>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeArtist(id: String): Flow<Artist?> = dao.observeById(id).map { it?.toDomain() }

    fun observeFavorites(): Flow<List<Artist>> = dao.observeFavorites().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh() {
        val dtos = api.getArtists()
        dao.upsertAll(dtos.map { it.toEntity() })
    }

    suspend fun toggleFavorite(
        artistId: String,
        isFavorited: Boolean,
    ) {
        dao.setFavorited(artistId, isFavorited)
        try {
            if (isFavorited) api.favoriteArtist(artistId) else api.unfavoriteArtist(artistId)
        } catch (e: Exception) {
            dao.setFavorited(artistId, !isFavorited)
            throw e
        }
    }
}

private fun ArtistDto.toEntity() =
    ArtistEntity(
        id = id,
        name = name,
        bio = bio,
        isFavorited = isFavorited,
        tags = tags,
    )

private fun ArtistEntity.toDomain() =
    Artist(
        id = id,
        name = name,
        bio = bio,
        isFavorited = isFavorited,
        tags = tags,
    )
