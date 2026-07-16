package com.ilyne.helloszigetkmp.domain.usecase

import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetLikedArtistCountUseCase(
    private val artistRepository: ArtistRepository,
) {
    operator fun invoke(): Flow<Int> =
        artistRepository.observeArtists().map { artists ->
            artists.sumOf { if (it.isFavorited) 1 else 0 }
        }
}
