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

enum class MovementPhase {
    None,
    Routing,
    Moving,
    Arrived,
    Canceled,
}

enum class MovementTransportMode {
    Drive,
    Cycle,
    Walk,
    Transit,
}

@Immutable
data class MovementSessionState(
    val phase: MovementPhase = MovementPhase.None,
    val originPresetId: String? = null,
    val destinationPresetId: String? = null,
    val transportMode: MovementTransportMode? = null,
    val speedMetersPerSecond: Double = 0.0,
    val progress: Double = 0.0,
    val etaEpochMillis: Long = 0L,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val currentAltitude: Double? = null,
) {
    val isInFlight: Boolean
        get() = phase == MovementPhase.Routing || phase == MovementPhase.Moving
}

@Immutable
data class SimulationControlState(
    val isRunning: Boolean = false,
    val activePresetId: String? = null,
    val pendingSettings: SimulationFeatureSettings = SimulationFeatureSettings(),
    val activeSettings: SimulationFeatureSettings = SimulationFeatureSettings(),
    val sessionId: String? = null,
    val sessionHeartbeatAtMillis: Long = 0L,
    val failureMessage: String? = null,
    val failureEventId: Long = 0L,
    val movementSession: MovementSessionState = MovementSessionState(),
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
