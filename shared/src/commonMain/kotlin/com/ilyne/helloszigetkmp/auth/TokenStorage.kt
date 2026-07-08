package com.ilyne.helloszigetkmp.auth

import com.ilyne.helloszigetkmp.data.api.auth.TokenDto
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "TokenStorage"

class TokenStorage(private val settings: Settings) {

    fun save(token: TokenDto) {
        settings.putString(KEY, Json.encodeToString(token))
    }

    fun read(): TokenDto? {
        return settings.getStringOrNull(KEY)?.let { Json.decodeFromString(it) }
    }

    fun clear() {
        settings.remove(KEY)
    }
}
