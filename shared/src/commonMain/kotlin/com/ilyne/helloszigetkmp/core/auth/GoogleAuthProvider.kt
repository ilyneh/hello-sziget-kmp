package com.ilyne.helloszigetkmp.core.auth

data class AuthUser(
    val idToken: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
)

// Extracted so SzigetAuthService can depend on this interface instead of the concrete
// expect/actual GoogleAuthProvider - the Android actual injects an Android Context via Koin
// at construction time, which makes it unconstructible from commonTest. Fakes for tests
// implement this interface directly instead.
interface GoogleAuthProviding {
    suspend fun signIn(): AuthUser

    fun signOut()

    fun getCurrentUser(): AuthUser?
}

expect class GoogleAuthProvider() : GoogleAuthProviding {
    override suspend fun signIn(): AuthUser

    override fun signOut()

    override fun getCurrentUser(): AuthUser?
}
