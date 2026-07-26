package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStorageTest {
    @Test
    fun read_beforeAnySave_returnsNull() {
        val storage = TokenStorage(MapSettings())

        assertNull(storage.read())
    }

    @Test
    fun save_thenRead_roundTripsToken() {
        val storage = TokenStorage(MapSettings())
        val token = TokenDto(accessToken = "access-1", refreshToken = "refresh-1", tokenType = "bearer")

        storage.save(token)

        assertEquals(token, storage.read())
    }

    @Test
    fun save_overwritesPreviouslySavedToken() {
        val storage = TokenStorage(MapSettings())
        storage.save(TokenDto(accessToken = "access-1", refreshToken = "refresh-1", tokenType = "bearer"))

        val newToken = TokenDto(accessToken = "access-2", refreshToken = "refresh-2", tokenType = "bearer")
        storage.save(newToken)

        assertEquals(newToken, storage.read())
    }

    @Test
    fun clear_removesSavedToken() {
        val storage = TokenStorage(MapSettings())
        storage.save(TokenDto(accessToken = "access-1", refreshToken = "refresh-1", tokenType = "bearer"))

        storage.clear()

        assertNull(storage.read())
    }

    @Test
    fun clear_beforeAnySave_isNoOp() {
        val storage = TokenStorage(MapSettings())

        storage.clear()

        assertNull(storage.read())
    }

    private companion object {
        const val TOKEN_STORAGE_KEY = "TokenStorage"
    }

    @Test
    fun read_corruptStoredJson_returnsNullAndClearsTheEntryInsteadOfThrowing() {
        // A stored value that fails to decode (e.g. the persisted TokenDto shape changed
        // incompatibly across an app update) must not crash restoreSession()'s launch-time read -
        // App.kt's LaunchedEffect has no catch around it.
        val settings: Settings = MapSettings()
        settings.putString(TOKEN_STORAGE_KEY, "{not valid json")
        val storage = TokenStorage(settings)

        assertNull(storage.read())
        // The corrupt entry is dropped so it doesn't keep failing to decode on every future read.
        assertNull(settings.getStringOrNull(TOKEN_STORAGE_KEY))
    }
}
