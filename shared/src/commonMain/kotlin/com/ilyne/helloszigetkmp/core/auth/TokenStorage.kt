package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.ilyne.helloszigetkmp.util.Logger
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val TAG = "TokenStorage"
private const val KEY = "TokenStorage"

class TokenStorage(private val settings: Settings) {

    fun save(token: TokenDto) {
        Logger.d(TAG, "saving token: ${Json.encodeToString(token)}")
        settings.putString(KEY, Json.encodeToString(token))
    }

    fun read(): TokenDto? {
        Logger.d(TAG, "reading token: ${settings.getStringOrNull(KEY)}")
        return settings.getStringOrNull(KEY)?.let { Json.decodeFromString(it) }
    }

    fun clear() {
        settings.remove(KEY)
    }
}
