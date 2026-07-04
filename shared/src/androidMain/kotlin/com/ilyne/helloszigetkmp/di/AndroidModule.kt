package com.ilyne.helloszigetkmp.di

import android.content.Context
import org.koin.dsl.module

fun initKoinAndroid(context: Context) {
    initKoin(platformModules = listOf(module { single<Context> { context } }))
}
