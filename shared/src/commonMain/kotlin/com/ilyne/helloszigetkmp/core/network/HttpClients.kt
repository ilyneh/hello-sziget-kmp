package com.ilyne.helloszigetkmp.core.network

import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.ilyne.helloszigetkmp.util.Logger as AppLogger
import io.ktor.client.plugins.logging.Logger as KtorLogger

private const val HTTP_LOG_TAG = "HttpClient"

/**
 * [isDebug] gates request/response logging entirely: release builds install no logger at all,
 * so headers (including the Authorization bearer token) and bodies are never written to device
 * logs. Debug builds route through our own [AppLogger] (Logcat/NSLog) instead of Ktor's
 * `Logger.SIMPLE`, which prints unconditionally and bypasses any log-level filtering.
 */
fun createBaseHttpClient(isDebug: Boolean): HttpClient =
    HttpClient {
        // A 307 means a call site is hitting the wrong URL (e.g. a trailing slash the
        // FastAPI backend doesn't register, since it runs with redirect_slashes=False).
        // Fail loudly instead of silently following so the bad URL gets fixed at the source.
        followRedirects = false
        // expectSuccess defaults to false in Ktor, so non-2xx responses (redirects included)
        // wouldn't otherwise throw — callers' try/catch blocks would never see the failure.
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            logger = object : KtorLogger {
                override fun log(message: String) {
                    AppLogger.d(HTTP_LOG_TAG, message)
                }
            }
            level = if (isDebug) LogLevel.INFO else LogLevel.NONE
        }
    }

fun createApiHttpClient(
    baseUrl: String,
    accessToken: String,
    refreshToken: String,
    tokenStorage: TokenStorage,
    onSessionInvalidated: () -> Unit,
    isDebug: Boolean,
): HttpClient =
    createBaseHttpClient(isDebug).config {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken, refreshToken)
                }
                refreshTokens {
                    val refreshTokenInfo: TokenDto = try {
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
    @SerialName("refresh_token") val refreshToken: String,
)
