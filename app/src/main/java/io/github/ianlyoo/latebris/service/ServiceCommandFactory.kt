package io.github.ianlyoo.latebris.service

import android.content.Context
import android.content.Intent

object ServiceCommandFactory {
    const val ACTION_START_SIMULATION = "io.github.ianlyoo.latebris.action.START_SIMULATION"
    const val ACTION_STOP_SIMULATION = "io.github.ianlyoo.latebris.action.STOP_SIMULATION"
    const val ACTION_APPLY_PENDING_OPTIONS = "io.github.ianlyoo.latebris.action.APPLY_PENDING_OPTIONS"
    const val ACTION_START_MOVEMENT = "io.github.ianlyoo.latebris.action.START_MOVEMENT"
    const val ACTION_START_MOVEMENT_FROM_CURRENT_LOCATION = "io.github.ianlyoo.latebris.action.START_MOVEMENT_FROM_CURRENT_LOCATION"
    const val ACTION_CANCEL_MOVEMENT = "io.github.ianlyoo.latebris.action.CANCEL_MOVEMENT"
    const val EXTRA_PRESET_ID = "extra_preset_id"
    const val EXTRA_ORIGIN_PRESET_ID = "extra_origin_preset_id"
    const val EXTRA_ORIGIN_PRESET_JSON = "extra_origin_preset_json"
    const val EXTRA_DESTINATION_PRESET_ID = "extra_destination_preset_id"
    const val EXTRA_TRANSPORT_MODE = "extra_transport_mode"
    const val EXTRA_SPEED_METERS_PER_SECOND = "extra_speed_meters_per_second"
    const val RUNTIME_ORIGIN_PRESET_ID_PREFIX = "__runtime_origin__:"

    fun startSimulation(context: Context, presetId: String): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_START_SIMULATION
            putExtra(EXTRA_PRESET_ID, presetId)
        }

    fun stopSimulation(context: Context): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_STOP_SIMULATION
        }

    fun applyPendingOptions(context: Context): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_APPLY_PENDING_OPTIONS
        }

    fun startMovement(
        context: Context,
        originPresetId: String,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
    ): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_START_MOVEMENT
            putExtra(EXTRA_ORIGIN_PRESET_ID, originPresetId)
            putExtra(EXTRA_DESTINATION_PRESET_ID, destinationPresetId)
            putExtra(EXTRA_TRANSPORT_MODE, transportMode.name)
            putExtra(EXTRA_SPEED_METERS_PER_SECOND, speedMetersPerSecond)
        }

    fun startMovementFromCurrentLocation(
        context: Context,
        originPresetJson: String,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
    ): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_START_MOVEMENT_FROM_CURRENT_LOCATION
            putExtra(EXTRA_ORIGIN_PRESET_JSON, originPresetJson)
            putExtra(EXTRA_DESTINATION_PRESET_ID, destinationPresetId)
            putExtra(EXTRA_TRANSPORT_MODE, transportMode.name)
            putExtra(EXTRA_SPEED_METERS_PER_SECOND, speedMetersPerSecond)
        }

    fun cancelMovement(context: Context): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_CANCEL_MOVEMENT
        }
}
