package com.example.gpstick.data.preset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InMemoryPresetRepositoryTest {
    private val sample = listOf(
        LocationPreset(
            id = "sample",
            name = "Sample",
            summary = "Summary",
            gps = GpsPreset(1.0, 2.0, 3.0, 3f),
            wifiNetworks = emptyList(),
            cellTowers = emptyList(),
        )
    )

    @Test
    fun returnsSeededPresets() {
        val repository = InMemoryPresetRepository(sample)

        assertEquals(1, repository.getPresets().size)
        assertNotNull(repository.getPreset("sample"))
    }
}
