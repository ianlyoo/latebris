package com.example.gpstick.service

import android.content.Context
import android.content.Intent

object ServiceCommandFactory {
    const val ACTION_START_SIMULATION = "com.example.gpstick.action.START_SIMULATION"
    const val ACTION_STOP_SIMULATION = "com.example.gpstick.action.STOP_SIMULATION"
    const val EXTRA_PRESET_ID = "extra_preset_id"

    fun startSimulation(context: Context, presetId: String): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_START_SIMULATION
            putExtra(EXTRA_PRESET_ID, presetId)
        }

    fun stopSimulation(context: Context): Intent =
        Intent(context, SimulatorControlService::class.java).apply {
            action = ACTION_STOP_SIMULATION
        }
}
