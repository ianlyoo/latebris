package com.example.gpstick.service

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import com.example.gpstick.data.preset.LocationPreset
import com.google.gson.Gson
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

        if (failureMessage != null) {
            editor.putString(KEY_LAST_FAILURE_MESSAGE, failureMessage)
                .putLong(KEY_LAST_FAILURE_EVENT_ID, nextFailureEventId)
        } else {
            editor.remove(KEY_LAST_FAILURE_MESSAGE)
        }

        editor.apply()
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

    fun asBundle(activePreset: LocationPreset? = null): Bundle = Bundle().apply {
        val snapshot = load()
        putBoolean(KEY_RUNNING, snapshot.isRunning)
        putString(KEY_PRESET_ID, snapshot.activePresetId)
        putBoolean(KEY_PENDING_FEATURES_ENABLED, snapshot.featuresEnabled)
        putBoolean(KEY_PENDING_GPS_MOCK_ENABLED, snapshot.isGpsMockEnabled)
        putBoolean(KEY_PENDING_WIFI_MOCK_ENABLED, snapshot.isWifiMockEnabled)
        putBoolean(KEY_PENDING_CELL_MOCK_ENABLED, snapshot.isCellMockEnabled)
        putBoolean(KEY_PENDING_MOVEMENT_SIMULATION_ENABLED, snapshot.isMovementSimulationEnabled)
        putBoolean(KEY_ACTIVE_FEATURES_ENABLED, snapshot.activeFeaturesEnabled)
        putBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, snapshot.activeGpsMockEnabled)
        putBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, snapshot.activeWifiMockEnabled)
        putBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, snapshot.activeCellMockEnabled)
        putBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, snapshot.activeMovementSimulationEnabled)
        putString(KEY_ACTIVE_PRESET_JSON, activePreset?.let(gson::toJson))
    }

    fun asProviderBundle(activePreset: LocationPreset? = null): Bundle = Bundle().apply {
        val snapshot = invalidateStaleRunningState()
        putBoolean(KEY_RUNNING, snapshot.isRunning)
        putString(KEY_PRESET_ID, snapshot.activePresetId)
        putBoolean(KEY_ACTIVE_FEATURES_ENABLED, snapshot.activeFeaturesEnabled)
        putBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, snapshot.activeGpsMockEnabled)
        putBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, snapshot.activeWifiMockEnabled)
        putBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, snapshot.activeCellMockEnabled)
        putBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, snapshot.activeMovementSimulationEnabled)
        putString(KEY_ACTIVE_PRESET_JSON, activePreset?.let(gson::toJson))
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
        )
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
        const val KEY_ACTIVE_PRESET_JSON = "active_preset_json"
        const val KEY_SESSION_ID = "simulation_session_id"
        const val KEY_SESSION_HEARTBEAT_AT_MILLIS = "simulation_session_heartbeat_at_millis"
        const val KEY_LAST_FAILURE_MESSAGE = "last_failure_message"
        const val KEY_LAST_FAILURE_EVENT_ID = "last_failure_event_id"
        const val METHOD_GET_STATE = "getSimulationState"
        const val AUTHORITY = "com.example.gpstick.simulation.state"
        const val SESSION_HEARTBEAT_TIMEOUT_MILLIS = 10_000L
        private val gson = Gson()

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
        )

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        fun getInstance(context: Context): SimulationStateStore =
            instance ?: synchronized(this) {
                instance ?: SimulationStateStore(context.applicationContext).also {
                    instance = it
                }
            }

        fun readFromProvider(context: Context): SimulationControlState {
            return readSnapshotFromProvider(context).controlState
        }

        fun readSnapshotFromProvider(context: Context): SimulationStateSnapshot {
            val bundle = runCatching {
                context.contentResolver.call(CONTENT_URI, METHOD_GET_STATE, null, null)
            }.getOrNull()

            val controlState = decodeProviderControlState(bundle)
            val activePreset = bundle?.getString(KEY_ACTIVE_PRESET_JSON)?.let(::decodePreset)
            return SimulationStateSnapshot(
                controlState = controlState,
                activePreset = activePreset,
            )
        }

        private fun decodeControlState(bundle: Bundle?): SimulationControlState {
            val pendingSettings = SimulationFeatureSettings(
                featuresEnabled = bundle?.getBoolean(KEY_PENDING_FEATURES_ENABLED, true) == true,
                isGpsMockEnabled = bundle?.getBoolean(KEY_PENDING_GPS_MOCK_ENABLED, true) == true,
                isWifiMockEnabled = bundle?.getBoolean(KEY_PENDING_WIFI_MOCK_ENABLED, true) == true,
                isCellMockEnabled = bundle?.getBoolean(KEY_PENDING_CELL_MOCK_ENABLED, true) == true,
                isMovementSimulationEnabled = bundle?.getBoolean(KEY_PENDING_MOVEMENT_SIMULATION_ENABLED, false) == true,
            )

            return SimulationControlState(
                isRunning = bundle?.getBoolean(KEY_RUNNING, false) == true,
                activePresetId = bundle?.getString(KEY_PRESET_ID),
                pendingSettings = pendingSettings,
                activeSettings = SimulationFeatureSettings(
                    featuresEnabled = bundle?.getBoolean(KEY_ACTIVE_FEATURES_ENABLED, pendingSettings.featuresEnabled) == true,
                    isGpsMockEnabled = bundle?.getBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, pendingSettings.isGpsMockEnabled) == true,
                    isWifiMockEnabled = bundle?.getBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, pendingSettings.isWifiMockEnabled) == true,
                    isCellMockEnabled = bundle?.getBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, pendingSettings.isCellMockEnabled) == true,
                    isMovementSimulationEnabled = bundle?.getBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, pendingSettings.isMovementSimulationEnabled) == true,
                ),
            )
        }

        private fun decodeProviderControlState(bundle: Bundle?): SimulationControlState {
            val activeSettings = SimulationFeatureSettings(
                featuresEnabled = bundle?.getBoolean(KEY_ACTIVE_FEATURES_ENABLED, false) == true,
                isGpsMockEnabled = bundle?.getBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, false) == true,
                isWifiMockEnabled = bundle?.getBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, false) == true,
                isCellMockEnabled = bundle?.getBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, false) == true,
                isMovementSimulationEnabled = bundle?.getBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, false) == true,
            )

            return SimulationControlState(
                isRunning = bundle?.getBoolean(KEY_RUNNING, false) == true,
                activePresetId = bundle?.getString(KEY_PRESET_ID),
                pendingSettings = activeSettings,
                activeSettings = activeSettings,
            )
        }

        private fun decodePreset(raw: String): LocationPreset? {
            return runCatching {
                gson.fromJson(raw, LocationPreset::class.java)
            }.getOrNull()
        }
    }
}
