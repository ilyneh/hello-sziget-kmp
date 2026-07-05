package com.ilyne.helloszigetkmp.data.api

import com.ilyne.helloszigetkmp.data.api.dto.ArtistDto
import com.ilyne.helloszigetkmp.data.api.dto.SetTimeDto
import com.ilyne.helloszigetkmp.data.api.dto.StageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post

class SzigetApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getArtists(): List<ArtistDto> = client.get("$baseUrl/artists").body()

    suspend fun getArtist(id: String): ArtistDto = client.get("$baseUrl/artists/$id").body()

    suspend fun getStages(): List<StageDto> = client.get("$baseUrl/stages").body()

    suspend fun getStage(id: String): StageDto = client.get("$baseUrl/stages/$id").body()

    suspend fun getSetTimes(): List<SetTimeDto> = client.get("$baseUrl/set_times").body()

    suspend fun favoriteArtist(
        artistId: String,
    ) = client.post(urlString = "$baseUrl/artists/$artistId/favorite")

    suspend fun unfavoriteArtist(
        artistId: String,
    ) = client.delete(urlString = "$baseUrl/artists/$artistId/favorite")
}
