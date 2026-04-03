package com.example.gpstick.service

import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.GpsPreset
import com.example.gpstick.data.preset.InMemoryPresetRepository
import com.example.gpstick.data.preset.LocationPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SimulationCoordinatorTest {
    @Test
    fun startsAndStopsPlaceholderManagers() {
        val repository = InMemoryPresetRepository(
            initialPresets = listOf(
                LocationPreset(
                    id = "demo",
                    name = "Demo",
                    summary = "Demo preset",
                    gps = GpsPreset(1.0, 2.0, 3.0, 4f),
                    wifiNetworks = emptyList(),
                    cellTowers = emptyList(),
                )
            )
        )
        val coordinator = SimulationCoordinator(
            presetRepository = repository,
            gpsHookManager = GpsHookManager(),
            wifiHookManager = WifiHookManager(),
            cellHookManager = CellHookManager(),
        )

        val preset = coordinator.start("demo")

        assertNotNull(preset)
        assertEquals(ServiceState.Running, coordinator.currentState())

        coordinator.stop()

        assertEquals(ServiceState.Stopped, coordinator.currentState())
    }
}
