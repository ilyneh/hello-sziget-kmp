package com.ilyne.hello_sziget_kmp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val id: String,
    val name: String,
    val genre: String,
    val imageUrl: String?,
    val bio: String?,
    val isFavorited: Boolean,
)
