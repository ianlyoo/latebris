package com.example.gpstick.core.cell

import com.example.gpstick.data.preset.LocationPreset

data class CellSimulationState(
    val activePresetId: String? = null,
    val cellCount: Int = 0,
)

class CellHookManager {
    private var state = CellSimulationState()

    fun applyPreset(preset: LocationPreset) {
        state = CellSimulationState(
            activePresetId = preset.id,
            cellCount = preset.cellTowers.size,
        )
    }

    fun clear() {
        state = CellSimulationState()
    }

    fun currentState(): CellSimulationState = state
}
