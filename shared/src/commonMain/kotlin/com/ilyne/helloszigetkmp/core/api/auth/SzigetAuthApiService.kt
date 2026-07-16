package com.ilyne.helloszigetkmp.core.api.auth

import com.ilyne.helloszigetkmp.core.config.AppConfiguring
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SzigetAuthApiService(
    private val client: HttpClient,
    private val appConfig: AppConfiguring,
) {
    private val baseUrl = "${appConfig.baseUrlLocal()}/auth"

    suspend fun googleLogin(googleToken: String): TokenDto =
        client
            .post(urlString = "$baseUrl/google/mobile") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(googleToken))
            }.body()

    suspend fun logout(
        accessToken: String,
        refreshToken: String,
    ): Unit =
        client
            .post(urlString = "$baseUrl/logout") {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(LogoutRequestDto(refreshToken))
            }.body()
}

@Serializable
data class LoginRequestDto(
    @SerialName("id_token") val googleToken: String,
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)
