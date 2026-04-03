package com.example.gpstick.service

import android.content.Context
import android.content.Intent
import com.example.gpstick.core.gps.GpsMockService

object ServiceCommandFactory {
    const val ACTION_START_SIMULATION = "com.example.gpstick.action.START_SIMULATION"
    const val ACTION_STOP_SIMULATION = "com.example.gpstick.action.STOP_SIMULATION"
    const val ACTION_START_GPS_MOCK = "com.example.gpstick.action.START_GPS_MOCK"
    const val ACTION_STOP_GPS_MOCK = "com.example.gpstick.action.STOP_GPS_MOCK"
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

    fun startGpsMock(context: Context): Intent =
        Intent(context, GpsMockService::class.java).apply {
            action = ACTION_START_GPS_MOCK
        }

    fun stopGpsMock(context: Context): Intent =
        Intent(context, GpsMockService::class.java).apply {
            action = ACTION_STOP_GPS_MOCK
        }
}
