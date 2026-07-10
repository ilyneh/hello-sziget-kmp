package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.core.db.dao.StageDao
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ScheduleRepository(
    private val api: SzigetApiService,
    private val setTimeDao: SetTimeDao,
    private val stageDao: StageDao,
    private val artistDao: ArtistDao,
) {

    companion object {
        private const val TAG = "ScheduleRepository"
    }

    data class SetTimesForDay(
        val setTimes: List<SetTime>,
        val stages: List<Stage>,
    )

    fun observeSetTimesForDay(
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<SetTimesForDay> =
        combine(
            setTimeDao.observeByDay(dayStartMillis, dayEndMillis),
            stageDao.observeAll(),
            artistDao.observeAll(),
        ) { setTimes, stageEntities, artists ->
            val stages = stageEntities.map { Stage(it.id, it.name, it.description) }
            val stageMap = stages.associateBy { it.id }
            val artistMap = artists.associateBy { it.id }
            SetTimesForDay(
                setTimes = setTimes.map { st ->
                    SetTime(
                        id = st.id,
                        artistId = st.artistId,
                        stageId = st.stageId,
                        startTime = st.startTime,
                        endTime = st.endTime,
                        hideEndTime = st.hideEndTime,
                        artist = artistMap[st.artistId]?.let {
                            Artist(it.id, it.name, it.bio, imageUrl = it.imageUrl, it.isFavorited, it.tags)
                        },
                        stage = stageMap[st.stageId],
                    )
                },
                stages = stages,
            )
        }


    fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> =
        setTimeDao.observeSetTimeRange()


    fun observeFavoriteSetTimes(): Flow<List<SetTimeWithArtistStageSummary>> =
        setTimeDao.observeFavorites()


    suspend fun refresh() {
        val stages = api.getStages()
        stageDao.upsertAll(stages.map { StageEntity(it.id, it.name, it.description) })
        Logger.d(TAG, "Refreshed ${stages.size} stages")

        val setTimes = api.getSetTimes()
        setTimeDao.upsertAll(
            setTimes.map {
                SetTimeEntity(it.id, it.artistId, it.stageId, it.startTime, it.endTime, it.hideEndTime)
            },
        )
        Logger.d(TAG, "Refreshed ${setTimes.size} set times")
    }
}
