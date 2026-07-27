package com.ilyne.helloszigetkmp.core.config

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugOverridableAppConfigTest {
    @Test
    fun baseUrlLocal_debugBuild_noOverrideSet_fallsBackToDelegate() {
        val debugConfigStore = DebugConfigStore(MapSettings())
        val appConfig = DebugOverridableAppConfig(
            delegate = FakeAppConfig(isDebug = true, baseUrl = "https://delegate.test"),
            debugConfigStore = debugConfigStore,
        )

        assertEquals("https://delegate.test", appConfig.baseUrlLocal())
    }

    @Test
    fun baseUrlLocal_debugBuild_overrideSet_usesOverrideInsteadOfDelegate() {
        val debugConfigStore = DebugConfigStore(MapSettings())
        debugConfigStore.setBaseUrlOverride("http://10.0.2.2:8081/api/v1")
        val appConfig = DebugOverridableAppConfig(
            delegate = FakeAppConfig(isDebug = true, baseUrl = "https://delegate.test"),
            debugConfigStore = debugConfigStore,
        )

        assertEquals("http://10.0.2.2:8081/api/v1", appConfig.baseUrlLocal())
    }

    @Test
    fun baseUrlLocal_releaseBuild_ignoresOverride_fallsBackToDelegate() {
        // The override must never be able to steer a release build even if one was somehow
        // persisted (e.g. a debug build's data surviving into a release install on the same
        // device) - delegate.isDebug() is checked before the override is ever consulted.
        val debugConfigStore = DebugConfigStore(MapSettings())
        debugConfigStore.setBaseUrlOverride("http://10.0.2.2:8081/api/v1")
        val appConfig = DebugOverridableAppConfig(
            delegate = FakeAppConfig(isDebug = false, baseUrl = "https://delegate.test"),
            debugConfigStore = debugConfigStore,
        )

        assertEquals("https://delegate.test", appConfig.baseUrlLocal())
    }

    @Test
    fun isDebug_delegatesToWrappedAppConfig() {
        val debugConfigStore = DebugConfigStore(MapSettings())
        val appConfig = DebugOverridableAppConfig(
            delegate = FakeAppConfig(isDebug = true),
            debugConfigStore = debugConfigStore,
        )

        assertEquals(true, appConfig.isDebug())
    }

    private class FakeAppConfig(
        private val isDebug: Boolean,
        private val baseUrl: String = "https://unused.test",
    ) : AppConfiguring {
        override fun baseUrlLocal(): String = baseUrl

        override fun isDebug(): Boolean = isDebug
    }
}
