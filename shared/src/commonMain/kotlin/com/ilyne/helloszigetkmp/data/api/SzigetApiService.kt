package com.ilyne.helloszigetkmp.data.api

import com.ilyne.helloszigetkmp.data.api.dto.ArtistDto
import com.ilyne.helloszigetkmp.data.api.dto.ArtistFriendFavoritedDto
import com.ilyne.helloszigetkmp.data.api.dto.SetTimeDto
import com.ilyne.helloszigetkmp.data.api.dto.StageDto
import com.ilyne.helloszigetkmp.data.api.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post

class SzigetApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getMe(): UserDto = client.get("$baseUrl/auth/me").body()

    suspend fun getUsers(): List<UserDto> = client.get("$baseUrl/users").body()

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

    suspend fun getFriends(): List<UserDto> = client.get("$baseUrl/friends").body()

    suspend fun getFriendRequests(): List<UserDto> = client.get("$baseUrl/friends/requests").body()

    suspend fun getSentFriendRequests(): List<UserDto> = client.get("$baseUrl/friends/sent").body()

    suspend fun sendFriendRequest(userId: String) = client.post(urlString = "$baseUrl/friends/request/$userId")

    suspend fun acceptFriendRequest(userId: String) = client.post(urlString = "$baseUrl/friends/accept/$userId")

    suspend fun removeFriend(userId: String) = client.delete(urlString = "$baseUrl/friends/$userId")

    suspend fun getArtistsFriendFavorited(): List<ArtistFriendFavoritedDto> =
        client.get(urlString = "$baseUrl/friends/favorites").body()
}
