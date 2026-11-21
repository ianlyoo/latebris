package io.github.ianlyoo.latebris.ui

import android.content.Context
import io.github.ianlyoo.latebris.core.cell.CellHookManager
import io.github.ianlyoo.latebris.core.gps.GpsHookManager
import io.github.ianlyoo.latebris.core.wifi.WifiHookManager
import io.github.ianlyoo.latebris.data.preset.DeviceStateCaptureRepository
import io.github.ianlyoo.latebris.data.preset.FilePresetRepository
import io.github.ianlyoo.latebris.data.preset.PresetRepository
import io.github.ianlyoo.latebris.service.AndroidForegroundServiceController
import io.github.ianlyoo.latebris.service.ForegroundServiceController
import io.github.ianlyoo.latebris.service.HookStatePublisher
import io.github.ianlyoo.latebris.service.SimulationStateStore
import io.github.ianlyoo.latebris.service.SimulationCoordinator

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

    /** Keeps the hook state file that target apps read in step with the simulation. */
    val hookStatePublisher = HookStatePublisher(appContext, simulationStateStore, presetRepository)

    val foregroundServiceController: ForegroundServiceController =
        AndroidForegroundServiceController(appContext)

    fun resetStaleSimulationState() {
        simulationStateStore.invalidateStaleRunningState()
    }
}
