package io.github.ianlyoo.latebris.core.gps

import io.github.ianlyoo.latebris.data.preset.LocationPreset
import io.github.ianlyoo.latebris.data.preset.PresetManager

data class GpsSimulationState(
    val activePresetId: String? = null,
    val lastUpdateSummary: String = "Idle",
)

class GpsHookManager {
    private var state = GpsSimulationState()

    fun applyPreset(preset: LocationPreset) {
        PresetManager.activatePreset(preset)
        state = GpsSimulationState(
            activePresetId = preset.id,
            lastUpdateSummary = "Prepared mock GPS payload for ${preset.gps.latitude}, ${preset.gps.longitude}, ${preset.gps.altitude}",
        )
    }

    fun clear() {
        PresetManager.clearActivePreset()
        state = GpsSimulationState()
    }

    fun currentState(): GpsSimulationState = state
}
