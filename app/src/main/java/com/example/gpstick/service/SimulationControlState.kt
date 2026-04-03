package com.example.gpstick.service

data class SimulationControlState(
    val isRunning: Boolean = false,
    val activePresetId: String? = null,
)
