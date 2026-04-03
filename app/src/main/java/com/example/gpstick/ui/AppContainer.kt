package com.example.gpstick.ui

import android.app.ActivityManager
import android.content.Context
import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.gps.GpsMockService
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.FilePresetRepository
import com.example.gpstick.data.preset.PresetRepository
import com.example.gpstick.service.AndroidForegroundServiceController
import com.example.gpstick.service.ForegroundServiceController
import com.example.gpstick.service.SimulationStateStore
import com.example.gpstick.service.SimulationCoordinator
import com.example.gpstick.service.SimulatorControlService

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val presetRepository: PresetRepository = FilePresetRepository(appContext)
    val simulationStateStore = SimulationStateStore.getInstance(appContext)

    val simulationCoordinator: SimulationCoordinator = SimulationCoordinator(
        presetRepository = presetRepository,
        gpsHookManager = GpsHookManager(),
        wifiHookManager = WifiHookManager(),
        cellHookManager = CellHookManager(),
    )

    val foregroundServiceController: ForegroundServiceController =
        AndroidForegroundServiceController(appContext)

    @Suppress("DEPRECATION")
    fun resetStaleSimulationState() {
        val isMarkedRunning = simulationStateStore.load().isRunning
        if (!isMarkedRunning) {
            return
        }

        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
        val controlServiceRunning = runningServices.any {
            it.service.className == SimulatorControlService::class.java.name
        }
        val gpsServiceRunning = runningServices.any {
            it.service.className == GpsMockService::class.java.name
        }

        if (!controlServiceRunning && !gpsServiceRunning) {
            simulationStateStore.setSimulationInactive()
        }
    }
}
