package com.ilyne.helloszigetkmp.core.config

import com.russhwolf.settings.Settings

private const val KEY_BASE_URL_OVERRIDE = "DebugConfigStore.baseUrlOverride"
private const val KEY_TOKEN_OVERRIDE = "DebugConfigStore.tokenOverride"
private const val KEY_SKIP_GOOGLE_SIGN_IN_OVERRIDE = "DebugConfigStore.skipGoogleSignInOverride"

/**
 * Runtime-settable counterpart to the compile-time `sziget.localBackendUrl`/
 * `sziget.localBearerToken`/`sziget.skipGoogleSignIn` Gradle properties (see [AppConfig] and
 * [SKIP_GOOGLE_SIGN_IN]) - lets a debug build point at a different backend or bypass Google
 * sign-in without a rebuild, via the login screen's debug bottom sheet.
 *
 * Persisted in the plain (non-secure) [Settings] instance, same as other dev-facing prefs, so it
 * survives process death but is wiped by an app uninstall. Every getter/setter here is
 * meaningless unless the caller has already checked [AppConfiguring.isDebug] - that gate lives in
 * callers ([DebugOverridableAppConfig], [com.ilyne.helloszigetkmp.core.auth.SzigetAuthService]),
 * mirroring the double-checked pattern [SKIP_GOOGLE_SIGN_IN] already uses so a stray persisted
 * value can never steer a release build.
 */
class DebugConfigStore(
    private val settings: Settings,
) {
    fun getBaseUrlOverride(): String? = settings.getStringOrNull(KEY_BASE_URL_OVERRIDE)?.takeIf { it.isNotBlank() }

    fun setBaseUrlOverride(value: String?) {
        if (value.isNullOrBlank()) settings.remove(KEY_BASE_URL_OVERRIDE) else settings.putString(KEY_BASE_URL_OVERRIDE, value)
    }

    fun getTokenOverride(): String? = settings.getStringOrNull(KEY_TOKEN_OVERRIDE)?.takeIf { it.isNotBlank() }

    fun setTokenOverride(value: String?) {
        if (value.isNullOrBlank()) settings.remove(KEY_TOKEN_OVERRIDE) else settings.putString(KEY_TOKEN_OVERRIDE, value)
    }

    // Defaults to the compile-time flag so an untouched debug build behaves exactly as it did
    // before this store existed, until someone actually opens the debug sheet and changes it.
    fun getSkipGoogleSignIn(): Boolean = settings.getBoolean(KEY_SKIP_GOOGLE_SIGN_IN_OVERRIDE, SKIP_GOOGLE_SIGN_IN)

    fun setSkipGoogleSignIn(value: Boolean) {
        settings.putBoolean(KEY_SKIP_GOOGLE_SIGN_IN_OVERRIDE, value)
    }
}
