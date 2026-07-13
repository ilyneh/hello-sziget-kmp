package com.ilyne.helloszigetkmp.core.config

const val BASE_URL_DEV = "https://hello-sziget-127130301586.us-east1.run.app/api/v1"

// TODO: Set the real production Cloud Run URL before shipping a release build.
// No production backend URL was discoverable in the repo/docs at the time this
// dev/release split was wired up (only the dev Cloud Run URL above exists).
// This placeholder MUST be replaced before a release build is distributed.
const val BASE_URL_PROD = "https://hello-sziget-127130301586.us-east1.run.app/api/v1"

const val FESTIVAL_TIME_ZONE_ID = "Europe/Budapest"

// Overridable at build time via the `sziget.localBearerToken` property (see :shared's
// build.gradle.kts), e.g. when the backend issues a fresh test token. Falls back to the
// checked-in default when no override is configured.
val BEARER_TOKEN_LOCALHOST: String = SzigetBuildConfig.LOCAL_BEARER_TOKEN

// Overridable at build time via the `sziget.skipGoogleSignIn` property. Even if left/set to
// true by accident, SzigetAuthService.localSignIn() ignores it outside debug builds, so this
// can never bypass Google sign-in in a release build.
val SKIP_GOOGLE_SIGN_IN: Boolean = SzigetBuildConfig.SKIP_GOOGLE_SIGN_IN

expect class AppConfig() {
    fun baseUrlLocal(): String

    fun isDebug(): Boolean
}
