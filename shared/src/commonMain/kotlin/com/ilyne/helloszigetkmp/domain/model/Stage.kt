package com.ilyne.helloszigetkmp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Stage(
    val id: String,
    val name: String,
    val description: String?,
)
