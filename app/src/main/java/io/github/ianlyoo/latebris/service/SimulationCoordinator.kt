package io.github.ianlyoo.latebris.service

import io.github.ianlyoo.latebris.core.cell.CellHookManager
import io.github.ianlyoo.latebris.core.gps.GpsHookManager
import io.github.ianlyoo.latebris.core.wifi.WifiHookManager
import io.github.ianlyoo.latebris.data.preset.LocationPreset
import io.github.ianlyoo.latebris.data.preset.PresetRepository

class SimulationCoordinator(
    private val presetRepository: PresetRepository,
    private val gpsHookManager: GpsHookManager,
    private val wifiHookManager: WifiHookManager,
    private val cellHookManager: CellHookManager,
) {
    private var state: ServiceState = ServiceState.Stopped
    private var activePreset: LocationPreset? = null

    fun availablePresets(): List<LocationPreset> = presetRepository.getPresets()

    fun currentState(): ServiceState = state

    fun currentPreset(): LocationPreset? = activePreset

    fun start(presetId: String): LocationPreset? {
        val preset = presetRepository.getPreset(presetId) ?: return null
        return applyPreset(preset)
    }

    fun applyPreset(preset: LocationPreset): LocationPreset {
        gpsHookManager.applyPreset(preset)
        wifiHookManager.applyPreset(preset)
        cellHookManager.applyPreset(preset)
        activePreset = preset
        state = ServiceState.Running
        return preset
    }

    fun stop() {
        gpsHookManager.clear()
        wifiHookManager.clear()
        cellHookManager.clear()
        activePreset = null
        state = ServiceState.Stopped
    }
}
