package com.ilyne.helloszigetkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.ilyne.helloszigetkmp.di.initKoin
import platform.UIKit.UIViewController

// Named to match the iOS-side factory convention expected by ContentView.swift
// (MainViewControllerKt.MainViewController()), not a Kotlin function name.
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
