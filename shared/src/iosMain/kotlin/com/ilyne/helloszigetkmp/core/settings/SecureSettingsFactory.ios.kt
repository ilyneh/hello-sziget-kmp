package com.ilyne.helloszigetkmp.core.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Security.SecItemDelete
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword

private const val KEYCHAIN_SERVICE_NAME = "com.ilyne.helloszigetkmp.secure_settings"

// The `service: String` convenience constructor only sets kSecAttrService, leaving the OS default
// accessibility of kSecAttrAccessibleWhenUnlocked - which IS included in encrypted iTunes/iCloud
// backups, so a restore onto a different device would carry the stored auth tokens with it. Using
// the vararg constructor to also set kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly keeps these
// entries device-local. (The retained service-name CFStringRef below is intentionally never
// released - this factory is only ever called once, for a Koin singleton with app-lifetime scope.)
@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class, ExperimentalForeignApi::class)
actual fun createSecureSettings(): Settings {
    purgeLegacyAccessibilityKeychainItem()
    return KeychainSettings(
        kSecAttrService to CFBridgingRetain(KEYCHAIN_SERVICE_NAME),
        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
    )
}

/**
 * One-time cleanup for the item(s) this factory used to write before `kSecAttrAccessible` was
 * added above. `KeychainSettings` folds every `defaultProperties` entry - including
 * `kSecAttrAccessible` - into the *query* it issues for reads/updates, not just item creation. An
 * item written by a build before that attribute existed is stored under the OS default
 * accessibility, so it silently stops matching that query: reads return null (looks like "no
 * session", harmless) but `SecItemUpdate` returns `errSecItemNotFound`, which the library's
 * `checkError()` turns into a thrown `"Keychain error: the specified item could not be found in
 * the keychain"` - surfacing as a sign-in failure on any device/simulator that had already signed
 * in with an older build. Deleting with a query that omits `kSecAttrAccessible` matches the item
 * regardless of its stored accessibility, so this clears the stale entry (a no-op, via
 * `errSecItemNotFound`, once nothing legacy is left) and lets the fresh sign-in's `SecItemAdd`
 * succeed normally instead of hitting `errSecDuplicateItem` against an item its own query can no
 * longer see.
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
private fun purgeLegacyAccessibilityKeychainItem() {
    memScoped {
        val cfService = CFBridgingRetain(KEYCHAIN_SERVICE_NAME)
        val keys = allocArrayOf(kSecClass, kSecAttrService)
        val values = allocArrayOf(kSecClassGenericPassword, cfService)
        val query: CFDictionaryRef? = CFDictionaryCreate(
            kCFAllocatorDefault,
            keys.reinterpret(),
            values.reinterpret(),
            2.convert(),
            null,
            null,
        )
        SecItemDelete(query) // ignored: errSecItemNotFound just means nothing legacy was left.
        CFBridgingRelease(query)
        CFBridgingRelease(cfService)
    }
}
