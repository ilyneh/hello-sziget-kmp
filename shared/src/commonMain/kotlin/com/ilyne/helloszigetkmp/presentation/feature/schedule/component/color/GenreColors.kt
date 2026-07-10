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
private val PINK = SzigetPalette.HotPink
private val TEAL = SzigetPalette.DarkTeal
private val YELLOW = SzigetPalette.SunshineYellow
private val WARM_ORANGE = SzigetPalette.WarmOrange

// Fill in the swatch for each genre group. Point multiple groups at the same swatch
// to have them render as the same tile color.
private val genreColors: Map<GenreGroup, Color> = mapOf(
    GenreGroup.ROCK to PINK,
    GenreGroup.POP to PINK,
    GenreGroup.INDIE to PINK,
    GenreGroup.JAZZ to PURPLE,
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
    GenreGroup.PERFORMANCE to WARM_ORANGE,
    GenreGroup.COMEDY to WARM_ORANGE,
    GenreGroup.WORKSHOP to WARM_ORANGE,
    GenreGroup.VISUAL_ART to WARM_ORANGE,
    GenreGroup.UNKNOWN to WARM_ORANGE,
)

private val DEFAULT_COLOR = WARM_ORANGE

fun genreColor(genreGroup: GenreGroup): Color = genreColors[genreGroup] ?: DEFAULT_COLOR

// An artist can carry several tags across different genre groups — resolve them all,
// then pick whichever group comes first in GenreGroup's declaration order so the color
// is deterministic regardless of the order the tags happen to appear in.
fun artistColor(artist: Artist): Color {
    val matchedGroups = artist.tags?.mapNotNull { genreGroupOf(it) }?.toSet() ?: emptySet()

    val group = GenreGroup.entries.firstOrNull { it in matchedGroups }
    return group?.let(::genreColor) ?: DEFAULT_COLOR
}
