package com.ilyne.helloszigetkmp.core.config

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual class AppConfig actual constructor() {
    @OptIn(ExperimentalNativeApi::class)
    actual fun baseUrlLocal(): String {
        // Platform.isDebugBinary reflects whether the shared framework was built in debug or
        // release mode. Xcode's `embedAndSignAppleFrameworkForXcode` build phase (see
        // iosApp.xcodeproj) forwards the active scheme's build configuration (Debug/Release) to
        // the Gradle task, which picks NativeBuildType.DEBUG or RELEASE accordingly.
        return if (Platform.isDebugBinary) BASE_URL_DEV else BASE_URL_PROD
    }
}
