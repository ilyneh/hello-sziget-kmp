package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CurrentUserProviderTest {
    private val user = User(id = "user-1", name = "Ilyne", imageUrl = "https://example.com/avatar.png")

    @Test
    fun currentUser_beforeSet_throws() {
        val provider = CurrentUserProvider()

        assertFailsWith<IllegalStateException> { provider.currentUser }
    }

    @Test
    fun currentUser_afterSet_returnsSetUser() {
        val provider = CurrentUserProvider()

        provider.set(user)

        assertEquals(user, provider.currentUser)
    }

    @Test
    fun currentUser_afterClear_throwsAgain() {
        val provider = CurrentUserProvider()
        provider.set(user)

        provider.clear()

        assertFailsWith<IllegalStateException> { provider.currentUser }
    }
}
