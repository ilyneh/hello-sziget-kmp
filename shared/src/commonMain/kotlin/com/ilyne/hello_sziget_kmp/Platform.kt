package com.ilyne.hello_sziget_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform