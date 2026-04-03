package com.example.gpstick.service

import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetRepository

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
