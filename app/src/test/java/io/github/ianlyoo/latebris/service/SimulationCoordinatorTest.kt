package io.github.ianlyoo.latebris.service

import io.github.ianlyoo.latebris.core.cell.CellHookManager
import io.github.ianlyoo.latebris.core.gps.GpsHookManager
import io.github.ianlyoo.latebris.core.wifi.WifiHookManager
import io.github.ianlyoo.latebris.data.preset.GpsPreset
import io.github.ianlyoo.latebris.data.preset.InMemoryPresetRepository
import io.github.ianlyoo.latebris.data.preset.LocationPreset
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
