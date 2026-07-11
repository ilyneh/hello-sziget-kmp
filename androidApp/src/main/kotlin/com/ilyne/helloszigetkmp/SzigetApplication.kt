package com.ilyne.helloszigetkmp

import android.app.Application
import com.ilyne.helloszigetkmp.core.image.initAppImageLoader
import com.ilyne.helloszigetkmp.di.initKoinAndroid

class SzigetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(applicationContext)
        initAppImageLoader()
    }
}
