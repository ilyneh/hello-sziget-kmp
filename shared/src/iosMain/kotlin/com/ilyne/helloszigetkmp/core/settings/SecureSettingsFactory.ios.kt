package com.ilyne.helloszigetkmp.core.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.CFBridgingRetain
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService

private const val KEYCHAIN_SERVICE_NAME = "com.ilyne.helloszigetkmp.secure_settings"

// The `service: String` convenience constructor only sets kSecAttrService, leaving the OS default
// accessibility of kSecAttrAccessibleWhenUnlocked - which IS included in encrypted iTunes/iCloud
// backups, so a restore onto a different device would carry the stored auth tokens with it. Using
// the vararg constructor to also set kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly keeps these
// entries device-local. (The retained service-name CFStringRef below is intentionally never
// released - this factory is only ever called once, for a Koin singleton with app-lifetime scope.)
@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class, ExperimentalForeignApi::class)
actual fun createSecureSettings(): Settings =
    KeychainSettings(
        kSecAttrService to CFBridgingRetain(KEYCHAIN_SERVICE_NAME),
        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
    )
