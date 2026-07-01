package com.ilyne.hello_sziget_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.ilyne.hello_sziget_kmp.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}