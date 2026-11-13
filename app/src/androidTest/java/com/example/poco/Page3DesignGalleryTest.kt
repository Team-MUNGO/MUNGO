package com.example.poco

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.core.designsystem.FitWideWindow
import com.example.poco.core.theme.MungoTheme
import com.example.poco.data.vehicle.VehicleIdentity
import com.example.poco.data.vehicle.VehicleLinkState
import com.example.poco.presentation.boarding.ChecklistPreview
import com.example.poco.presentation.boarding.CompanionsPreview
import com.example.poco.presentation.boarding.SeatPreview
import com.example.poco.presentation.home.HomeScreenPreview
import com.example.poco.presentation.mypet.MyPetDetailPreview
import com.example.poco.presentation.mypet.MyPetEditPreview
import com.example.poco.presentation.onboarding.AlertsPreview
import com.example.poco.presentation.onboarding.BasicPreview
import com.example.poco.presentation.onboarding.CompletePreview
import com.example.poco.presentation.onboarding.TraitsPreview
import com.example.poco.presentation.record.AlertSettings
import com.example.poco.presentation.record.AlertsPage
import com.example.poco.presentation.record.AppInfoPage
import com.example.poco.presentation.record.SettingsPage
import com.example.poco.presentation.record.SettingsScreen
import com.example.poco.presentation.record.VehiclePage
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Render existing preview fixtures for visual comparison with Figma Page 3. */
class Page3DesignGalleryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun page3Gallery() {
        val screen = mutableStateOf("home")
        compose.setContent {
            MungoTheme(darkTheme = false) {
                when (screen.value) {
                    "settings-alerts" -> SettingsScreen(SettingsPage.ALERTS, {
                    }, {}) { AlertsPage(AlertSettings(hasPet = true), {}, {}, {}, {}) }
                    "settings-vehicle" -> SettingsScreen(SettingsPage.VEHICLE, {
                    }, {
                    }) {
                        VehiclePage(
                            VehicleLinkState(),
                            VehicleIdentity(
                                "그랜저",
                                "현대",
                                System.currentTimeMillis()
                            ),
                            true,
                            {
                            },
                            {},
                            {}
                        )
                    }
                    "app-info" -> SettingsScreen(SettingsPage.APP_INFO, {
                    }, {}) { AppInfoPage("1.0.0", true) }
                    else -> FitWideWindow(minWidth = androidx.compose.ui.unit.Dp(1700f)) {
                        when (screen.value) {
                            "home" -> HomeScreenPreview()
                            "pet" -> MyPetDetailPreview()
                            "pet-edit" -> MyPetEditPreview()
                            "basic" -> BasicPreview()
                            "traits" -> TraitsPreview()
                            "alerts" -> AlertsPreview()
                            "complete" -> CompletePreview()
                            "boarding-pets" -> CompanionsPreview()
                            "boarding-seat" -> SeatPreview()
                            "boarding-checklist" -> ChecklistPreview()
                        }
                    }
                }
            }
        }
        for (name in listOf(
            "home",
            "pet",
            "pet-edit",
            "basic",
            "traits",
            "alerts",
            "complete",
            "app-info",
            "settings-alerts",
            "settings-vehicle",
            "boarding-pets",
            "boarding-seat",
            "boarding-checklist"
        )) {
            compose.runOnIdle { screen.value = name }
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.filesDir, "page3-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
