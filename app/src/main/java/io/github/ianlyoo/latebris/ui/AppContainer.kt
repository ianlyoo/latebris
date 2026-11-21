package io.github.ianlyoo.latebris.ui

import android.content.Context
import io.github.ianlyoo.latebris.core.gps.GpsMockRunner
import io.github.ianlyoo.latebris.data.preset.DeviceStateCaptureRepository
import io.github.ianlyoo.latebris.data.preset.FilePresetRepository
import io.github.ianlyoo.latebris.data.preset.PresetRepository
import io.github.ianlyoo.latebris.service.AndroidForegroundServiceController
import io.github.ianlyoo.latebris.service.ForegroundServiceController
import io.github.ianlyoo.latebris.service.HookStatePublisher
import io.github.ianlyoo.latebris.service.SimulationStateStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val presetRepository: PresetRepository = FilePresetRepository(appContext)
    val deviceStateCaptureRepository: DeviceStateCaptureRepository = DeviceStateCaptureRepository(appContext)
    val simulationStateStore = SimulationStateStore.getInstance(appContext)

    /** Keeps the hook state file that target apps read in step with the simulation. */
    val hookStatePublisher = HookStatePublisher(appContext, simulationStateStore, presetRepository)

    val foregroundServiceController: ForegroundServiceController =
        AndroidForegroundServiceController(appContext)

    fun resetStaleSimulationState() {
        val state = simulationStateStore.invalidateStaleRunningState()
        // A process that died while simulating (crash, force-stop) leaves its test providers
        // registered, and Android keeps serving their last fake fix until they are removed.
        if (!state.isRunning) {
            GpsMockRunner(appContext).stop()
        }
    }
}
