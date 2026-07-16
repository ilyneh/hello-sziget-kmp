package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.core.api.auth.TokenDto
import com.russhwolf.settings.MapSettings
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
}
