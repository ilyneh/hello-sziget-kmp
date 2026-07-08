package com.ilyne.helloszigetkmp.auth

class LogoutService(private val session: SzigetAuthService) {

    suspend fun logout() = session.logout()
}
