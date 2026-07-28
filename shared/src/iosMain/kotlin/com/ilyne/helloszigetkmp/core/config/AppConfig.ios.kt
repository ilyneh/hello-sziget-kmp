package com.ilyne.helloszigetkmp.core.config

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual class AppConfig actual constructor() : AppConfiguring {
    @OptIn(ExperimentalNativeApi::class)
    actual override fun baseUrlLocal(): String {
        return BASE_URL_PROD
    }

    @OptIn(ExperimentalNativeApi::class)
    actual override fun isDebug(): Boolean = Platform.isDebugBinary
}
