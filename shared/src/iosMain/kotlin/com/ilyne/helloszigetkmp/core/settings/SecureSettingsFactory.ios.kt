package com.ilyne.helloszigetkmp.core.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

private const val KEYCHAIN_SERVICE_NAME = "com.ilyne.helloszigetkmp.secure_settings"

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual fun createSecureSettings(): Settings = KeychainSettings(service = KEYCHAIN_SERVICE_NAME)
