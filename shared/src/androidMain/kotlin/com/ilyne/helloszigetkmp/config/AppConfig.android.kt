package com.ilyne.helloszigetkmp.config

actual class AppConfig actual constructor() {
    actual fun baseUrl(): String {
        return "http://10.0.2.2:8000/api/v1"
    }
}
