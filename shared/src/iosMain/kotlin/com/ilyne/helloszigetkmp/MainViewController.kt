package com.ilyne.helloszigetkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.ilyne.helloszigetkmp.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}