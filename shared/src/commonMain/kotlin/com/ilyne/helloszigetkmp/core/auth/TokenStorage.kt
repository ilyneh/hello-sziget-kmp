package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "TokenStorage"

class TokenStorage(
    private val settings: Settings,
) {
    fun save(token: TokenDto) {
        settings.putString(KEY, Json.encodeToString(token))
    }

    // A stored value that fails to decode (e.g. the persisted shape changed incompatibly across
    // an app update) must not crash restoreSession()'s launch-time read - treat it as "no
    // session" and drop the corrupt entry so it doesn't loop on every future read.
    fun read(): TokenDto? =
        settings.getStringOrNull(KEY)?.let {
            runCatching { Json.decodeFromString<TokenDto>(it) }.getOrElse {
                clear()
                null
            }
        }

    fun clear() {
        settings.remove(KEY)
    }
}
