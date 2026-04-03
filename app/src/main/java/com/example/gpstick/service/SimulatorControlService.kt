package com.example.gpstick.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.FilePresetRepository
import com.example.gpstick.data.preset.PresetManager

class SimulatorControlService : Service() {
    private lateinit var simulationCoordinator: SimulationCoordinator
    private lateinit var stateStore: SimulationStateStore
    private lateinit var notificationFactory: SimulationNotificationFactory

    override fun onCreate() {
        super.onCreate()
        stateStore = SimulationStateStore.getInstance(this)
        notificationFactory = SimulationNotificationFactory(this)
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
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleStart(intent: Intent, startId: Int) {
        if (!RuntimePermissionGate.hasRequiredSimulationPermissions(this)) {
            stateStore.setSimulationInactive()
            stopSelf(startId)
            return
        }

        val simulationState = stateStore.load()
        if (!simulationState.hasAnyMockFeatureEnabled) {
            handleStop(startId)
            return
        }

        val presetId = intent.getStringExtra(ServiceCommandFactory.EXTRA_PRESET_ID)
        val preset = presetId?.let(simulationCoordinator::start)
        if (preset == null) {
            handleStop(startId)
            return
        }

        PresetManager.activatePreset(preset)
        stateStore.setSimulationActive(preset.id)
        val appliedState = stateStore.load()

        startForeground(
            SimulationNotificationFactory.CONTROL_NOTIFICATION_ID,
            notificationFactory.buildControlNotification(preset, appliedState),
        )

        if (appliedState.activeGpsMockEnabled) {
            startForegroundService(ServiceCommandFactory.startGpsMock(this))
        }
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
            handleStop(startId)
            return
        }

        PresetManager.activatePreset(preset)
        startForeground(
            SimulationNotificationFactory.CONTROL_NOTIFICATION_ID,
            notificationFactory.buildControlNotification(preset, simulationState),
        )

        if (simulationState.activeGpsMockEnabled) {
            startForegroundService(ServiceCommandFactory.startGpsMock(this))
        }
    }

    private fun handleStop(startId: Int) {
        startService(ServiceCommandFactory.stopGpsMock(this))
        simulationCoordinator.stop()
        PresetManager.clearActivePreset()
        stateStore.setSimulationInactive()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }
}
