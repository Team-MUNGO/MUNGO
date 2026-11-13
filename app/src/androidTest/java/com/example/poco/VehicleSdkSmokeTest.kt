package com.example.poco

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.vehicle.Vehicle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.core.driving.PetSeat
import com.example.poco.data.automotive.VehicleDataSourceImpl
import com.example.poco.data.automotive.VehicleUpdate
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasourceimpl.HvacDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.NaviDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.TtsDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.WindowDataSourceImpl
import com.example.poco.data.repositoryimpl.WindowRepositoryImpl
import java.io.File
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit integration probe, no window writes or simulated sensor values. */
class VehicleSdkSmokeTest {
    @Test fun readsVehicleServices() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("sdkSmoke") == "true")
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            "android.car.permission.CAR_SPEED"
        )
        val report = StringBuilder()
        suspend fun record(name: String, block: suspend () -> Any?) {
            val result = runCatching { withTimeout(15000) { block() } }
            report.append(name).append(": ").append(
                result.fold({
                    it.toString()
                }, { "UNAVAILABLE: ${it.message}" })
            ).append('\n')
        }
        val vehicle = Vehicle(context)
        val tts = TextToSpeech(context, ai.pleos.playground.tts.constant.Mode.ON_DEVICE)
        val manager =
            PleosSdkManager(
                vehicle,
                tts,
                SpeechToText(context, ai.pleos.playground.stt.constant.Mode.ON_DEVICE),
                ai.pleos.playground.location.fused.FusedLocation(context),
                ai.pleos.playground.navi.helper.NaviHelper(context)
            )
        try {
            record("AAOS speed") {
                VehicleDataSourceImpl(
                    context
                ).observe().filterIsInstance<VehicleUpdate.Speed>().first()
            }
            record("AAOS gear") {
                VehicleDataSourceImpl(
                    context
                ).observe().filterIsInstance<VehicleUpdate.Gear>().first()
            }
            record("Interior Celsius") {
                HvacDataSourceImpl(
                    vehicle.getHVAC(),
                    manager
                ).observeInteriorTemperature(PetSeat.PASSENGER).first()
            }
            record("Window capabilities") {
                WindowRepositoryImpl(
                    WindowDataSourceImpl(vehicle.getWindow(), manager)
                ).getSupportedWindows()
            }
            record("Navi connection") {
                NaviDataSourceImpl(
                    ai.pleos.playground.navi.helper.NaviHelper(context),
                    manager,
                    context
                ).observeBumps().first()
            }
            record("TTS playback callbacks") {
                TtsDataSourceImpl(tts, manager).speak("창문 안전 안내 테스트입니다.") { }
                "completed"
            }
        } finally {
            runCatching { vehicle.release() }
            File(
                context.filesDir,
                "vehicle-sdk-smoke.txt"
            ).writeText(report.toString())
        }
    }
}
