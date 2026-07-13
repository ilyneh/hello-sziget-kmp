package com.ilyne.helloszigetkmp.core.config

const val BASE_URL_DEV = "https://hello-sziget-127130301586.us-east1.run.app/api/v1"

// TODO: Set the real production Cloud Run URL before shipping a release build.
// No production backend URL was discoverable in the repo/docs at the time this
// dev/release split was wired up (only the dev Cloud Run URL above exists).
// This placeholder MUST be replaced before a release build is distributed.
const val BASE_URL_PROD = "https://hello-sziget-127130301586.us-east1.run.app/api/v1"

const val BEARER_TOKEN_LOCALHOST = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZXhwIjoxNzgzNzkzNjI0fQ.dWI3JEGFl9f6phvIkp3nZHdMTxN-Ds1dMQagpLpFL8A"
const val FESTIVAL_TIME_ZONE_ID = "Europe/Budapest"

const val SKIP_GOOGLE_SIGN_IN = false

expect class AppConfig() {
    fun baseUrlLocal(): String

    fun isDebug(): Boolean
}
