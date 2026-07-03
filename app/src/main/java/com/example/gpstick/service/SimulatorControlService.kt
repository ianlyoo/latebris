package com.example.gpstick.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.gpstick.core.cell.CellHookManager
import com.example.gpstick.core.gps.GpsHookManager
import com.example.gpstick.core.gps.GpsMockRunner
import com.example.gpstick.core.gps.MovementProgressSnapshot
import com.example.gpstick.core.gps.MovementRouteSpec
import com.example.gpstick.core.wifi.WifiHookManager
import com.example.gpstick.data.preset.FilePresetRepository
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetManager
import com.example.gpstick.service.routing.OpenRouteServiceClient
import com.example.gpstick.service.routing.RoutePoint
import com.google.gson.Gson
import java.util.UUID

class SimulatorControlService : Service() {
    private lateinit var simulationCoordinator: SimulationCoordinator
    private lateinit var stateStore: SimulationStateStore
    private lateinit var notificationFactory: SimulationNotificationFactory
    private lateinit var gpsMockRunner: GpsMockRunner
    private lateinit var presetRepository: FilePresetRepository
    private lateinit var routeClient: OpenRouteServiceClient
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var activeSessionId: String? = null
    private var explicitShutdown = false
    private var movementRequestToken: Long = 0L
    private var runtimeOriginPresetId: String? = null
    private val gson = Gson()

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
        presetRepository = FilePresetRepository(this)
        routeClient = OpenRouteServiceClient()
        gpsMockRunner = GpsMockRunner(
            context = this,
            onRuntimeFailure = {
                stopSimulation(failureMessage = "Simulation stopped because GPS mocking failed.")
            },
            onMovementProgress = ::handleMovementProgress,
            onMovementArrived = ::handleMovementArrived,
        )
        notificationFactory.ensureChannel()
        simulationCoordinator = SimulationCoordinator(
            presetRepository = presetRepository,
            gpsHookManager = GpsHookManager(),
            wifiHookManager = WifiHookManager(),
            cellHookManager = CellHookManager(),
        )
        val initialState = stateStore.load()
        runtimeOriginPresetId = initialState.activePresetId?.takeIf(::isRuntimeOriginPresetId)
        if (!initialState.isRunning) {
            cleanupRuntimeOriginPresets()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceCommandFactory.ACTION_START_SIMULATION -> handleStart(intent, startId)
            ServiceCommandFactory.ACTION_STOP_SIMULATION -> handleStop(startId)
            ServiceCommandFactory.ACTION_APPLY_PENDING_OPTIONS -> handleApplyPendingOptions(startId)
            ServiceCommandFactory.ACTION_START_MOVEMENT -> handleStartMovement(intent)
            ServiceCommandFactory.ACTION_START_MOVEMENT_FROM_CURRENT_LOCATION -> handleStartMovementFromCurrentLocation(intent, startId)
            ServiceCommandFactory.ACTION_CANCEL_MOVEMENT -> handleCancelMovement()
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
        val preset = presetId?.let(presetRepository::getPreset)
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
        val preset = presetId?.let(presetRepository::getPreset)
        if (preset == null) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation state could not be restored because the active preset was missing.",
            )
            return
        }
        runtimeOriginPresetId = preset.id.takeIf(::isRuntimeOriginPresetId)

        val restoredMovement = simulationState.movementSession
        startSession(
            preset = preset,
            settings = simulationState.activeSettings,
            startId = startId,
            sessionId = simulationState.sessionId ?: UUID.randomUUID().toString(),
        )

        if (restoredMovement.isInFlight) {
            val latitude = restoredMovement.currentLatitude ?: preset.gps.latitude
            val longitude = restoredMovement.currentLongitude ?: preset.gps.longitude
            val altitude = restoredMovement.currentAltitude ?: preset.gps.altitude
            stateStore.setMovementCanceledAt(
                currentLatitude = latitude,
                currentLongitude = longitude,
                currentAltitude = altitude,
            )
        }
    }

    private fun handleStop(startId: Int) {
        stopSimulation(startId)
    }

    private fun handleApplyPendingOptions(startId: Int) {
        val simulationState = stateStore.load()
        if (!simulationState.isRunning) {
            return
        }

        val pendingSettings = simulationState.pendingSettings
        if (!pendingSettings.hasAnyMockFeatureEnabled) {
            return
        }

        if (simulationState.movementSession.isInFlight) {
            if (!pendingSettings.featuresEnabled || !pendingSettings.isGpsMockEnabled) {
                val lastCoordinate = gpsMockRunner.cancelRoutePlayback() ?: gpsMockRunner.currentCoordinateOrNull()
                if (lastCoordinate != null) {
                    stateStore.setMovementCanceledAt(
                        currentLatitude = lastCoordinate.latitude,
                        currentLongitude = lastCoordinate.longitude,
                        currentAltitude = lastCoordinate.altitude,
                    )
                }
                gpsMockRunner.stop()
            } else {
                gpsMockRunner.updateSettings(pendingSettings)
            }
            stateStore.promotePendingSettingsToActive()
            return
        }

        val presetId = simulationState.activePresetId
        val preset = presetId?.let(simulationCoordinator::start)
        if (preset == null) {
            stopSimulation(
                startId = startId,
                failureMessage = "Simulation could not apply pending options because the active preset was not found.",
            )
            return
        }

        PresetManager.activatePreset(preset)
        if (pendingSettings.featuresEnabled && pendingSettings.isGpsMockEnabled) {
            val gpsStarted = gpsMockRunner.start(
                preset = preset,
                settings = pendingSettings,
            )
            if (!gpsStarted) {
                stopSimulation(
                    startId = startId,
                    failureMessage = "Simulation stopped because pending options could not be applied to GPS mocking.",
                )
                return
            }
        } else {
            gpsMockRunner.stop()
        }

        stateStore.promotePendingSettingsToActive()
    }

    private fun handleStartMovement(intent: Intent) {
        val simulationState = stateStore.load()
        if (!simulationState.isRunning || !simulationState.activeFeaturesEnabled || !simulationState.activeGpsMockEnabled) {
            return
        }

        val originPresetId = intent.getStringExtra(ServiceCommandFactory.EXTRA_ORIGIN_PRESET_ID) ?: return
        val destinationPresetId = intent.getStringExtra(ServiceCommandFactory.EXTRA_DESTINATION_PRESET_ID) ?: return
        val transportMode = intent.getStringExtra(ServiceCommandFactory.EXTRA_TRANSPORT_MODE)
            ?.let { raw -> MovementTransportMode.entries.firstOrNull { it.name == raw } }
            ?: return
        val speedMetersPerSecond = intent.getDoubleExtra(ServiceCommandFactory.EXTRA_SPEED_METERS_PER_SECOND, 0.0)

        val originPreset = presetRepository.getPreset(originPresetId) ?: return
        val destinationPreset = presetRepository.getPreset(destinationPresetId) ?: return
        val currentCoordinate = gpsMockRunner.currentCoordinateOrNull() ?: RoutePoint(
            latitude = originPreset.gps.latitude,
            longitude = originPreset.gps.longitude,
            altitude = originPreset.gps.altitude,
        )

        beginMovementRoute(
            originPresetId = originPresetId,
            destinationPreset = destinationPreset,
            transportMode = transportMode,
            speedMetersPerSecond = speedMetersPerSecond,
            currentCoordinate = currentCoordinate,
        )
    }

    private fun handleStartMovementFromCurrentLocation(intent: Intent, startId: Int) {
        val simulationState = stateStore.load()
        if (simulationState.isRunning) {
            return
        }
        if (!RuntimePermissionGate.hasRequiredSimulationPermissions(this)) {
            stopSimulation(
                startId = startId,
                failureMessage = "Movement could not start because required permissions are missing.",
            )
            return
        }

        val pendingSettings = simulationState.pendingSettings
        if (!pendingSettings.featuresEnabled || !pendingSettings.isGpsMockEnabled) {
            stopSimulation(
                startId = startId,
                failureMessage = "Movement needs simulation features and GPS mock enabled before it can auto-start.",
            )
            return
        }

        val originPreset = intent.getStringExtra(ServiceCommandFactory.EXTRA_ORIGIN_PRESET_JSON)
            ?.let(::decodePreset)
            ?: run {
                stopSimulation(
                    startId = startId,
                    failureMessage = "Movement could not start because the current device location was unavailable.",
                )
                return
            }

        val destinationPresetId = intent.getStringExtra(ServiceCommandFactory.EXTRA_DESTINATION_PRESET_ID) ?: return
        val destinationPreset = presetRepository.getPreset(destinationPresetId) ?: return
        val transportMode = intent.getStringExtra(ServiceCommandFactory.EXTRA_TRANSPORT_MODE)
            ?.let { raw -> MovementTransportMode.entries.firstOrNull { it.name == raw } }
            ?: return
        val speedMetersPerSecond = intent.getDoubleExtra(ServiceCommandFactory.EXTRA_SPEED_METERS_PER_SECOND, 0.0)

        cleanupRuntimeOriginPresets(exceptId = originPreset.id)
        presetRepository.upsertPreset(originPreset)
        runtimeOriginPresetId = originPreset.id.takeIf(::isRuntimeOriginPresetId)

        startSession(
            preset = originPreset,
            settings = pendingSettings,
            startId = startId,
            sessionId = UUID.randomUUID().toString(),
        )

        if (!stateStore.load().isRunning) {
            return
        }

        beginMovementRoute(
            originPresetId = originPreset.id,
            destinationPreset = destinationPreset,
            transportMode = transportMode,
            speedMetersPerSecond = speedMetersPerSecond,
            currentCoordinate = RoutePoint(
                latitude = originPreset.gps.latitude,
                longitude = originPreset.gps.longitude,
                altitude = originPreset.gps.altitude,
            ),
        )
    }

    private fun beginMovementRoute(
        originPresetId: String,
        destinationPreset: LocationPreset,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
        currentCoordinate: RoutePoint,
    ) {
        val currentState = stateStore.load()
        if (!currentState.isRunning || !currentState.activeFeaturesEnabled || !currentState.activeGpsMockEnabled) {
            return
        }

        movementRequestToken += 1L
        val requestToken = movementRequestToken
        stateStore.setMovementRouting(
            originPresetId = originPresetId,
            destinationPresetId = destinationPreset.id,
            transportMode = transportMode,
            speedMetersPerSecond = speedMetersPerSecond,
            currentLatitude = currentCoordinate.latitude,
            currentLongitude = currentCoordinate.longitude,
            currentAltitude = currentCoordinate.altitude,
        )

        Thread {
            val route = routeClient.fetchRoute(
                originLatitude = currentCoordinate.latitude,
                originLongitude = currentCoordinate.longitude,
                originAltitude = currentCoordinate.altitude,
                destinationLatitude = destinationPreset.gps.latitude,
                destinationLongitude = destinationPreset.gps.longitude,
                destinationAltitude = destinationPreset.gps.altitude,
                mode = transportMode,
            )

            heartbeatHandler.post {
                if (requestToken != movementRequestToken) {
                    return@post
                }
                val latestState = stateStore.load()
                if (!latestState.isRunning || !latestState.activeFeaturesEnabled || !latestState.activeGpsMockEnabled) {
                    return@post
                }

                val routeResult = route
                if (routeResult == null) {
                    stateStore.clearMovementSession()
                    return@post
                }

                val started = gpsMockRunner.startRoutePlayback(
                    MovementRouteSpec(
                        geometry = routeResult.geometry,
                        speedMetersPerSecond = speedMetersPerSecond,
                        fallbackDurationSeconds = routeResult.durationSeconds,
                    ),
                )
                if (!started) {
                    stateStore.clearMovementSession()
                    return@post
                }

                val etaEpochMillis = resolveEtaEpochMillis(
                    distanceMeters = routeResult.distanceMeters,
                    speedMetersPerSecond = speedMetersPerSecond,
                    fallbackDurationSeconds = routeResult.durationSeconds,
                )
                val firstPoint = routeResult.geometry.firstOrNull() ?: currentCoordinate
                stateStore.setMovementInProgress(
                    etaEpochMillis = etaEpochMillis,
                    currentLatitude = firstPoint.latitude,
                    currentLongitude = firstPoint.longitude,
                    currentAltitude = firstPoint.altitude,
                )
            }
        }.start()
    }

    private fun handleCancelMovement() {
        movementRequestToken += 1L
        val currentState = stateStore.load()
        if (!currentState.isRunning) {
            return
        }

        val lastCoordinate = gpsMockRunner.cancelRoutePlayback() ?: gpsMockRunner.currentCoordinateOrNull() ?: return
        stateStore.setMovementCanceledAt(
            currentLatitude = lastCoordinate.latitude,
            currentLongitude = lastCoordinate.longitude,
            currentAltitude = lastCoordinate.altitude,
        )
    }

    private fun handleMovementProgress(snapshot: MovementProgressSnapshot) {
        stateStore.updateMovementProgress(
            progress = snapshot.progress,
            etaEpochMillis = snapshot.etaEpochMillis,
            currentLatitude = snapshot.latitude,
            currentLongitude = snapshot.longitude,
            currentAltitude = snapshot.altitude,
        )
    }

    private fun handleMovementArrived(snapshot: MovementProgressSnapshot) {
        val currentState = stateStore.load()
        val destinationPresetId = currentState.movementSession.destinationPresetId ?: return
        val destinationPreset = presetRepository.getPreset(destinationPresetId) ?: return

        simulationCoordinator.applyPreset(destinationPreset)
        PresetManager.activatePreset(destinationPreset)
        stateStore.setMovementArrived(
            destinationPresetId = destinationPresetId,
            currentLatitude = snapshot.latitude,
            currentLongitude = snapshot.longitude,
            currentAltitude = snapshot.altitude,
        )
        cleanupRuntimeOriginPresets()
        runtimeOriginPresetId = null

        if (currentState.activeSettings.featuresEnabled && currentState.activeSettings.isGpsMockEnabled) {
            val gpsStarted = gpsMockRunner.start(
                preset = destinationPreset,
                settings = currentState.activeSettings.copy(isMovementSimulationEnabled = false),
            )
            if (!gpsStarted) {
                stopSimulation(failureMessage = "Simulation stopped because destination GPS mocking could not continue.")
            }
        }
    }

    private fun resolveEtaEpochMillis(
        distanceMeters: Double,
        speedMetersPerSecond: Double,
        fallbackDurationSeconds: Double,
    ): Long {
        return when {
            speedMetersPerSecond > 0.0 -> {
                System.currentTimeMillis() + (distanceMeters / speedMetersPerSecond * 1_000.0).toLong()
            }

            fallbackDurationSeconds > 0.0 -> {
                System.currentTimeMillis() + (fallbackDurationSeconds * 1_000.0).toLong()
            }

            else -> 0L
        }
    }

    private fun startSession(
        preset: LocationPreset,
        settings: SimulationFeatureSettings,
        startId: Int,
        sessionId: String,
    ) {
        explicitShutdown = false
        simulationCoordinator.applyPreset(preset)
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
        movementRequestToken += 1L
        stopHeartbeat()
        activeSessionId = null
        gpsMockRunner.stop()
        simulationCoordinator.stop()
        PresetManager.clearActivePreset()
        stateStore.setSimulationInactive(failureMessage = failureMessage)
        cleanupRuntimeOriginPresets()
        runtimeOriginPresetId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatHandler.post(heartbeatRunnable)
    }

    private fun stopHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
    }

    private fun cleanupRuntimeOriginPresets(exceptId: String? = null) {
        presetRepository.getPresets()
            .map(LocationPreset::id)
            .filter { isRuntimeOriginPresetId(it) && it != exceptId }
            .forEach(presetRepository::deletePreset)
    }

    private fun decodePreset(raw: String): LocationPreset? {
        return runCatching {
            gson.fromJson(raw, LocationPreset::class.java)
        }.getOrNull()
    }

    private fun isRuntimeOriginPresetId(id: String): Boolean =
        id.startsWith(ServiceCommandFactory.RUNTIME_ORIGIN_PRESET_ID_PREFIX)

    private companion object {
        const val HEARTBEAT_INTERVAL_MILLIS = 2_000L
    }
}
