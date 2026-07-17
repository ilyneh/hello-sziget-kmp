package com.ilyne.helloszigetkmp.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class GoogleAuthProvider actual constructor() :
    KoinComponent,
    GoogleAuthProviding {
        private val context: Context by inject()
        private val credentialManager by lazy { CredentialManager.create(context) }

        actual override suspend fun signIn(): AuthUser {
            val googleIdOption = GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GoogleAuthConfig.WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            return try {
                val result = credentialManager.getCredential(request = request, context = context)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                AuthUser(
                    idToken = credential.idToken,
                    email = credential.id,
                    displayName = credential.displayName ?: "",
                    photoUrl = credential.profilePictureUri?.toString(),
                )
            } catch (e: Exception) {
                throw AuthException("Google Sign-In failed: ${e.message}", e)
            }
        }

        actual override fun signOut() {
            // CredentialManager sign-out is handled at the Google account level
        }

        actual override fun getCurrentUser(): AuthUser? = null
    }

class AuthException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
