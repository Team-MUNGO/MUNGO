package com.example.poco.data.vehicle

import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.repositoryimpl.toDrivingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleLinkTest {
    /** Only the manufacturer code is read from the VIN; the emulator's "invalid" gives nothing. */
    @Test fun makerComesOnlyFromKnownVinCodes() {
        assertEquals("현대", makerFromVin("KMHxxxxxxxxxxxxxx"))
        assertEquals("기아", makerFromVin("knaxxxxxxxxxxxxxx"))
        assertNull(makerFromVin("invalid"))
        assertNull(makerFromVin(null))
        assertNull(makerFromVin("WVWxxxxxxxxxxxxxx"))
        assertNull(VehicleIdentity(null, null, 0).displayName)
        assertEquals("현대 차량", VehicleIdentity(null, "현대", 0).displayName)
        assertEquals("Grandeur", VehicleIdentity("Grandeur", "현대", 0).displayName)
    }

    /** 설정 > 알림 toggles are stored on the pet and reach the driving session. */
    @Test fun alertTogglesReachTheSession() {
        val pet = Pet(name = "현댕이", breed = "시츄", ageYears = 3, weightKg = 6f, seat = PetSeat.PASSENGER, motionSensitivity = com.example.poco.data.model.MotionSensitivity.NORMAL, windowAlertEnabled = false, restSuggestionEnabled = false)
        val s = pet.toDrivingSettings()!!
        assertFalse(s.windowAlerts)
        assertFalse(s.restSuggestions)
    }
}
