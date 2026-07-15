package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color

import androidx.compose.ui.graphics.Color
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.genreGroupOf
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette

// Named swatches so multiple genre groups can point at the same color by reusing a name
// instead of repeating a hex value. Add/remove swatches as needed — this list has no
// required size.
private val BLUE = SzigetPalette.PrimaryBlue
private val PURPLE = SzigetPalette.Magenta
private val GREEN = SzigetPalette.TealGreen
private val WARM_ORANGE = SzigetPalette.WarmOrange
private val TEAL = SzigetPalette.DarkTeal
private val YELLOW = SzigetPalette.SunshineYellow

// Fill in the swatch for each genre group. Point multiple groups at the same swatch
// to have them render as the same tile color.
private val genreColors: Map<GenreGroup, Color> = mapOf(
    GenreGroup.ROCK to WARM_ORANGE,
    GenreGroup.POP to WARM_ORANGE,
    GenreGroup.INDIE to WARM_ORANGE,
    GenreGroup.JAZZ to PURPLE,
    GenreGroup.ELECTRONIC to BLUE,
    GenreGroup.TECHNO to BLUE,
    GenreGroup.HOUSE to BLUE,
    GenreGroup.RAP to PURPLE,
    GenreGroup.HIP_HOP to PURPLE,
    GenreGroup.EXPERIMENTAL to TEAL,
    GenreGroup.FOLK to GREEN,
    GenreGroup.DISCO to BLUE,
    GenreGroup.TRANCE to BLUE,
    GenreGroup.BASS to BLUE,
    GenreGroup.WORLD to GREEN,
    GenreGroup.DANCE to TEAL,
    GenreGroup.PERFORMANCE to YELLOW,
    GenreGroup.COMEDY to YELLOW,
    GenreGroup.WORKSHOP to YELLOW,
    GenreGroup.VISUAL_ART to YELLOW,
    GenreGroup.UNKNOWN to YELLOW,
)

private val DEFAULT_COLOR = YELLOW

fun genreColor(genreGroup: GenreGroup): Color = genreColors[genreGroup] ?: DEFAULT_COLOR

// An artist can carry several tags across different genre groups — resolve them all,
// then pick whichever group comes first in GenreGroup's declaration order so the color
// is deterministic regardless of the order the tags happen to appear in.
fun artistColor(artist: Artist): Color {
    val matchedGroups = artist.tags?.mapNotNull { genreGroupOf(it) }?.toSet() ?: emptySet()

    val group = GenreGroup.entries.firstOrNull { it in matchedGroups }
    return group?.let(::genreColor) ?: DEFAULT_COLOR
}
