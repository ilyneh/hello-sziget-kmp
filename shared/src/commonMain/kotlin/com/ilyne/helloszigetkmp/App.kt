package com.ilyne.helloszigetkmp

import androidx.compose.runtime.Composable
import com.ilyne.helloszigetkmp.navigation.AppNavGraph
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavGraph()
    }
}
