package com.ilyne.hello_sziget_kmp.presentation.schedule.components

import androidx.compose.ui.graphics.Color
import com.ilyne.hello_sziget_kmp.domain.model.Artist
import com.ilyne.hello_sziget_kmp.domain.model.GenreGroup
import com.ilyne.hello_sziget_kmp.domain.model.genreGroupOf

// Named swatches so multiple genre groups can point at the same color by reusing a name
// instead of repeating a hex value. Add/remove swatches as needed — this list has no
// required size.
private val RED = Color(0xFFE8354A)
private val BLUE = Color(0xFF0EA5E9)
private val PURPLE = Color(0xFF8B3CF7)
private val ORANGE = Color(0xFFF59E0B)
private val GREEN = Color(0xFF10B981)
private val PINK = Color(0xFFEC4899)
private val TEAL = Color(0xFF2BB8B8)
private val YELLOW = Color(0xFFFFDC00)
private val GRAY = Color(0xFFAAAAAA)

// Fill in the swatch for each genre group. Point multiple groups at the same swatch
// to have them render as the same tile color.
private val genreColors: Map<GenreGroup, Color> = mapOf(
    GenreGroup.ROCK to RED,
    GenreGroup.POP to PINK,
    GenreGroup.INDIE to PINK,
    GenreGroup.JAZZ to ORANGE,
    GenreGroup.ELECTRONIC to BLUE,
    GenreGroup.TECHNO to BLUE,
    GenreGroup.HOUSE to BLUE,
    GenreGroup.RAP to PURPLE,
    GenreGroup.HIP_HOP to PURPLE,
    GenreGroup.EXPERIMENTAL to TEAL,
    GenreGroup.FOLK to GREEN,
    GenreGroup.DISCO to YELLOW,
    GenreGroup.TRANCE to BLUE,
    GenreGroup.BASS to BLUE,
    GenreGroup.WORLD to GREEN,
    GenreGroup.DANCE to TEAL,
    GenreGroup.PERFORMANCE to GRAY,
    GenreGroup.COMEDY to GRAY,
    GenreGroup.WORKSHOP to GRAY,
    GenreGroup.VISUAL_ART to GRAY,
)

private val DEFAULT_COLOR = GRAY

fun genreColor(genreGroup: GenreGroup): Color =
    genreColors[genreGroup] ?: DEFAULT_COLOR

// An artist can carry several tags across different genre groups — resolve them all,
// then pick whichever group comes first in GenreGroup's declaration order so the color
// is deterministic regardless of the order the tags happen to appear in.
fun artistColor(artist: Artist): Color {
    val matchedGroups = artist.tags
        ?.mapNotNull { genreGroupOf(it) }
        ?.toSet()
        ?: emptySet()

    val group = GenreGroup.entries.firstOrNull { it in matchedGroups }
    return group?.let(::genreColor) ?: DEFAULT_COLOR
}
