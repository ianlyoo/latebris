package com.example.gpstick.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.gps.GpsMockRunner
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.FilePresetRepository
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetManager
import java.util.UUID

class SimulatorControlService : Service() {
    private lateinit var simulationCoordinator: SimulationCoordinator
    private lateinit var stateStore: SimulationStateStore
    private lateinit var notificationFactory: SimulationNotificationFactory
    private lateinit var gpsMockRunner: GpsMockRunner
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var activeSessionId: String? = null
    private var explicitShutdown = false

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            val sessionId = activeSessionId ?: return
            stateStore.updateSessionHeartbeat(sessionId)
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        stateStore = SimulationStateStore.getInstance(this)
        notificationFactory = SimulationNotificationFactory(this)
        gpsMockRunner = GpsMockRunner(this) {
            stopSimulation(failureMessage = "Simulation stopped because GPS mocking failed.")
        }
        notificationFactory.ensureChannel()
        simulationCoordinator = SimulationCoordinator(
            presetRepository = FilePresetRepository(this),
            gpsHookManager = GpsHookManager(),
            wifiHookManager = WifiHookManager(),
            cellHookManager = CellHookManager(),
        )
        stateStore.load()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceCommandFactory.ACTION_START_SIMULATION -> handleStart(intent, startId)
            ServiceCommandFactory.ACTION_STOP_SIMULATION -> handleStop(startId)
            null -> restoreRunningSession(startId)
            else -> stopSimulation(startId, failureMessage = "Simulation command was not recognized.")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopHeartbeat()
        if (stateStore.load().isRunning && !explicitShutdown) {
            terminateSession(failureMessage = "Simulation stopped unexpectedly.")
        } else {
            gpsMockRunner.stop()
        }
        super.onDestroy()
    }

    private fun handleStart(intent: Intent, startId: Int) {
        if (!RuntimePermissionGate.hasRequiredSimulationPermissions(this)) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation could not start because required permissions are missing.",
            )
            return
        }

        val simulationState = stateStore.load()
        if (!simulationState.hasAnyMockFeatureEnabled) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation could not start because no mock features are enabled.",
            )
            return
        }

        val presetId = intent.getStringExtra(ServiceCommandFactory.EXTRA_PRESET_ID)
        val preset = presetId?.let(simulationCoordinator::start)
        if (preset == null) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation could not start because the selected preset was not found.",
            )
            return
        }

        startSession(
            preset = preset,
            settings = simulationState.pendingSettings,
            startId = startId,
            sessionId = UUID.randomUUID().toString(),
        )
    }

    private fun restoreRunningSession(startId: Int) {
        val simulationState = stateStore.load()
        if (!simulationState.isRunning) {
            stopSelf(startId)
            return
        }

        val presetId = simulationState.activePresetId
        val preset = presetId?.let(simulationCoordinator::start)
        if (preset == null) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation state could not be restored because the active preset was missing.",
            )
            return
        }

        startSession(
            preset = preset,
            settings = simulationState.activeSettings,
            startId = startId,
            sessionId = simulationState.sessionId ?: UUID.randomUUID().toString(),
        )
    }

    private fun handleStop(startId: Int) {
        stopSimulation(startId)
    }

    private fun startSession(
        preset: LocationPreset,
        settings: SimulationFeatureSettings,
        startId: Int,
        sessionId: String,
    ) {
        explicitShutdown = false
        PresetManager.activatePreset(preset)

        if (settings.featuresEnabled && settings.isGpsMockEnabled) {
            val gpsStarted = gpsMockRunner.start(
                preset = preset,
                settings = settings,
            )
            if (!gpsStarted) {
                stopSimulation(
                    startId = startId,
                    failureMessage = "Simulation could not start because GPS mocking could not be initialized.",
                )
                return
            }
        }

        val foregroundStarted = runCatching {
            startForeground(
                SimulationNotificationFactory.NOTIFICATION_ID,
                notificationFactory.buildRunningNotification(),
            )
        }.isSuccess
        if (!foregroundStarted) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation could not start because the background service notification failed.",
            )
            return
        }

        activeSessionId = sessionId
        stateStore.setSimulationActive(
            activePresetId = preset.id,
            sessionId = sessionId,
        )
        startHeartbeat()
    }

    private fun stopSimulation(startId: Int? = null, failureMessage: String? = null) {
        explicitShutdown = true
        terminateSession(failureMessage = failureMessage)
        if (startId != null) {
            stopSelf(startId)
        } else {
            stopSelf()
        }
    }

    private fun terminateSession(failureMessage: String? = null) {
        stopHeartbeat()
        activeSessionId = null
        gpsMockRunner.stop()
        simulationCoordinator.stop()
        PresetManager.clearActivePreset()
        stateStore.setSimulationInactive(failureMessage = failureMessage)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatHandler.post(heartbeatRunnable)
    }

    private fun stopHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MILLIS = 2_000L
    }
}
