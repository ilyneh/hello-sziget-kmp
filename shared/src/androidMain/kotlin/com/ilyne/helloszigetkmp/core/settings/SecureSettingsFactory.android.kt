package com.ilyne.helloszigetkmp.core.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val ENCRYPTED_PREFS_FILE_NAME = "secure_settings"

actual fun createSecureSettings(): Settings {
    val appContext = AndroidSecureSettingsContext.context

    val masterKey =
        MasterKey
            .Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    val encryptedPrefs =
        EncryptedSharedPreferences.create(
            appContext,
            ENCRYPTED_PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    return SharedPreferencesSettings(encryptedPrefs)
}

object AndroidSecureSettingsContext : KoinComponent {
    val context: Context by inject()
}
