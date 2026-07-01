package com.ilyne.hello_sziget_kmp.auth

data class AuthUser(
    val idToken: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
)

expect class GoogleAuthProvider() {
    suspend fun signIn(): AuthUser
    fun signOut()
    fun getCurrentUser(): AuthUser?
}
