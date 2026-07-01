package com.ilyne.hello_sziget_kmp

import android.app.Application
import com.ilyne.hello_sziget_kmp.di.initKoinAndroid

class SzigetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(applicationContext)
    }
}
