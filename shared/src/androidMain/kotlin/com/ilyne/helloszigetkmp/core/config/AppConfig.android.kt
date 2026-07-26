package com.ilyne.helloszigetkmp.core.config

import android.content.Context
import android.content.pm.ApplicationInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class AppConfig actual constructor() : AppConfiguring {
    actual override fun baseUrlLocal(): String {
        // The local-backend override must only ever take effect in a debuggable build - checked
        // *before* looking at the override, not after, so a stray `local.properties` entry on a
        // dev machine can't point a release/beta build at a local/cleartext backend.
        if (!AppConfigContext.isDebuggable) return BASE_URL_PROD

        // Point a debug build at a local backend by setting `sziget.localBackendUrl` in
        // local.properties or via `-Psziget.localBackendUrl=...`, e.g. the Android emulator
        // host alias "http://10.0.2.2:8000/api/v1". See local.properties.example.
        return SzigetBuildConfig.LOCAL_BACKEND_URL.takeIf { it.isNotBlank() } ?: BASE_URL_DEV
    }

    actual override fun isDebug(): Boolean = AppConfigContext.isDebuggable
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
