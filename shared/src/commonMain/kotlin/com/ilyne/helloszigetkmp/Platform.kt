package com.ilyne.helloszigetkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
