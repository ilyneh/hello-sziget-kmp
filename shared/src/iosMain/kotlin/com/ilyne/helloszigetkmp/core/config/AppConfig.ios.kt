package com.ilyne.helloszigetkmp.core.config

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual class AppConfig actual constructor() : AppConfiguring {
    @OptIn(ExperimentalNativeApi::class)
    actual override fun baseUrlLocal(): String {
        // Platform.isDebugBinary reflects whether the shared framework was built in debug or
        // release mode. Xcode's `embedAndSignAppleFrameworkForXcode` build phase (see
        // iosApp.xcodeproj) forwards the active scheme's build configuration (Debug/Release) to
        // the Gradle task, which picks NativeBuildType.DEBUG or RELEASE accordingly.
        //
        // The local-backend override must only ever take effect in a debug binary - checked
        // *before* looking at the override, not after, so a stray `local.properties` entry on a
        // dev machine can't point a release/beta build at a local/cleartext backend.
        if (!Platform.isDebugBinary) return BASE_URL_PROD

        // Point a debug build at a local backend by setting `sziget.localBackendUrl` in
        // local.properties or via `-Psziget.localBackendUrl=...`, e.g. your machine's LAN IP
        // "http://192.168.1.193:8000/api/v1". See local.properties.example.
        return SzigetBuildConfig.LOCAL_BACKEND_URL.takeIf { it.isNotBlank() } ?: BASE_URL_DEV
    }

    @OptIn(ExperimentalNativeApi::class)
    actual override fun isDebug(): Boolean = Platform.isDebugBinary
}
