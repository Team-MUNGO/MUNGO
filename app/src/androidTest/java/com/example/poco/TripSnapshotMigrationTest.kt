package com.example.poco

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.data.record.TripDatabase
import com.example.poco.data.record.TRIP_MIGRATION_1_2
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TripSnapshotMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TripDatabase::class.java)

    @Test fun migrationPreservesExistingEventsWithoutInventingLocations() {
        val name = "trip-snapshot-migration-" + java.util.UUID.randomUUID()
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO trip_events (tripId, atEpoch, type, scoreDelta, scoreAfter, detail) VALUES ('old', 1000, 'SCORE_PENALTY', -4, 96, 'SHARP_TURN')")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, TRIP_MIGRATION_1_2).apply {
            query("SELECT scoreAfter, latitude, longitude, temperatureC, scoreBefore FROM trip_events WHERE tripId = 'old'").use {
                assertTrue(it.moveToFirst())
                assertEquals(96, it.getInt(0))
                assertTrue(it.isNull(1))
                assertTrue(it.isNull(2))
                assertTrue(it.isNull(3))
                assertTrue(it.isNull(4))
            }
            close()
        }
    }
}
