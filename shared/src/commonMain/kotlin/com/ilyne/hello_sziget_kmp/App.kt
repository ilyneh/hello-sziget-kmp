package com.ilyne.hello_sziget_kmp

import androidx.compose.runtime.Composable
import com.ilyne.hello_sziget_kmp.navigation.AppNavGraph
import com.ilyne.hello_sziget_kmp.presentation.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavGraph()
    }
}
