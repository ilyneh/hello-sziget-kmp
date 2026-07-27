package com.ilyne.helloszigetkmp.core.config

/**
 * Wraps the platform [AppConfig] so [DebugConfigStore]'s runtime base-URL override transparently
 * applies to every existing consumer of [AppConfiguring.baseUrlLocal] (e.g.
 * [com.ilyne.helloszigetkmp.core.auth.SzigetAuthService]'s `loadAuthenticatedModules`) with no
 * per-call-site changes.
 *
 * [delegate]'s own debuggable-build check is still the source of truth for [isDebug] and is
 * checked *before* consulting the override, same rationale as [delegate]'s own local-backend-url
 * override: a stray persisted value must never be able to point a release build anywhere but
 * [delegate]'s own release URL.
 */
class DebugOverridableAppConfig(
    private val delegate: AppConfiguring,
    private val debugConfigStore: DebugConfigStore,
) : AppConfiguring {
    override fun isDebug(): Boolean = delegate.isDebug()

    override fun baseUrlLocal(): String {
        if (!delegate.isDebug()) return delegate.baseUrlLocal()
        return debugConfigStore.getBaseUrlOverride() ?: delegate.baseUrlLocal()
    }
}
