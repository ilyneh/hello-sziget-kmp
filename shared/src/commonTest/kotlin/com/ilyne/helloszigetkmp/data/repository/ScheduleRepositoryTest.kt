//package com.ilyne.helloszigetkmp.data.repository
//
//import com.ilyne.helloszigetkmp.data.api.SzigetApiService
//import com.ilyne.helloszigetkmp.data.db.dao.ArtistDao
//import com.ilyne.helloszigetkmp.data.db.ArtistEntity
//import com.ilyne.helloszigetkmp.data.db.SetTimeDao
//import com.ilyne.helloszigetkmp.data.db.SetTimeEntity
//import com.ilyne.helloszigetkmp.data.db.StageDao
//import com.ilyne.helloszigetkmp.data.db.StageEntity
//import com.ilyne.helloszigetkmp.domain.model.SetTimeDay
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.mock.MockEngine
//import io.ktor.client.engine.mock.respondOk
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.flowOf
//import kotlinx.coroutines.test.runTest
//import kotlinx.datetime.LocalDateTime
//import kotlinx.datetime.TimeZone
//import kotlinx.datetime.toInstant
//import kotlin.test.Test
//import kotlin.test.assertEquals
//
///**
// * [ScheduleRepository.observeSetTimeDays] buckets the min/max set time range into festival days using a 6am local cutoff, so a set that
// * runs past midnight still belongs to the previous festival day rather than starting a new one.
// */
//class ScheduleRepositoryTest {
//    private val budapest = TimeZone.of("Europe/Budapest")
//
//    @Test
//    fun singleSetTime_onSameDay_producesOneDay() =
//        runTest {
//            val range = SetTimeDao.SetTimeRange(
//                minStart = millisAt("2026-08-06T18:00:00"),
//                maxStart = millisAt("2026-08-06T20:00:00"),
//            )
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            // Aug 6 2026 is a Thursday (isoDayNumber 4)
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-06T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-07T06:00:00"),
//                        dateOfMonth = 6,
//                        dayOfWeek = 4,
//                    ),
//                ),
//                result.days,
//            )
//        }
//
//    @Test
//    fun setTimeJustBeforeCutoff_isGroupedWithPreviousDay() =
//        runTest {
//            // 5:59am is still "last night" for a festival — it should collapse onto Aug 6, not start Aug 7
//            val range = SetTimeDao.SetTimeRange(
//                minStart = millisAt("2026-08-07T05:59:00"),
//                maxStart = millisAt("2026-08-07T05:59:00"),
//            )
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-06T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-07T06:00:00"),
//                        dateOfMonth = 6,
//                        dayOfWeek = 4,
//                    ),
//                ),
//                result.days,
//            )
//        }
//
//    @Test
//    fun setTimeExactlyAtCutoff_startsNewDay() =
//        runTest {
//            // 6:00am exactly is the boundary — this should count as the new day, not the previous one
//            val range = SetTimeDao.SetTimeRange(
//                minStart = millisAt("2026-08-07T06:00:00"),
//                maxStart = millisAt("2026-08-07T06:00:00"),
//            )
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            // Aug 7 2026 is a Friday (isoDayNumber 5)
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-07T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-08T06:00:00"),
//                        dateOfMonth = 7,
//                        dayOfWeek = 5,
//                    ),
//                ),
//                result.days,
//            )
//        }
//
//    @Test
//    fun multiDayRange_producesConsecutiveDays() =
//        runTest {
//            val range = SetTimeDao.SetTimeRange(
//                minStart = millisAt("2026-08-06T18:00:00"),
//                maxStart = millisAt("2026-08-08T14:00:00"),
//            )
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-06T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-07T06:00:00"),
//                        dateOfMonth = 6,
//                        dayOfWeek = 4,
//                    ), // Thu
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-07T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-08T06:00:00"),
//                        dateOfMonth = 7,
//                        dayOfWeek = 5,
//                    ), // Fri
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-08T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-09T06:00:00"),
//                        dateOfMonth = 8,
//                        dayOfWeek = 6,
//                    ), // Sat
//                ),
//                result.days,
//            )
//        }
//
//    @Test
//    fun monthRollover_isHandledCorrectly() =
//        runTest {
//            val range = SetTimeDao.SetTimeRange(
//                minStart = millisAt("2026-01-31T18:00:00"),
//                maxStart = millisAt("2026-02-01T20:00:00"),
//            )
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-01-31T06:00:00"),
//                        dayEndMillis = millisAt("2026-02-01T06:00:00"),
//                        dateOfMonth = 31,
//                        dayOfWeek = 6,
//                    ), // Jan 31 2026, Sat
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-02-01T06:00:00"),
//                        dayEndMillis = millisAt("2026-02-02T06:00:00"),
//                        dateOfMonth = 1,
//                        dayOfWeek = 7,
//                    ), // Feb 1 2026, Sun
//                ),
//                result.days,
//            )
//        }
//
//    @Test
//    fun yearRollover_isHandledCorrectly() =
//        runTest {
//            val range = SetTimeDao.SetTimeRange(
//                minStart = millisAt("2026-12-31T22:00:00"),
//                // 2am Jan 1 is before the 6am cutoff, so it collapses onto Dec 31's festival day
//                maxStart = millisAt("2027-01-01T02:00:00"),
//            )
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-12-31T06:00:00"),
//                        dayEndMillis = millisAt("2027-01-01T06:00:00"),
//                        dateOfMonth = 31,
//                        dayOfWeek = 4,
//                    ),
//                ),
//                result.days,
//            ) // Dec 31 2026, Thu
//        }
//
//    @Test
//    fun minAndMaxOnSameInstant_producesSingleDay() =
//        runTest {
//            val onlyMillis = millisAt("2026-08-09T21:00:00")
//            val range = SetTimeDao.SetTimeRange(minStart = onlyMillis, maxStart = onlyMillis)
//
//            val result = repository(range).observeSetTimeDays().first()
//
//            assertEquals(
//                listOf(
//                    SetTimeDay(
//                        dayStartMillis = millisAt("2026-08-09T06:00:00"),
//                        dayEndMillis = millisAt("2026-08-10T06:00:00"),
//                        dateOfMonth = 9,
//                        dayOfWeek = 7,
//                    ),
//                ),
//                result.days,
//            ) // Aug 9 2026, Sun
//        }
//
//    private fun millisAt(isoLocalDateTime: String): Long = LocalDateTime.parse(isoLocalDateTime).toInstant(budapest).toEpochMilliseconds()
//
//    private fun repository(range: SetTimeDao.SetTimeRange): ScheduleRepository {
//        val setTimeDao = object : SetTimeDao {
//            override fun observeAll(): Flow<List<SetTimeEntity>> = flowOf(emptyList())
//
//            override fun observeByDay(
//                dayStartMillis: Long,
//                dayEndMillis: Long,
//            ): Flow<List<SetTimeEntity>> = flowOf(emptyList())
//
//            override suspend fun upsertAll(setTimes: List<SetTimeEntity>) {}
//
//            override fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> = flowOf(range)
//        }
//        val stageDao = object : StageDao {
//            override fun observeAll(): Flow<List<StageEntity>> = flowOf(emptyList())
//
//            override suspend fun upsertAll(stages: List<StageEntity>) {}
//        }
//        val artistDao = object : ArtistDao {
//            override fun observeAll(): Flow<List<ArtistEntity>> = flowOf(emptyList())
//
//            override fun observeById(id: String): Flow<ArtistEntity?> = flowOf(null)
//
//            override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(emptyList())
//
//            override suspend fun upsertAll(artists: List<ArtistEntity>) {}
//
//            override suspend fun setFavorited(
//                id: String,
//                isFavorited: Boolean,
//            ) {}
//        }
//        val api = SzigetApiService(
//            client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
//            baseUrl = "https://unused.test",
//        )
//        return ScheduleRepository(api = api, setTimeDao = setTimeDao, stageDao = stageDao, artistDao = artistDao)
//    }
//}
