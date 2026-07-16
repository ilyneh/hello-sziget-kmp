package com.ilyne.helloszigetkmp.presentation.component.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Centered [CircularProgressIndicator] in a [Box], for the common "screen is loading" state.
 *
 * Defaults to filling the available space; pass a fixed-size [modifier] (e.g. a specific
 * `height`) for screens where the loading indicator should occupy a bounded area instead of the
 * whole screen.
 */
@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
