package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.db.SetTimeEntity
import com.ilyne.helloszigetkmp.data.db.StageEntity
import com.ilyne.helloszigetkmp.data.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.data.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.data.db.dao.StageDao
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
import com.ilyne.helloszigetkmp.domain.model.SetTimeDays
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.util.Logger
import com.ilyne.helloszigetkmp.util.datetime.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ScheduleRepository(
    private val api: SzigetApiService,
    private val setTimeDao: SetTimeDao,
    private val stageDao: StageDao,
    private val artistDao: ArtistDao,
) {
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

    fun observeSetTimeDays(): Flow<SetTimeDays> =
        setTimeDao.observeSetTimeRange().map { setTimeRange ->
            val startDate = setTimeRange.minStart.toFestivalDate()
            val endDate = setTimeRange.maxStart.toFestivalDate()

            val days = generateSequence(startDate) { it.plus(1, DateTimeUnit.DAY) }
                .takeWhile { it <= endDate }
                .map { date ->
                    val dayStartMillis = date
                        .atTime(FESTIVAL_DAY_CUTOFF_HOURS.inWholeHours.toInt(), 0)
                        .toInstant(DateTimeUtils.FESTIVAL_TIME_ZONE)
                        .toEpochMilliseconds()

                    SetTimeDay(
                        dayStartMillis = dayStartMillis,
                        dayEndMillis = dayStartMillis + 24.hours.inWholeMilliseconds,
                        dateOfMonth = date.day,
                        dayOfWeek = date.dayOfWeek.isoDayNumber,
                    )
                }.toList()

            SetTimeDays(days = days)
        }

    /**
     * Maps an epoch millis timestamp to its "festival date" — a day spans 6am to 6am local time, so a set at 2am is still considered part
     * of the previous calendar day.
     */
    private fun Long.toFestivalDate(): LocalDate =
        (Instant.fromEpochMilliseconds(this) - FESTIVAL_DAY_CUTOFF_HOURS).toLocalDateTime(DateTimeUtils.FESTIVAL_TIME_ZONE).date

    private companion object {
        const val TAG = "ScheduleRepository"
        val FESTIVAL_DAY_CUTOFF_HOURS = 6.hours
    }

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
