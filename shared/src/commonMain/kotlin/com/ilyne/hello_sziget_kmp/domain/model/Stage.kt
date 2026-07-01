package com.ilyne.hello_sziget_kmp.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Stage(
    val id: String,
    val name: String,
    val color: Long,
)
