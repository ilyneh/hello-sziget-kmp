package com.ilyne.helloszigetkmp.core.auth

import kotlinx.coroutines.suspendCancellableCoroutine

// Implemented by calling into Swift via a provided callback factory.
// The Swift layer (ContentView.swift) sets these before launching the KMP root.
//
// A plain callback (rather than `suspend () -> AuthUser`) is used here because
// Kotlin/Native's Objective-C header only exposes stored suspend-function
// properties as the `KotlinSuspendFunction0` protocol, which Swift can't
// satisfy with a plain closure. A completion-handler shape bridges to Swift
// as a normal block instead.
var googleSignInHandler: ((onResult: (AuthUser?, String?) -> Unit) -> Unit)? = null
var googleSignOutHandler: (() -> Unit)? = null
var googleCurrentUserHandler: (() -> AuthUser?)? = null

actual class GoogleAuthProvider actual constructor() : GoogleAuthProviding {
    actual override suspend fun signIn(): AuthUser {
        val handler = googleSignInHandler
            ?: throw AuthException("Google Sign-In handler not configured on iOS")
        return suspendCancellableCoroutine { continuation ->
            handler { user, errorMessage ->
                if (user != null) {
                    continuation.resume(user, onCancellation = null)
                } else {
                    continuation.resumeWith(Result.failure(AuthException(errorMessage ?: "Google Sign-In failed")))
                }
            }
        }
    }

    actual override fun signOut() {
        googleSignOutHandler?.invoke()
    }

    actual override fun getCurrentUser(): AuthUser? = googleCurrentUserHandler?.invoke()
}

class AuthException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
