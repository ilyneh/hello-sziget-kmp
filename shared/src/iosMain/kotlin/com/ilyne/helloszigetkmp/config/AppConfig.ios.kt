package com.ilyne.helloszigetkmp.config

actual class AppConfig actual constructor() {
    actual fun baseUrl(): String {
        return "http://127.0.0.1:8000/api/v1"
    }
}
