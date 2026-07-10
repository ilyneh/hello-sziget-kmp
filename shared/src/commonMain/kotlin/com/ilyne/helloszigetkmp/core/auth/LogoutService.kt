package com.ilyne.helloszigetkmp.core.auth

class LogoutService(private val session: SzigetAuthService) {

    suspend fun logout() = session.logout()
}
