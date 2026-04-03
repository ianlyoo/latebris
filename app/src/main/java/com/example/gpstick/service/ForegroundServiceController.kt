package com.example.gpstick.service

import android.content.Context
import com.example.gpstick.data.preset.LocationPreset

interface ForegroundServiceController {
    fun start(preset: LocationPreset): Boolean

    fun stop()
}

class AndroidForegroundServiceController(
    private val context: Context,
) : ForegroundServiceController {
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
}
