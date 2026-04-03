package com.example.gpstick.data.preset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetManagerTest {
    @Test
    fun storesAndClearsActivePreset() {
        val preset = LocationPreset(
            id = "sample",
            name = "Sample",
            summary = "Summary",
            gps = GpsPreset(1.0, 2.0, 3.0, 4f),
            wifiNetworks = emptyList(),
            cellTowers = emptyList(),
        )

        PresetManager.activatePreset(preset)

        assertEquals("sample", PresetManager.getActivePreset()?.id)

        PresetManager.clearActivePreset()

        assertNull(PresetManager.getActivePreset())
    }
}
