package com.ilyne.helloszigetkmp.presentation.util

/**
 * Shared shape for the "Loading / Success / Error" status enum that recurs across several
 * feature `UiState`s (Discover, Schedule, MyLineup, ArtistDetail). Each feature parameterizes
 * [Error] with its own reason type (e.g. a `String?` message or a feature-specific error-reason
 * enum) while sharing the same [Loading]/[Success] markers.
 *
 * `AddFriendUiState.Status` is intentionally not migrated to this type: it has no [Loading]
 * state (search results are never shown as loading) and its baseline state is semantically an
 * "idle" state rather than a completed "success" load, so forcing it into this shape would be
 * an awkward fit rather than a genuine simplification.
 */
sealed class LoadStatus<out E> {
    data object Loading : LoadStatus<Nothing>()

    data object Success : LoadStatus<Nothing>()

    data class Error<out E>(
        val reason: E,
    ) : LoadStatus<E>()
}
