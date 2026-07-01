package com.ilyne.hello_sziget_kmp.di

import android.content.Context
import org.koin.dsl.module

fun initKoinAndroid(context: Context) {
    initKoin(platformModules = listOf(module { single<Context> { context } }))
}
