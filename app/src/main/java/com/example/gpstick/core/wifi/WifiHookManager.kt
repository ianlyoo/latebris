package com.example.gpstick.core.wifi

import com.example.gpstick.data.preset.LocationPreset

data class WifiSimulationState(
    val activePresetId: String? = null,
    val accessPointCount: Int = 0,
)

class WifiHookManager {
    private var state = WifiSimulationState()

    fun applyPreset(preset: LocationPreset) {
        state = WifiSimulationState(
            activePresetId = preset.id,
            accessPointCount = preset.wifiNetworks.size,
        )
    }

    fun clear() {
        state = WifiSimulationState()
    }

    fun currentState(): WifiSimulationState = state
}
