package com.example.gpstick.service

import android.content.Context
import com.example.gpstick.data.preset.LocationPreset

interface ForegroundServiceController {
    fun start(preset: LocationPreset)

    fun stop()
}

class AndroidForegroundServiceController(
    private val context: Context,
) : ForegroundServiceController {
    override fun start(preset: LocationPreset) {
        val command = ServiceCommandFactory.startSimulation(context = context, presetId = preset.id)
        context.startForegroundService(command)
    }

    override fun stop() {
        val command = ServiceCommandFactory.stopSimulation(context)
        context.startService(command)
    }
}
