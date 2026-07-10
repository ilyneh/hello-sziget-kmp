package com.ilyne.helloszigetkmp.core.network

import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val baseHttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.INFO
        }
    }

fun createApiHttpClient(
    baseUrl: String,
    accessToken: String,
    refreshToken: String,
    tokenStorage: TokenStorage,
    onSessionInvalidated: () -> Unit,
): HttpClient =
    baseHttpClient.config {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken, refreshToken)
                }
                refreshTokens {
                    val refreshTokenInfo: TokenDto =  try {
                        val refreshToken = oldTokens?.refreshToken
                            ?: throw IllegalStateException("Attempting to refresh token without a refresh token.")

                        client.post(urlString = "$baseUrl/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            markAsRefreshTokenRequest()
                            val refreshTokenRequestData = RefreshTokenRequest(refreshToken = refreshToken)
                            setBody(Json.encodeToString(value = refreshTokenRequestData))
                        }.body()
                    } catch (e: Exception) {
                        onSessionInvalidated()
                        return@refreshTokens null
                    }
                    tokenStorage.save(token = refreshTokenInfo)
                    BearerTokens(refreshTokenInfo.accessToken, refreshTokenInfo.refreshToken)
                }
            }
        }
    }

@Serializable
private data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)
