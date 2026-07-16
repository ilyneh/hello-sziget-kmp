package com.ilyne.helloszigetkmp.presentation.component.pulltorefresh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Thin wrapper around [PullToRefreshBox] for the repeated
 * `PullToRefreshBox(isRefreshing = ..., onRefresh = ..., modifier = modifier.fillMaxSize()) { content() }`
 * shape used by refreshable screens. Callers remain responsible for computing [isRefreshing] from
 * their own status/UI state, since that varies per screen.
 */
@Composable
fun PullToRefreshContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        content = { content() },
    )
}
