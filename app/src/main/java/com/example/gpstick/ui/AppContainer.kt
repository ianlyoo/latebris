package com.example.gpstick.ui

import android.content.Context
import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.DeviceStateCaptureRepository
import com.example.gpstick.data.preset.FilePresetRepository
import com.example.gpstick.data.preset.PresetRepository
import com.example.gpstick.service.AndroidForegroundServiceController
import com.example.gpstick.service.ForegroundServiceController
import com.example.gpstick.service.SimulationStateStore
import com.example.gpstick.service.SimulationCoordinator

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val presetRepository: PresetRepository = FilePresetRepository(appContext)
    val deviceStateCaptureRepository: DeviceStateCaptureRepository = DeviceStateCaptureRepository(appContext)
    val simulationStateStore = SimulationStateStore.getInstance(appContext)

    val simulationCoordinator: SimulationCoordinator = SimulationCoordinator(
        presetRepository = presetRepository,
        gpsHookManager = GpsHookManager(),
        wifiHookManager = WifiHookManager(),
        cellHookManager = CellHookManager(),
    )

    val foregroundServiceController: ForegroundServiceController =
        AndroidForegroundServiceController(appContext)

    fun resetStaleSimulationState() {
        simulationStateStore.invalidateStaleRunningState()
    }
}
