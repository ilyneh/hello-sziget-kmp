package com.ilyne.helloszigetkmp.core.image

import coil3.SingletonImageLoader

actual fun initAppImageLoader() {
    SingletonImageLoader.setSafe { createAppImageLoader(it) }
}
