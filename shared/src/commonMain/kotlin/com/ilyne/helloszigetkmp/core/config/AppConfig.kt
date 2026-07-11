package com.ilyne.helloszigetkmp.core.config

const val BASE_URL_DEBUG = ""
const val BASE_URL_PROD = ""

const val BEARER_TOKEN_LOCALHOST = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZXhwIjoxNzgzNzg0NzgzfQ.HYxoW6awIkacjkZbDgDMouogN-64ulrL_bnoE-IQ9Xg"
const val FESTIVAL_TIME_ZONE_ID = "Europe/Budapest"

const val SKIP_GOOGLE_SIGN_IN = false

expect class AppConfig() {
    fun baseUrl(): String
}
