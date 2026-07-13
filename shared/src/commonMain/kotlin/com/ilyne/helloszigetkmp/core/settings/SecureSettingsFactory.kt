package com.ilyne.helloszigetkmp.core.settings

import com.russhwolf.settings.Settings

/**
 * Creates a [Settings] instance backed by platform-native encrypted storage:
 * - Android: `EncryptedSharedPreferences` (AndroidX Security Crypto)
 * - iOS: Keychain
 *
 * This should be used for any [Settings] that may hold sensitive data (e.g. auth tokens),
 * as opposed to the unencrypted default `Settings()` factory from multiplatform-settings.
 */
expect fun createSecureSettings(): Settings
