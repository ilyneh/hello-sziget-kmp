package com.ilyne.helloszigetkmp.core.config

actual class AppConfig actual constructor() {
    actual fun baseUrlLocal(): String {
        // return "http://10.0.2.2:8000/api/v1"
        return BASE_URL_DEV
    }
}
