package com.ilyne.helloszigetkmp.presentation.feature.discover.filter.usecase

import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.displayName
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilter

class GetActiveDiscoverFiltersTextUseCase {
    operator fun invoke(filter: DiscoverFilter): List<String> =
        PerformanceType.entries
            .filter { type -> genreGroupsFor(type).any { it in filter.selectedGenreGroups } }
            .map { it.displayName() }
}
