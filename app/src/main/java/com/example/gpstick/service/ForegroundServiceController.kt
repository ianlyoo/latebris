package com.example.gpstick.service

import android.content.Context
import com.example.gpstick.data.preset.LocationPreset
import com.google.gson.Gson

interface ForegroundServiceController {
    fun start(preset: LocationPreset): Boolean

    fun stop()

    fun applyPendingOptions(): Boolean

    fun startMovement(
        originPresetId: String,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
    ): Boolean

    fun startMovementFromCurrentLocation(
        originPreset: LocationPreset,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
    ): Boolean

    fun cancelMovement(): Boolean
}

class AndroidForegroundServiceController(
    private val context: Context,
) : ForegroundServiceController {
    private val gson = Gson()

    override fun start(preset: LocationPreset): Boolean {
        if (!RuntimePermissionGate.hasRequiredSimulationPermissions(context)) {
            return false
        }

        return try {
            val command = ServiceCommandFactory.startSimulation(context = context, presetId = preset.id)
            context.startForegroundService(command)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun stop() {
        val command = ServiceCommandFactory.stopSimulation(context)
        context.startService(command)
    }

    override fun applyPendingOptions(): Boolean {
        return try {
            val command = ServiceCommandFactory.applyPendingOptions(context)
            context.startService(command)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun startMovement(
        originPresetId: String,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
    ): Boolean {
        return try {
            val command = ServiceCommandFactory.startMovement(
                context = context,
                originPresetId = originPresetId,
                destinationPresetId = destinationPresetId,
                transportMode = transportMode,
                speedMetersPerSecond = speedMetersPerSecond,
            )
            context.startService(command)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun startMovementFromCurrentLocation(
        originPreset: LocationPreset,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
    ): Boolean {
        if (!RuntimePermissionGate.hasRequiredSimulationPermissions(context)) {
            return false
        }

        return try {
            val command = ServiceCommandFactory.startMovementFromCurrentLocation(
                context = context,
                originPresetJson = gson.toJson(originPreset),
                destinationPresetId = destinationPresetId,
                transportMode = transportMode,
                speedMetersPerSecond = speedMetersPerSecond,
            )
            context.startForegroundService(command)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun cancelMovement(): Boolean {
        return try {
            val command = ServiceCommandFactory.cancelMovement(context)
            context.startService(command)
            true
        } catch (_: Exception) {
            false
        }
    }
}
