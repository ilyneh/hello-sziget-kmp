package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.data.db.entity.StageEntity
import com.ilyne.helloszigetkmp.data.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.data.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.data.db.dao.StageDao
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ScheduleRepository(
    private val api: SzigetApiService,
    private val setTimeDao: SetTimeDao,
    private val stageDao: StageDao,
    private val artistDao: ArtistDao,
) {

    companion object {
        private const val TAG = "ScheduleRepository"
    }

    fun observeSetTimesForDay(
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<List<SetTime>> =
        combine(
            setTimeDao.observeByDay(dayStartMillis, dayEndMillis),
            stageDao.observeAll(),
            artistDao.observeAll(),
        ) { setTimes, stages, artists ->
            val stageMap = stages.associateBy { it.id }
            val artistMap = artists.associateBy { it.id }
            setTimes.map { st ->
                SetTime(
                    id = st.id,
                    artistId = st.artistId,
                    stageId = st.stageId,
                    startTime = st.startTime,
                    endTime = st.endTime,
                    hideEndTime = st.hideEndTime,
                    artist = artistMap[st.artistId]?.let {
                        Artist(it.id, it.name, it.bio, it.isFavorited, it.tags)
                    },
                    stage = stageMap[st.stageId]?.let {
                        Stage(it.id, it.name, it.description)
                    },
                )
            }
        }

    fun observeStages(): Flow<List<Stage>> =
        stageDao.observeAll().map { entities ->
            entities.map { Stage(it.id, it.name, it.description) }
        }


    fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> =
        setTimeDao.observeSetTimeRange()

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
