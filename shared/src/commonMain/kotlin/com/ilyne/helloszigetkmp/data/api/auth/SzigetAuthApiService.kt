package com.ilyne.helloszigetkmp.data.api.auth

import com.ilyne.helloszigetkmp.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SzigetAuthApiService(
    private val client: HttpClient,
    private val appConfig: AppConfig,
) {
    private val baseUrl = "${appConfig.baseUrl()}/auth"

    suspend fun googleLogin(googleToken: String): TokenDto {
        return client.post(urlString = "$baseUrl/google/mobile") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(googleToken))
        }.body()
    }

    suspend fun logout(refreshToken: String) {
        return client.post(urlString = "$baseUrl/logout") {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequestDto(refreshToken))
        }.body()
    }
}

@Serializable
data class LoginRequestDto(@SerialName("id_token") val googleToken: String)

@Serializable
data class LogoutRequestDto(@SerialName("refresh_token") val refreshToken: String)
