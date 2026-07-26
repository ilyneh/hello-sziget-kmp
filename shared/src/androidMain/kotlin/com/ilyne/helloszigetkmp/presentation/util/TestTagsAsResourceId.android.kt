package com.ilyne.helloszigetkmp.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId as platformTestTagsAsResourceId

actual fun Modifier.testTagsAsResourceId(): Modifier = this.semantics { platformTestTagsAsResourceId = true }
