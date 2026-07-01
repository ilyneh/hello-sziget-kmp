package com.ilyne.hello_sziget_kmp.data.api

import com.ilyne.hello_sziget_kmp.data.api.dto.ArtistDto
import com.ilyne.hello_sziget_kmp.data.api.dto.SetTimeDto
import com.ilyne.hello_sziget_kmp.data.api.dto.StageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch

class SzigetApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getArtists(): List<ArtistDto> =
        client.get("$baseUrl/artists").body()

    suspend fun getArtist(id: String): ArtistDto =
        client.get("$baseUrl/artists/$id").body()

    suspend fun getStages(): List<StageDto> =
        client.get("$baseUrl/stages").body()

    suspend fun getStage(id: String): StageDto =
        client.get("$baseUrl/stages/$id").body()

    suspend fun getSetTimes(): List<SetTimeDto> =
        client.get("$baseUrl/set_times").body()

    suspend fun toggleFavorite(artistId: String, isFavorited: Boolean): ArtistDto =
        client.patch("$baseUrl/artists/$artistId/favorite") {
            // body would carry { "is_favorited": isFavorited } — add serialization body when needed
        }.body()
}
