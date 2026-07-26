package com.ilyne.helloszigetkmp.presentation.util

import androidx.compose.ui.Modifier

/**
 * Exposes Compose testTags as platform-native accessibility ids so UI test frameworks like
 * Maestro (which drive the app through the platform accessibility/view tree) can target Compose
 * nodes the same way they'd target a native view id.
 *
 * Android-only: `androidx.compose.ui.semantics.testTagsAsResourceId` bridges testTag to
 * Android's AccessibilityNodeInfo resource-id, a concept that doesn't exist on iOS - Compose
 * Multiplatform's iOS target already exposes `Modifier.testTag(...)` as the view's
 * `accessibilityIdentifier` without needing an explicit opt-in, so the iOS actual is a no-op.
 */
expect fun Modifier.testTagsAsResourceId(): Modifier
