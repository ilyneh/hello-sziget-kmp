package com.ilyne.helloszigetkmp.network

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
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
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
    refreshToken: String
): HttpClient =
    baseHttpClient.config {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken, refreshToken)
                }
                refreshTokens {
                    val refreshTokenInfo: TokenInfo = client.submitForm(
                        url = "$baseUrl/auth/refresh",
                        formParameters = parameters {
                            append("refresh_token", oldTokens?.refreshToken ?: "")
                        }
                    ) {
                        markAsRefreshTokenRequest()
                    }.body()
                    BearerTokens(refreshTokenInfo.accessToken, refreshTokenInfo.refreshToken)
                }
            }
        }
    }

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
)

