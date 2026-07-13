package com.ilyne.helloszigetkmp.core.config

import android.content.Context
import android.content.pm.ApplicationInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class AppConfig actual constructor() {
    actual fun baseUrlLocal(): String {
        // Uncomment to point a debug build at a local backend (Android emulator host alias):
        // if (AppConfigContext.isDebuggable) return "http://10.0.2.2:8000/api/v1"
        return if (AppConfigContext.isDebuggable) BASE_URL_DEV else BASE_URL_PROD
    }
}

/**
 * The shared module's Android Gradle target (com.android.kotlin.multiplatform.library) does not
 * generate a per-build-type BuildConfig, so we can't read BuildConfig.DEBUG here directly.
 * Instead we mirror the pattern used by DatabaseBuilderFactory.android.kt: lazily inject the
 * Application Context (provided by androidApp's Koin module) and check the manifest's
 * ApplicationInfo.FLAG_DEBUGGABLE flag, which AGP sets based on the debug/release build type.
 */
private object AppConfigContext : KoinComponent {
    private val context: Context by inject()

    val isDebuggable: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
