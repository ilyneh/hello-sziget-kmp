package com.ilyne.hello_sziget_kmp.data.repository

import com.ilyne.hello_sziget_kmp.data.api.SzigetApiService
import com.ilyne.hello_sziget_kmp.data.db.ArtistDao
import com.ilyne.hello_sziget_kmp.data.db.SetTimeDao
import com.ilyne.hello_sziget_kmp.data.db.SetTimeEntity
import com.ilyne.hello_sziget_kmp.data.db.StageDao
import com.ilyne.hello_sziget_kmp.data.db.StageEntity
import com.ilyne.hello_sziget_kmp.domain.model.Artist
import com.ilyne.hello_sziget_kmp.domain.model.SetTime
import com.ilyne.hello_sziget_kmp.domain.model.Stage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ScheduleRepository(
    private val api: SzigetApiService,
    private val setTimeDao: SetTimeDao,
    private val stageDao: StageDao,
    private val artistDao: ArtistDao,
) {
    fun observeSetTimesForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<SetTime>> =
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
                        Artist(it.id, it.name, it.bio, it.isFavorited)
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

    suspend fun refresh() {
        val stages = api.getStages()
        stageDao.upsertAll(stages.map { StageEntity(it.id, it.name, it.description) })

        val setTimes = api.getSetTimes()
        setTimeDao.upsertAll(setTimes.map {
            SetTimeEntity(it.id, it.artistId, it.stageId, it.startTime, it.endTime, it.hideEndTime)
        })
    }
}
