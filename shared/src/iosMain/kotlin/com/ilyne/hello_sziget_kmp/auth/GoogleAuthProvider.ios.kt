package com.ilyne.hello_sziget_kmp.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Implemented by calling into Swift via a provided callback factory.
// The Swift layer (ContentView.swift) sets this before launching the KMP root.
var googleSignInHandler: (suspend () -> AuthUser)? = null
var googleSignOutHandler: (() -> Unit)? = null
var googleCurrentUserHandler: (() -> AuthUser?)? = null

actual class GoogleAuthProvider actual constructor() {
    actual suspend fun signIn(): AuthUser =
        googleSignInHandler?.invoke()
            ?: throw AuthException("Google Sign-In handler not configured on iOS")

    actual fun signOut() {
        googleSignOutHandler?.invoke()
    }

    actual fun getCurrentUser(): AuthUser? =
        googleCurrentUserHandler?.invoke()
}

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
