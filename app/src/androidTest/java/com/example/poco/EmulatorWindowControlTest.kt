package com.example.poco

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.vehicle.Vehicle
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.core.driving.ComfortScoreConfig
import com.example.poco.core.driving.DrivingClock
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.automotive.VehicleUpdate
import com.example.poco.data.model.CarWindow
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasourceimpl.WindowDataSourceImpl
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.VehicleRepository
import com.example.poco.data.repositoryimpl.DrivingSessionRepositoryImpl
import com.example.poco.data.repositoryimpl.WindowRepositoryImpl
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in simulator-only actuator check. Never run window writes on a real vehicle. */
class EmulatorWindowControlTest {
    @Test fun reportsActuatorSupportAndChecksReadback() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("sdkWindowControl") == "true")
        assumeTrue(Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vehicle = Vehicle(context)
        val manager = PleosSdkManager(
            vehicle,
            TextToSpeech(context, ai.pleos.playground.tts.constant.Mode.ON_DEVICE),
            SpeechToText(context, ai.pleos.playground.stt.constant.Mode.ON_DEVICE),
            ai.pleos.playground.location.fused.FusedLocation(context),
            ai.pleos.playground.navi.helper.NaviHelper(context)
        )
        val windows = WindowRepositoryImpl(WindowDataSourceImpl(vehicle.getWindow(), manager))
        val scopeClock = DrivingClock { android.os.SystemClock.elapsedRealtime() }
        val session = DrivingSessionRepositoryImpl(
            object : VehicleRepository {
                override fun observe() = emptyFlow<VehicleUpdate>()
            },
            object : HvacRepository {
                override fun observeInteriorTemperature(seat: PetSeat) = emptyFlow<Reading<Float>>()
                override suspend fun getSeatTemperature(seat: com.example.poco.data.model.PetSeat) =
                    Result.success<Float?>(null)
                override fun observeSeatTemperature(seat: com.example.poco.data.model.PetSeat) =
                    emptyFlow<Float>()
                override suspend fun setSeatTemperature(
                    seat: com.example.poco.data.model.PetSeat,
                    celsius: Float
                ) = Result.success<Float?>(null)
            },
            windows,
            object : NaviRepository {
                override fun observeEvents() = emptyFlow<com.example.poco.data.model.NaviEvent>()
                override suspend fun openNavigation(keyword: String?) = Result.success(Unit)
                override fun observeBumps() = emptyFlow<Reading<List<BumpApproach>>>()
                override fun observeRoute() =
                    emptyFlow<com.example.poco.data.pleos.datasource.NaviRouteUpdate>()
            },
            object : TtsRepository {
                override suspend fun speak(text: String) = Result.success(Unit)
                override suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit) = Unit
            },
            scopeClock,
            ComfortScoreConfig(),
            object : com.example.poco.data.settings.UserSettingsRepository {
                override val restIntervalMinutes = kotlinx.coroutines.flow.flowOf(120)
                override val voiceGuidanceEnabled = kotlinx.coroutines.flow.flowOf(false)
                override suspend fun setRestIntervalMinutes(minutes: Int) = Unit
                override suspend fun setVoiceGuidanceEnabled(enabled: Boolean) = Unit
            },
            object : com.example.poco.data.record.TripRecordSink {
                override fun begin(trip: com.example.poco.data.record.TripEntity) = Unit
                override fun event(event: com.example.poco.data.record.TripEventEntity) = Unit
                override suspend fun complete(
                    tripId: String,
                    endedAt: Long,
                    finalScore: Int,
                    drivingMs: Long,
                    restMs: Long,
                    restCount: Int
                ) = true
            },
            object : com.example.poco.data.assistant.AssistantWaypointSource {
                override suspend fun add(place: com.example.poco.core.driving.VerifiedPlace) =
                    error("not used")
                override suspend fun remove(place: com.example.poco.core.driving.VerifiedPlace) =
                    error("not used")
            },
            object : com.example.poco.data.assistant.LocationStream {
                override fun observe() = emptyFlow<com.example.poco.data.assistant.LocationFix>()
            },
            com.example.poco.data.repositoryimpl.DataOrigin { true },
            object : com.example.poco.data.repository.VehicleMotionRepository {
                override fun observeSpeedKmh() = kotlinx.coroutines.flow.emptyFlow<Float>()
                override fun observeSteeringAngle() = kotlinx.coroutines.flow.emptyFlow<Float>()
                override fun observeBrakePedalPressed() =
                    kotlinx.coroutines.flow.emptyFlow<Boolean>()
                override fun observeLocation() =
                    kotlinx.coroutines.flow.emptyFlow<com.example.poco.data.model.VehicleLocation>()
            }
        )
        var initial: Int? = null
        var stage = "capabilities"
        val evidence = StringBuilder()
        try {
            val range = windows.getSupportedWindows().getOrThrow().getValue(CarWindow.PASSENGER)
            initial = windows.getWindowPosition(CarWindow.PASSENGER).getOrThrow()
            val openRaw = (range.max * 0.5).toInt().coerceAtLeast(1)
            evidence.append(
                "initial="
            ).append(initial).append(" openTarget=").append(openRaw).append('\n')
            windows.setWindowPosition(CarWindow.PASSENGER, openRaw).getOrThrow()
            stage = "opening readback"
            evidence.append(
                "afterOpen="
            ).append(windows.getWindowPosition(CarWindow.PASSENGER)).append('\n')
            withContext(Dispatchers.Main) {
                session.start(DrivingSettings("에뮬레이터 테스트", PetSeat.PASSENGER, 20f, 25f))
            }
            stage = "opening verification"
            var opened = false
            repeat(6) {
                kotlinx.coroutines.delay(350)
                if (windows.getWindowPosition(CarWindow.PASSENGER).getOrThrow() == openRaw) {
                    opened = true
                }
            }
            if (!opened) {
                val actual = windows.getWindowPosition(CarWindow.PASSENGER).getOrThrow()
                val snapshot = withTimeout(10000) {
                    session.state.filter { it.windowPercent.valueOrNull() != null }.first()
                }
                assertEquals(initial, actual)
                assertFalse(snapshot.windowWarning)
                File(context.filesDir, "window-control-result.txt").writeText(
                    "UNSUPPORTED IN THIS SIMULATOR: set returned success but raw position stayed " +
                        "$actual (target $openRaw). " +
                        "App retained observed closed state; no false warning. " +
                        "Actual closing movement remains unverified. Other inputs were test fakes."
                )
                return@runBlocking
            }
            stage = "warning detection"
            withTimeout(15000) { session.state.filter { it.windowWarning }.first() }
            stage = "closing readback"
            withContext(Dispatchers.Main) { session.closeWindow() }
            val closed = withTimeout(15000) {
                session.state.filter {
                    !it.windowWarning && it.windowPercent.valueOrNull() == 0.0
                }.first()
            }
            assertFalse(closed.windowWarning)
            assertEquals(0, windows.getWindowPosition(CarWindow.PASSENGER).getOrThrow())
            File(context.filesDir, "window-control-result.txt").writeText(
                "Simulator-only: observed >20% opening, warning shown, close requested, actual readback=0, warning cleared. Other inputs were test fakes."
            )
        } catch (error: Exception) {
            evidence.append("FAILED at ").append(stage).append(": ").append(error).append('\n')
            evidence.append("session=").append(session.state.value).append('\n')
            evidence.append(
                "readback="
            ).append(windows.getWindowPosition(CarWindow.PASSENGER)).append('\n')
            File(context.filesDir, "window-control-result.txt").writeText(evidence.toString())
            throw error
        } finally {
            withContext(Dispatchers.Main) { session.end() }
            initial?.let { windows.setWindowPosition(CarWindow.PASSENGER, it) }
            vehicle.release()
        }
    }
}
