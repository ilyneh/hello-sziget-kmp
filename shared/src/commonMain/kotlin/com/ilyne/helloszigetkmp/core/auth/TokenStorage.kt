package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.ilyne.helloszigetkmp.core.settings.SettingsStore
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "TokenStorage"

class TokenStorage(
    settings: Settings,
) {
    // A stored value that fails to decode (e.g. the persisted shape changed incompatibly across
    // an app update) must not crash restoreSession()'s launch-time read - treat it as "no
    // session" and drop the corrupt entry so it doesn't loop on every future read.
    private val store = SettingsStore<TokenDto>(
        settings = settings,
        key = KEY,
        encode = { Json.encodeToString(it) },
        decode = { Json.decodeFromString(it) },
        onDecodeFailure = { clear() },
    )

    fun save(token: TokenDto) = store.save(token)

    fun read(): TokenDto? = store.read()

    fun clear() {
        store.clear()
    }
}
