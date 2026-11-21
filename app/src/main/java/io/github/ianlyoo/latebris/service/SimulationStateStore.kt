package io.github.ianlyoo.latebris.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SimulationStateStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences = appContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in STATE_KEY_SET) {
            _state.value = load()
        }
    }

    private val _state = MutableStateFlow(readFromPreferences())
    val state: StateFlow<SimulationControlState> = _state.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun load(): SimulationControlState {
        val snapshot = readFromPreferences()
        _state.value = snapshot
        return snapshot
    }

    fun setSimulationActive(
        activePresetId: String,
        sessionId: String,
        heartbeatAtMillis: Long = System.currentTimeMillis(),
    ) {
        val current = readFromPreferences()
        val snapshot = current.copy(
            isRunning = true,
            activePresetId = activePresetId,
            activeSettings = current.pendingSettings,
            sessionId = sessionId,
            sessionHeartbeatAtMillis = heartbeatAtMillis,
            failureMessage = null,
            movementSession = MovementSessionState(),
        )
        preferences.edit()
            .putBoolean(KEY_RUNNING, true)
            .putString(KEY_PRESET_ID, activePresetId)
            .putString(KEY_SESSION_ID, sessionId)
            .putLong(KEY_SESSION_HEARTBEAT_AT_MILLIS, heartbeatAtMillis)
            .putBoolean(KEY_ACTIVE_FEATURES_ENABLED, snapshot.activeSettings.featuresEnabled)
            .putBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, snapshot.activeSettings.isGpsMockEnabled)
            .putBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, snapshot.activeSettings.isWifiMockEnabled)
            .putBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, snapshot.activeSettings.isCellMockEnabled)
            .putBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, snapshot.activeSettings.isMovementSimulationEnabled)
            .putString(KEY_MOVEMENT_PHASE, MovementPhase.None.name)
            .remove(KEY_MOVEMENT_ORIGIN_PRESET_ID)
            .remove(KEY_MOVEMENT_DESTINATION_PRESET_ID)
            .remove(KEY_MOVEMENT_TRANSPORT_MODE)
            .putFloat(KEY_MOVEMENT_SPEED_METERS_PER_SECOND, 0f)
            .putFloat(KEY_MOVEMENT_PROGRESS, 0f)
            .putLong(KEY_MOVEMENT_ETA_EPOCH_MILLIS, 0L)
            .remove(KEY_MOVEMENT_CURRENT_LATITUDE)
            .remove(KEY_MOVEMENT_CURRENT_LONGITUDE)
            .remove(KEY_MOVEMENT_CURRENT_ALTITUDE)
            .remove(KEY_LAST_FAILURE_MESSAGE)
            .apply()
        _state.value = snapshot
    }

    fun setSimulationInactive(failureMessage: String? = null) {
        val current = readFromPreferences()
        val pendingSettings = current.pendingSettings
        val nextFailureEventId = if (failureMessage != null) current.failureEventId + 1 else current.failureEventId
        val snapshot = current.copy(
            isRunning = false,
            activePresetId = null,
            activeSettings = pendingSettings.copy(
                featuresEnabled = false,
                isGpsMockEnabled = false,
                isWifiMockEnabled = false,
                isCellMockEnabled = false,
                isMovementSimulationEnabled = false,
            ),
            sessionId = null,
            sessionHeartbeatAtMillis = 0L,
            failureMessage = failureMessage,
            failureEventId = nextFailureEventId,
            movementSession = MovementSessionState(),
        )
        val editor = preferences.edit()
            .putBoolean(KEY_RUNNING, false)
            .remove(KEY_PRESET_ID)
            .remove(KEY_SESSION_ID)
            .putLong(KEY_SESSION_HEARTBEAT_AT_MILLIS, 0L)
            .putBoolean(KEY_ACTIVE_FEATURES_ENABLED, false)
            .putBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, false)
            .putBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, false)
            .putBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, false)
            .putBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, false)
            .putString(KEY_MOVEMENT_PHASE, MovementPhase.None.name)
            .remove(KEY_MOVEMENT_ORIGIN_PRESET_ID)
            .remove(KEY_MOVEMENT_DESTINATION_PRESET_ID)
            .remove(KEY_MOVEMENT_TRANSPORT_MODE)
            .putFloat(KEY_MOVEMENT_SPEED_METERS_PER_SECOND, 0f)
            .putFloat(KEY_MOVEMENT_PROGRESS, 0f)
            .putLong(KEY_MOVEMENT_ETA_EPOCH_MILLIS, 0L)
            .remove(KEY_MOVEMENT_CURRENT_LATITUDE)
            .remove(KEY_MOVEMENT_CURRENT_LONGITUDE)
            .remove(KEY_MOVEMENT_CURRENT_ALTITUDE)

        if (failureMessage != null) {
            editor.putString(KEY_LAST_FAILURE_MESSAGE, failureMessage)
                .putLong(KEY_LAST_FAILURE_EVENT_ID, nextFailureEventId)
        } else {
            editor.remove(KEY_LAST_FAILURE_MESSAGE)
        }

        editor.apply()
        _state.value = snapshot
    }

    fun promotePendingSettingsToActive() {
        val current = readFromPreferences()
        if (!current.isRunning) {
            return
        }

        val snapshot = current.copy(
            activeSettings = current.pendingSettings,
            failureMessage = null,
        )
        preferences.edit()
            .putBoolean(KEY_ACTIVE_FEATURES_ENABLED, snapshot.activeSettings.featuresEnabled)
            .putBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, snapshot.activeSettings.isGpsMockEnabled)
            .putBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, snapshot.activeSettings.isWifiMockEnabled)
            .putBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, snapshot.activeSettings.isCellMockEnabled)
            .putBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, snapshot.activeSettings.isMovementSimulationEnabled)
            .remove(KEY_LAST_FAILURE_MESSAGE)
            .apply()
        _state.value = snapshot
    }

    fun setMovementRouting(
        originPresetId: String,
        destinationPresetId: String,
        transportMode: MovementTransportMode,
        speedMetersPerSecond: Double,
        currentLatitude: Double,
        currentLongitude: Double,
        currentAltitude: Double,
    ) {
        val current = readFromPreferences()
        if (!current.isRunning) {
            return
        }

        val movementSession = MovementSessionState(
            phase = MovementPhase.Routing,
            originPresetId = originPresetId,
            destinationPresetId = destinationPresetId,
            transportMode = transportMode,
            speedMetersPerSecond = speedMetersPerSecond,
            progress = 0.0,
            etaEpochMillis = 0L,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            currentAltitude = currentAltitude,
        )
        val snapshot = current.copy(
            movementSession = movementSession,
            failureMessage = null,
        )
        persistMovementSession(movementSession)
        _state.value = snapshot
    }

    fun setMovementInProgress(
        etaEpochMillis: Long,
        currentLatitude: Double,
        currentLongitude: Double,
        currentAltitude: Double,
    ) {
        val current = readFromPreferences()
        if (!current.isRunning || !current.movementSession.isInFlight) {
            return
        }

        val movementSession = current.movementSession.copy(
            phase = MovementPhase.Moving,
            etaEpochMillis = etaEpochMillis,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            currentAltitude = currentAltitude,
        )
        val snapshot = current.copy(movementSession = movementSession)
        persistMovementSession(movementSession)
        _state.value = snapshot
    }

    fun updateMovementProgress(
        progress: Double,
        etaEpochMillis: Long,
        currentLatitude: Double,
        currentLongitude: Double,
        currentAltitude: Double,
    ) {
        val current = readFromPreferences()
        if (!current.isRunning || !current.movementSession.isInFlight) {
            return
        }

        val movementSession = current.movementSession.copy(
            phase = MovementPhase.Moving,
            progress = progress.coerceIn(0.0, 1.0),
            etaEpochMillis = etaEpochMillis,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            currentAltitude = currentAltitude,
        )
        val snapshot = current.copy(movementSession = movementSession)
        persistMovementSession(movementSession)
        _state.value = snapshot
    }

    fun setMovementArrived(
        destinationPresetId: String,
        currentLatitude: Double,
        currentLongitude: Double,
        currentAltitude: Double,
    ) {
        val current = readFromPreferences()
        if (!current.isRunning) {
            return
        }

        val movementSession = current.movementSession.copy(
            phase = MovementPhase.Arrived,
            progress = 1.0,
            etaEpochMillis = 0L,
            destinationPresetId = destinationPresetId,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            currentAltitude = currentAltitude,
        )
        val snapshot = current.copy(
            activePresetId = destinationPresetId,
            movementSession = movementSession,
        )

        preferences.edit()
            .putString(KEY_PRESET_ID, destinationPresetId)
            .apply()
        persistMovementSession(movementSession)
        _state.value = snapshot
    }

    fun setMovementCanceledAt(
        currentLatitude: Double,
        currentLongitude: Double,
        currentAltitude: Double,
    ) {
        val current = readFromPreferences()
        if (!current.isRunning) {
            return
        }

        val movementSession = current.movementSession.copy(
            phase = MovementPhase.Canceled,
            etaEpochMillis = 0L,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            currentAltitude = currentAltitude,
        )
        val snapshot = current.copy(movementSession = movementSession)
        persistMovementSession(movementSession)
        _state.value = snapshot
    }

    fun clearMovementSession() {
        val current = readFromPreferences()
        if (!current.isRunning) {
            return
        }

        val snapshot = current.copy(movementSession = MovementSessionState())
        persistMovementSession(snapshot.movementSession)
        _state.value = snapshot
    }

    fun updateSessionHeartbeat(
        sessionId: String,
        heartbeatAtMillis: Long = System.currentTimeMillis(),
    ) {
        val current = readFromPreferences()
        if (!current.isRunning || current.sessionId != sessionId) {
            return
        }

        preferences.edit()
            .putLong(KEY_SESSION_HEARTBEAT_AT_MILLIS, heartbeatAtMillis)
            .apply()
        _state.value = load()
    }

    fun invalidateStaleRunningState(
        nowMillis: Long = System.currentTimeMillis(),
        timeoutMillis: Long = SESSION_HEARTBEAT_TIMEOUT_MILLIS,
    ): SimulationControlState {
        val snapshot = load()
        if (!snapshot.isRunning) {
            return snapshot
        }

        val heartbeatAtMillis = snapshot.sessionHeartbeatAtMillis
        val isStale = heartbeatAtMillis <= 0L || nowMillis - heartbeatAtMillis > timeoutMillis
        if (!isStale) {
            return snapshot
        }

        setSimulationInactive(
            failureMessage = "Simulation session expired because the background runtime stopped responding.",
        )
        return load()
    }

    fun setFeaturesEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PENDING_FEATURES_ENABLED, enabled).apply()
        _state.value = load()
    }

    fun setGpsMockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PENDING_GPS_MOCK_ENABLED, enabled).apply()
        _state.value = load()
    }

    fun setWifiMockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PENDING_WIFI_MOCK_ENABLED, enabled).apply()
        _state.value = load()
    }

    fun setCellMockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PENDING_CELL_MOCK_ENABLED, enabled).apply()
        _state.value = load()
    }

    fun setMovementSimulationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PENDING_MOVEMENT_SIMULATION_ENABLED, enabled).apply()
        _state.value = load()
    }

    private fun readFromPreferences(): SimulationControlState {
        val pendingSettings = SimulationFeatureSettings(
            featuresEnabled = preferences.getBoolean(KEY_PENDING_FEATURES_ENABLED, true),
            isGpsMockEnabled = preferences.getBoolean(KEY_PENDING_GPS_MOCK_ENABLED, true),
            isWifiMockEnabled = preferences.getBoolean(KEY_PENDING_WIFI_MOCK_ENABLED, true),
            isCellMockEnabled = preferences.getBoolean(KEY_PENDING_CELL_MOCK_ENABLED, true),
            isMovementSimulationEnabled = preferences.getBoolean(KEY_PENDING_MOVEMENT_SIMULATION_ENABLED, false),
        )
        val activeSettings = SimulationFeatureSettings(
            featuresEnabled = preferences.getBoolean(KEY_ACTIVE_FEATURES_ENABLED, pendingSettings.featuresEnabled),
            isGpsMockEnabled = preferences.getBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, pendingSettings.isGpsMockEnabled),
            isWifiMockEnabled = preferences.getBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, pendingSettings.isWifiMockEnabled),
            isCellMockEnabled = preferences.getBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, pendingSettings.isCellMockEnabled),
            isMovementSimulationEnabled = preferences.getBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, pendingSettings.isMovementSimulationEnabled),
        )

        return SimulationControlState(
            isRunning = preferences.getBoolean(KEY_RUNNING, false),
            activePresetId = preferences.getString(KEY_PRESET_ID, null),
            pendingSettings = pendingSettings,
            activeSettings = activeSettings,
            sessionId = preferences.getString(KEY_SESSION_ID, null),
            sessionHeartbeatAtMillis = preferences.getLong(KEY_SESSION_HEARTBEAT_AT_MILLIS, 0L),
            failureMessage = preferences.getString(KEY_LAST_FAILURE_MESSAGE, null),
            failureEventId = preferences.getLong(KEY_LAST_FAILURE_EVENT_ID, 0L),
            movementSession = MovementSessionState(
                phase = MovementPhase.entries.firstOrNull {
                    it.name == preferences.getString(KEY_MOVEMENT_PHASE, MovementPhase.None.name)
                } ?: MovementPhase.None,
                originPresetId = preferences.getString(KEY_MOVEMENT_ORIGIN_PRESET_ID, null),
                destinationPresetId = preferences.getString(KEY_MOVEMENT_DESTINATION_PRESET_ID, null),
                transportMode = MovementTransportMode.entries.firstOrNull {
                    it.name == preferences.getString(KEY_MOVEMENT_TRANSPORT_MODE, null)
                },
                speedMetersPerSecond = preferences.getFloat(KEY_MOVEMENT_SPEED_METERS_PER_SECOND, 0f).toDouble(),
                progress = preferences.getFloat(KEY_MOVEMENT_PROGRESS, 0f).toDouble(),
                etaEpochMillis = preferences.getLong(KEY_MOVEMENT_ETA_EPOCH_MILLIS, 0L),
                currentLatitude = preferences.takeIf { it.contains(KEY_MOVEMENT_CURRENT_LATITUDE) }
                    ?.getLong(KEY_MOVEMENT_CURRENT_LATITUDE, 0L)
                    ?.let(Double::fromBits),
                currentLongitude = preferences.takeIf { it.contains(KEY_MOVEMENT_CURRENT_LONGITUDE) }
                    ?.getLong(KEY_MOVEMENT_CURRENT_LONGITUDE, 0L)
                    ?.let(Double::fromBits),
                currentAltitude = preferences.takeIf { it.contains(KEY_MOVEMENT_CURRENT_ALTITUDE) }
                    ?.getLong(KEY_MOVEMENT_CURRENT_ALTITUDE, 0L)
                    ?.let(Double::fromBits),
            ),
        )
    }

    private fun persistMovementSession(session: MovementSessionState) {
        val editor = preferences.edit()
            .putString(KEY_MOVEMENT_PHASE, session.phase.name)
            .putString(KEY_MOVEMENT_ORIGIN_PRESET_ID, session.originPresetId)
            .putString(KEY_MOVEMENT_DESTINATION_PRESET_ID, session.destinationPresetId)
            .putString(KEY_MOVEMENT_TRANSPORT_MODE, session.transportMode?.name)
            .putFloat(KEY_MOVEMENT_SPEED_METERS_PER_SECOND, session.speedMetersPerSecond.toFloat())
            .putFloat(KEY_MOVEMENT_PROGRESS, session.progress.toFloat())
            .putLong(KEY_MOVEMENT_ETA_EPOCH_MILLIS, session.etaEpochMillis)

        if (session.currentLatitude != null) {
            editor.putLong(KEY_MOVEMENT_CURRENT_LATITUDE, session.currentLatitude.toRawBits())
        } else {
            editor.remove(KEY_MOVEMENT_CURRENT_LATITUDE)
        }
        if (session.currentLongitude != null) {
            editor.putLong(KEY_MOVEMENT_CURRENT_LONGITUDE, session.currentLongitude.toRawBits())
        } else {
            editor.remove(KEY_MOVEMENT_CURRENT_LONGITUDE)
        }
        if (session.currentAltitude != null) {
            editor.putLong(KEY_MOVEMENT_CURRENT_ALTITUDE, session.currentAltitude.toRawBits())
        } else {
            editor.remove(KEY_MOVEMENT_CURRENT_ALTITUDE)
        }

        editor.apply()
    }

    companion object {
        @Volatile
        private var instance: SimulationStateStore? = null

        const val PREF_NAME = "simulation_control_state"
        const val KEY_RUNNING = "simulation_running"
        const val KEY_PRESET_ID = "active_preset_id"
        const val KEY_PENDING_FEATURES_ENABLED = "pending_features_enabled"
        const val KEY_PENDING_GPS_MOCK_ENABLED = "pending_gps_mock_enabled"
        const val KEY_PENDING_WIFI_MOCK_ENABLED = "pending_wifi_mock_enabled"
        const val KEY_PENDING_CELL_MOCK_ENABLED = "pending_cell_mock_enabled"
        const val KEY_PENDING_MOVEMENT_SIMULATION_ENABLED = "pending_movement_simulation_enabled"
        const val KEY_ACTIVE_FEATURES_ENABLED = "active_features_enabled"
        const val KEY_ACTIVE_GPS_MOCK_ENABLED = "active_gps_mock_enabled"
        const val KEY_ACTIVE_WIFI_MOCK_ENABLED = "active_wifi_mock_enabled"
        const val KEY_ACTIVE_CELL_MOCK_ENABLED = "active_cell_mock_enabled"
        const val KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED = "active_movement_simulation_enabled"
        const val KEY_SESSION_ID = "simulation_session_id"
        const val KEY_SESSION_HEARTBEAT_AT_MILLIS = "simulation_session_heartbeat_at_millis"
        const val KEY_LAST_FAILURE_MESSAGE = "last_failure_message"
        const val KEY_LAST_FAILURE_EVENT_ID = "last_failure_event_id"
        const val KEY_MOVEMENT_PHASE = "movement_phase"
        const val KEY_MOVEMENT_ORIGIN_PRESET_ID = "movement_origin_preset_id"
        const val KEY_MOVEMENT_DESTINATION_PRESET_ID = "movement_destination_preset_id"
        const val KEY_MOVEMENT_TRANSPORT_MODE = "movement_transport_mode"
        const val KEY_MOVEMENT_SPEED_METERS_PER_SECOND = "movement_speed_meters_per_second"
        const val KEY_MOVEMENT_PROGRESS = "movement_progress"
        const val KEY_MOVEMENT_ETA_EPOCH_MILLIS = "movement_eta_epoch_millis"
        // Stored as the raw bits of a Double; a Float loses about 1 m at these magnitudes.
        const val KEY_MOVEMENT_CURRENT_LATITUDE = "movement_current_latitude_bits"
        const val KEY_MOVEMENT_CURRENT_LONGITUDE = "movement_current_longitude_bits"
        const val KEY_MOVEMENT_CURRENT_ALTITUDE = "movement_current_altitude_bits"
        const val SESSION_HEARTBEAT_TIMEOUT_MILLIS = 10_000L

        val STATE_KEY_SET = setOf(
            KEY_RUNNING,
            KEY_PRESET_ID,
            KEY_PENDING_FEATURES_ENABLED,
            KEY_PENDING_GPS_MOCK_ENABLED,
            KEY_PENDING_WIFI_MOCK_ENABLED,
            KEY_PENDING_CELL_MOCK_ENABLED,
            KEY_PENDING_MOVEMENT_SIMULATION_ENABLED,
            KEY_ACTIVE_FEATURES_ENABLED,
            KEY_ACTIVE_GPS_MOCK_ENABLED,
            KEY_ACTIVE_WIFI_MOCK_ENABLED,
            KEY_ACTIVE_CELL_MOCK_ENABLED,
            KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED,
            KEY_SESSION_ID,
            KEY_SESSION_HEARTBEAT_AT_MILLIS,
            KEY_LAST_FAILURE_MESSAGE,
            KEY_LAST_FAILURE_EVENT_ID,
            KEY_MOVEMENT_PHASE,
            KEY_MOVEMENT_ORIGIN_PRESET_ID,
            KEY_MOVEMENT_DESTINATION_PRESET_ID,
            KEY_MOVEMENT_TRANSPORT_MODE,
            KEY_MOVEMENT_SPEED_METERS_PER_SECOND,
            KEY_MOVEMENT_PROGRESS,
            KEY_MOVEMENT_ETA_EPOCH_MILLIS,
            KEY_MOVEMENT_CURRENT_LATITUDE,
            KEY_MOVEMENT_CURRENT_LONGITUDE,
            KEY_MOVEMENT_CURRENT_ALTITUDE,
        )

        fun getInstance(context: Context): SimulationStateStore =
            instance ?: synchronized(this) {
                instance ?: SimulationStateStore(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
