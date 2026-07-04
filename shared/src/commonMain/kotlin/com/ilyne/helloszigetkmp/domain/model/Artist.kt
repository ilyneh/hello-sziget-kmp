package com.ilyne.helloszigetkmp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val id: String,
    val name: String,
    val bio: String?,
    val isFavorited: Boolean,
    val tags: List<String>?,
)
