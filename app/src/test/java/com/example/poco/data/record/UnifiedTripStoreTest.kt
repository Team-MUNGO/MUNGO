package com.example.poco.data.record

import com.example.poco.core.driving.PetSeat as SessionSeat
import com.example.poco.data.local.datasource.TripLocalDataSource
import com.example.poco.data.model.EventStat
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.TripSummary
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repositoryimpl.toDrivingSettings
import com.example.poco.domain.driving.temperatureStandard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnifiedTripStoreTest {
    private val pet = Pet(
        name = "현댕이",
        breed = "시츄",
        ageYears = 3,
        weightKg = 6.3f,
        seat = PetSeat.REAR_RIGHT,
        motionSensitivity = MotionSensitivity.NORMAL
    )

    /** The driving session watches the 마이펫 pet: name, seat, weight and the POCO comfort range. */
    @Test fun petProfileBecomesSessionSettings() {
        val s = pet.toDrivingSettings()!!
        assertEquals("현댕이", s.petName)
        assertEquals(SessionSeat.REAR_RIGHT, s.seat)
        assertEquals(63, s.weightTenths)
        assertEquals(pet.temperatureStandard.comfort.start, s.minTemperatureC)
        assertEquals(pet.temperatureStandard.comfort.endInclusive, s.maxTemperatureC)
        assertNull(pet.copy(weightKg = null).toDrivingSettings()!!.weightTenths)
        assertNull(pet.copy(name = " ").toDrivingSettings())
    }

    private class FakeDao : TripDao {
        val trips = MutableStateFlow<Map<String, TripEntity>>(emptyMap())
        override suspend fun insertTrip(trip: TripEntity): Long {
            if (trip.id in trips.value) return -1L
            trips.value = trips.value + (trip.id to trip)
            return 1L
        }
        override fun observeFinishedTrips(): Flow<List<TripEntity>> = trips.map { m ->
            m.values.filter {
                it.status != "IN_PROGRESS"
            }.sortedByDescending { it.startedAtEpoch }
        }
        override suspend fun insertEvent(event: TripEventEntity) = 0L
        override suspend fun events(tripId: String) = emptyList<TripEventEntity>()
        override fun observeEvents(tripId: String) = flowOf(emptyList<TripEventEntity>())
        override suspend fun trip(id: String) = trips.value[id]
        override fun observeTrip(id: String) = trips.map { it[id] }
        override suspend fun complete(
            id: String,
            endedAt: Long,
            finalScore: Int,
            minScore: Int,
            drivingMs: Long,
            restMs: Long,
            restCount: Int,
            accel: Int,
            brake: Int,
            sharp: Int,
            repeated: Int,
            bumps: Int,
            windows: Int,
            temps: Int
        ) = 0
        override suspend fun markInterrupted(keep: String) = 0
        override suspend fun setMotionSickness(id: String, signs: String?) = Unit
        override suspend fun setOneLine(id: String, text: String) = Unit
    }

    private fun summary(id: Long, start: Long, score: Int) = TripSummary(
        id = id, startedAtMillis = start, endedAtMillis = start + 30 * 60_000, distanceKm = 0f,
        comfortScore = score,
        hardBrake = EventStat(
            2,
            -6
        ),
        hardAccel = EventStat(), sharpTurn = EventStat(1, -3),
        calmRecovery = EventStat(),
        temperatureAlert = EventStat(),
        windowAlertCount = 1,
        calmMinutes = 5,
        timeline = emptyList()
    )

    private fun stores(legacyTrips: List<TripSummary>): Pair<FakeDao, RecordTripRepository> {
        val dao = FakeDao()
        val legacy = object : TripLocalDataSource {
            override fun observeRecentTrips(limit: Int) = flowOf(legacyTrips)
            override suspend fun getTrip(id: Long) = null
            override suspend fun getPreviousTrip(beforeMillis: Long) = null
            override suspend fun insertTrip(trip: TripSummary) = error("old store is read-only now")
        }
        val pets = object : PetRepository {
            override fun observePets() = flowOf(listOf(pet))
            override suspend fun savePet(pet: Pet) = Result.success(0L)
            override suspend fun deletePet(pet: Pet) = Result.success(Unit)
            override suspend fun importPhoto(uri: String) = Result.success("")
            override suspend fun deletePhoto(path: String) = Result.success(Unit)
        }
        return dao to RecordTripRepository(dao, LegacyTripImporter(legacy, dao, pets))
    }

    /** Old poco.db trips are copied once (idempotent) and the home card reads the single store. */
    @Test fun legacyTripsAreImportedOnceAndFeedHome() = runTest {
        val (dao, repo) = stores(listOf(summary(2, 2_000_000, 90), summary(1, 1_000_000, 85)))
        val last = repo.observeLastTrip().first()!!
        assertEquals(90, last.comfortScore)
        assertEquals(5, last.scoreDelta)
        assertEquals(2, dao.trips.value.size)
        val imported = dao.trips.value.getValue("legacy-2")
        assertEquals("현댕이", imported.petName)
        assertEquals("UNKNOWN", imported.seat) // the old table never stored a seat
        assertEquals(2, imported.rapidBrakingCount)
        assertNull(imported.distanceMeters) // 0 km meant "not measured", not a real distance
        repo.observeTripStats().first()
        assertEquals(2, dao.trips.value.size)
        assertEquals(87, repo.observeTripStats().first().averageScore)
    }

    /** Session trips (with events) and interrupted trips without a score: only scored ones count. */
    @Test fun unscoredTripsAreNotCountedAsScores() = runTest {
        val (dao, repo) = stores(emptyList())
        dao.insertTrip(
            TripEntity(
                id = "s1",
                petName = "현댕이",
                petWeightTenths = 63,
                petSize = "SMALL",
                seat = "REAR_RIGHT",
                startedAtEpoch = 5_000_000, endedAtEpoch = 6_000_000, status = "COMPLETED",
                startRestIntervalMinutes = 120, finalScore = 88
            )
        )
        dao.insertTrip(
            TripEntity(
                id = "s2",
                petName = "현댕이",
                petWeightTenths = 63,
                petSize = "SMALL",
                seat = "REAR_RIGHT",
                startedAtEpoch = 7_000_000,
                status = "INTERRUPTED",
                startRestIntervalMinutes = 120
            )
        )
        assertEquals(88, repo.getLatestScore().getOrThrow())
        assertEquals(1, repo.observeTripStats().first().count)
        assertEquals(88, repo.getTrip(5_000_000).getOrThrow()!!.comfortScore)
        assertNull(repo.observeLastTrip().first()!!.scoreDelta)
    }
}
