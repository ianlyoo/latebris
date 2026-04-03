package com.example.gpstick.service

import androidx.compose.runtime.Immutable
import com.example.gpstick.data.preset.LocationPreset

@Immutable
data class SimulationFeatureSettings(
    val featuresEnabled: Boolean = true,
    val isGpsMockEnabled: Boolean = true,
    val isWifiMockEnabled: Boolean = true,
    val isCellMockEnabled: Boolean = true,
    val isMovementSimulationEnabled: Boolean = false,
) {
    val hasAnyMockFeatureEnabled: Boolean
        get() = featuresEnabled && (isGpsMockEnabled || isWifiMockEnabled || isCellMockEnabled)
}

@Immutable
data class SimulationControlState(
    val isRunning: Boolean = false,
    val activePresetId: String? = null,
    val pendingSettings: SimulationFeatureSettings = SimulationFeatureSettings(),
    val activeSettings: SimulationFeatureSettings = SimulationFeatureSettings(),
) {
    val hasAnyMockFeatureEnabled: Boolean
        get() = pendingSettings.hasAnyMockFeatureEnabled

    val hasAnyActiveMockFeatureEnabled: Boolean
        get() = activeSettings.hasAnyMockFeatureEnabled

    val featuresEnabled: Boolean
        get() = pendingSettings.featuresEnabled

    val isGpsMockEnabled: Boolean
        get() = pendingSettings.isGpsMockEnabled

    val isWifiMockEnabled: Boolean
        get() = pendingSettings.isWifiMockEnabled

    val isCellMockEnabled: Boolean
        get() = pendingSettings.isCellMockEnabled

    val isMovementSimulationEnabled: Boolean
        get() = pendingSettings.isMovementSimulationEnabled

    val activeFeaturesEnabled: Boolean
        get() = activeSettings.featuresEnabled

    val activeGpsMockEnabled: Boolean
        get() = activeSettings.isGpsMockEnabled

    val activeWifiMockEnabled: Boolean
        get() = activeSettings.isWifiMockEnabled

    val activeCellMockEnabled: Boolean
        get() = activeSettings.isCellMockEnabled

    val activeMovementSimulationEnabled: Boolean
        get() = activeSettings.isMovementSimulationEnabled
}

@Immutable
data class SimulationStateSnapshot(
    val controlState: SimulationControlState = SimulationControlState(),
    val activePreset: LocationPreset? = null,
)
