package com.ilyne.helloszigetkmp.domain.model

enum class PerformanceType {
    MUSIC,
    DANCE,
    ARTS_CULTURE,
}

private val performanceTypeMapping = mapOf(
    PerformanceType.MUSIC to listOf(
        GenreGroup.ROCK,
        GenreGroup.POP,
        GenreGroup.INDIE,
        GenreGroup.JAZZ,
        GenreGroup.ELECTRONIC,
        GenreGroup.TECHNO,
        GenreGroup.HOUSE,
        GenreGroup.RAP,
        GenreGroup.HIP_HOP,
        GenreGroup.EXPERIMENTAL,
        GenreGroup.FOLK,
        GenreGroup.DISCO,
        GenreGroup.TRANCE,
        GenreGroup.BASS,
        GenreGroup.WORLD,
    ),
    PerformanceType.DANCE to listOf(GenreGroup.DANCE),
    PerformanceType.ARTS_CULTURE to listOf(
        GenreGroup.PERFORMANCE,
        GenreGroup.COMEDY,
        GenreGroup.WORKSHOP,
        GenreGroup.VISUAL_ART,
        GenreGroup.UNKNOWN,
    ),
)

private val genreToPerformanceType: Map<GenreGroup, PerformanceType> = performanceTypeMapping
    .flatMap { (performanceType, groups) ->
        groups.map { it to performanceType }
    }.toMap()

enum class GenreGroup {
    ROCK,
    POP,
    INDIE,
    JAZZ,
    ELECTRONIC,
    TECHNO,
    HOUSE,
    RAP,
    HIP_HOP,
    EXPERIMENTAL,
    FOLK,
    DISCO,
    TRANCE,
    BASS,
    WORLD,
    DANCE,
    PERFORMANCE,
    COMEDY,
    WORKSHOP,
    VISUAL_ART,

    // Fallback for artists whose tags don't match any known genre group — not driven by any
    // tag itself, so it has no entry in `genreMapping`.
    UNKNOWN,
}

private val genreMapping = mapOf(
    GenreGroup.ROCK to listOf(
        "genre-rock",
        "genre-metal",
        "genre-punk-rock",
        "genre-alternative",
        "genre-alternative-rock",
        "genre-pop-punk",
        "genre-hardcore-punk",
        "genre-metalcore",
        "genre-garagerock",
        "genre-hardcore-rock",
        "genre-post-punk",
        "genre-psyheledic",
        "genre-neo-psychedelic-music",
    ),
    GenreGroup.POP to listOf(
        "genre-pop",
        "genre-pop-rock",
        "genre-alt-pop",
        "genre-electropop",
        "genre-scandi-pop",
        "genre-frenglish-pop",
        "genre-ballads",
        "genre-avant-garde-pop",
    ),
    GenreGroup.INDIE to listOf(
        "genre-indie",
        "genre-indie-rock",
        "genre-indie-pop",
        "genre-dance-pop",
        "genre-dream-pop",
        "genre-shoegaze",
    ),
    GenreGroup.JAZZ to listOf(
        "genre-jazz",
        "genre-funk",
        "genre-alt-jazz",
        "genre-improvisation",
    ),
    GenreGroup.ELECTRONIC to listOf(
        "genre-electronic",
        "genre-dnb",
        "genre-drum-bass",
        "genre-melodic",
        "genre-breakbeat",
        "genre-cold-electro",
        "genre-rave",
        "genre-drum",
        "genre-edm",
        "genre-british-electronica",
        "genre-club",
        "genre-underground",
        "genre-fast-paced",
    ),
    GenreGroup.TECHNO to listOf(
        "genre-techno",
        "genre-hard-techno",
        "genre-industrial-techno",
        "genre-melodictechno",
        "genre-minimal",
        "genre-dark-tehcno",
        "genre-fast-paced-techno",
        "genre-groovy-techno",
        "genre-underground-techno",
        "genre-modern-techno",
        "genre-analog-fuelled-techno",
    ),
    GenreGroup.HOUSE to listOf(
        "genre-tech-house",
        "genre-house",
        "genre-afro-house",
        "genre-electro-house",
        "genre-deep-house",
        "genre-melodichouse",
        "genre-groovy-house",
        "genre-progressive-house",
        "genre-bass-house",
        "genre-groovy",
        "genre-stutter-house",
        "genre-garage",
        "genre-modern-house",
        "genre-indie-house",
        "genre-future-house",
    ),
    GenreGroup.RAP to listOf("genre-rap"),
    GenreGroup.HIP_HOP to listOf("genre-hip-hop", "genre-trap"),
    GenreGroup.EXPERIMENTAL to listOf("genre-experimental", "genre-experimental-styles"),
    GenreGroup.FOLK to listOf("genre-folk"),
    GenreGroup.DISCO to listOf(
        "genre-disco",
        "genre-post-disco",
        "genre-playing-disco",
        "genre-boogie",
    ),
    GenreGroup.TRANCE to listOf(
        "genre-trance",
        "genre-progressive-trance",
        "genre-psy-trance",
        "genre-funky-trance",
    ),
    GenreGroup.BASS to listOf(
        "genre-dubstep",
        "genre-bass-music",
        "genre-uk-club",
        "genre-jungle",
        "genre-grime",
        "genre-ghettotech",
    ),
    GenreGroup.WORLD to listOf(
        "genre-reggie",
        "genre-flamenco-rumba",
        "genre-flamenco",
        "genre-wagogo",
    ),
    // Non-music movement/performance acts (Sziget runs circus, dance and cabaret programming
    // alongside music) — grouped separately so they don't get colored as a music genre.
    GenreGroup.DANCE to listOf(
        "genre-contemporary-dance",
        "genre-ballroom",
        "genre-participatory-dance-jam",
        "genre-street-and-club-dance",
        "genre-brazilian-couple-dance",
        "genre-electronic-contemporary-dance",
        "genre-queer-immersive-outdoor-movement",
        "genre-voguing-performance",
        "genre-acrobatics-and-dance",
    ),
    GenreGroup.PERFORMANCE to listOf(
        "genre-drag",
        "genre-magic",
        "genre-ventriloquism",
        "genre-sideshow",
        "genre-a-roving-musical-performance",
        "genre-one-man-band",
        "genre-contemporary-circus",
        "genre-contemporary-skating",
        "genre-street-theater",
        "genre-acrobatics-and-big-wheel",
        "genre-interdisciplinary-performance",
        "genre-beatbox",
        "genre-cabaret",
        "genre-solo-post-opera-performance",
        "genre-contemporary-cabaret",
        "genre-live-karaoke-party",
        "genre-clowning",
        "genre-chaotic-variety-show",
        "genre-circus-cabaret",
        "genre-fire-juggling-show",
        "genre-kinetic-contemporary-circus",
        "genre-open-air-show",
    ),
    GenreGroup.COMEDY to listOf(
        "genre-comedy-artistic",
        "genre-alt-comedy",
        "genre-comedy",
        "genre-stand-up-comedy",
    ),
    GenreGroup.WORKSHOP to listOf(
        "genre-workshop",
        "genre-activity",
        "genre-discussion",
        "genre-board-games",
        "genre-live-podcast",
        "genre-lgbtq",
    ),
    GenreGroup.VISUAL_ART to listOf(
        "genre-installation",
        "genre-visual",
        "genre-creative-and-fine-arts-activities",
        "genre-fine-arts",
    ),
)

private val tagToGenreGroup: Map<String, GenreGroup> = genreMapping.flatMap { (group, tags) -> tags.map { it to group } }.toMap()

fun genreGroupOf(tag: String): GenreGroup? = tagToGenreGroup[tag]

fun performanceTypeOf(genreGroup: GenreGroup): PerformanceType? = genreToPerformanceType[genreGroup]

fun genreGroupsFor(performanceType: PerformanceType): List<GenreGroup> = performanceTypeMapping[performanceType].orEmpty()

fun PerformanceType.displayName(): String =
    when (this) {
        PerformanceType.MUSIC -> "Music"
        PerformanceType.DANCE -> "Dance"
        PerformanceType.ARTS_CULTURE -> "Arts & Culture"
    }

// Shared by Schedule and Discover filtering: an artist passes if any of its tags map to a
// selected genre group, falling back to UNKNOWN when the artist has no recognized tags at all.
fun passesGenreFilter(
    tags: List<String>?,
    selectedGenreGroups: Set<GenreGroup>,
): Boolean {
    val genreGroups = tags?.mapNotNull { genreGroupOf(it) }.orEmpty()
    val effectiveGroups = genreGroups.ifEmpty { listOf(GenreGroup.UNKNOWN) }

    return effectiveGroups.any { it in selectedGenreGroups }
}

fun GenreGroup.displayName(): String =
    when (this) {
        GenreGroup.ROCK -> "Rock"
        GenreGroup.POP -> "Pop"
        GenreGroup.INDIE -> "Indie"
        GenreGroup.JAZZ -> "Jazz"
        GenreGroup.ELECTRONIC -> "Electronic"
        GenreGroup.TECHNO -> "Techno"
        GenreGroup.HOUSE -> "House"
        GenreGroup.RAP -> "Rap"
        GenreGroup.HIP_HOP -> "Hip-Hop"
        GenreGroup.EXPERIMENTAL -> "Experimental"
        GenreGroup.FOLK -> "Folk"
        GenreGroup.DISCO -> "Disco"
        GenreGroup.TRANCE -> "Trance"
        GenreGroup.BASS -> "Bass"
        GenreGroup.WORLD -> "World"
        GenreGroup.DANCE -> "Dance"
        GenreGroup.PERFORMANCE -> "Performance"
        GenreGroup.COMEDY -> "Comedy"
        GenreGroup.WORKSHOP -> "Workshop"
        GenreGroup.VISUAL_ART -> "Visual Art"
        GenreGroup.UNKNOWN -> "Unknown"
    }
