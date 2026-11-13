package com.example.poco

import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.data.assistant.PleosLocationSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in, on-demand real SDK read; never records or transmits coordinates. */
class FusedLocationSdkSmokeTest {
    @Test fun currentLocationUsesPublicPleosService() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("fusedSmoke") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val result = runCatching {
            runBlocking { withContext(Dispatchers.Main) { PleosLocationSource(context).current() } }
        }
        val report = if (result.isSuccess) {
            val here = result.getOrThrow()
            // Plausibility only (no coordinates written): not the 0,0 placeholder and inside Korea's bounds.
            val notZero = here.latitude != 0.0 || here.longitude != 0.0
            val inKorea = here.latitude in 33.0..38.7 && here.longitude in 124.5..131.0
            "Fresh validated location received; ageMs=${here.ageMs}, notZero=$notZero, inKorea=$inKorea. Coordinates omitted."
        } else {
            "Location unavailable: ${result.exceptionOrNull()?.javaClass?.simpleName}. No coordinates logged."
        }
        File(context.filesDir, "fused-location-result.txt").writeText(report)
        assertTrue(report, result.isSuccess)
    }
}
