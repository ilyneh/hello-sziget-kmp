package com.ilyne.helloszigetkmp.core.config

actual class AppConfig actual constructor() {
    actual fun baseUrl(): String {
        return "http://192.168.1.193:8000/api/v1"
    }
}
