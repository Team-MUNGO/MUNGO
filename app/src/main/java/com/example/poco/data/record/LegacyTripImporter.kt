package com.example.poco.data.record

import com.example.poco.data.local.datasource.TripLocalDataSource
import com.example.poco.data.repository.PetRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Trips saved by the old driving screen live in poco.db. They are copied once per process into the
 * single trip store (id "legacy-<id>", insert-or-ignore, so repeats are no-ops). The old rows are
 * left untouched; nothing is deleted.
 */
@Singleton
class LegacyTripImporter @Inject constructor(
    private val legacy: TripLocalDataSource,
    private val dao: TripDao,
    private val pets: PetRepository
) {
    private val mutex = Mutex()
    private var done = false

    suspend fun importOnce() = mutex.withLock {
        if (done) return@withLock
        runCatching {
            val old = legacy.observeRecentTrips(Int.MAX_VALUE).first()
            if (old.isNotEmpty()) {
                // The old table has no pet columns; the current first pet is the best label we have.
                val pet = pets.observePets().first().firstOrNull()
                old.forEach { trip ->
                    dao.insertTrip(
                        trip.toRecord(
                            id = "legacy-${trip.id}",
                            petName = pet?.name?.takeIf { it.isNotBlank() } ?: "반려견",
                            seat = "UNKNOWN"
                        )
                    )
                }
            }
        }.onFailure { Timber.w(it, "이전 이동 기록 가져오기 실패") }
        done = true
    }
}
