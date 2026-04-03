package com.example.gpstick.service

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
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

    fun setSimulationActive(activePresetId: String) {
        val current = readFromPreferences()
        val snapshot = current.copy(
            isRunning = true,
            activePresetId = activePresetId,
            activeSettings = current.pendingSettings,
        )
        preferences.edit()
            .putBoolean(KEY_RUNNING, true)
            .putString(KEY_PRESET_ID, activePresetId)
            .putBoolean(KEY_ACTIVE_FEATURES_ENABLED, snapshot.activeSettings.featuresEnabled)
            .putBoolean(KEY_ACTIVE_GPS_MOCK_ENABLED, snapshot.activeSettings.isGpsMockEnabled)
            .putBoolean(KEY_ACTIVE_WIFI_MOCK_ENABLED, snapshot.activeSettings.isWifiMockEnabled)
            .putBoolean(KEY_ACTIVE_CELL_MOCK_ENABLED, snapshot.activeSettings.isCellMockEnabled)
            .putBoolean(KEY_ACTIVE_MOVEMENT_SIMULATION_ENABLED, snapshot.activeSettings.isMovementSimulationEnabled)
            .apply()
        _state.value = snapshot
    }

    fun setSimulationInactive() {
        val snapshot = readFromPreferences().copy(
            isRunning = false,
            activePresetId = null,
        )
        preferences.edit()
            .putBoolean(KEY_RUNNING, false)
            .remove(KEY_PRESET_ID)
            .apply()
        _state.value = snapshot
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

    fun asBundle(): Bundle = Bundle().apply {
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
        const val METHOD_GET_STATE = "getSimulationState"
        const val AUTHORITY = "com.example.gpstick.simulation.state"

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
        )

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        fun getInstance(context: Context): SimulationStateStore =
            instance ?: synchronized(this) {
                instance ?: SimulationStateStore(context.applicationContext).also {
                    instance = it
                }
            }

        fun readFromProvider(context: Context): SimulationControlState {
            val bundle = context.contentResolver.call(CONTENT_URI, METHOD_GET_STATE, null, null)
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
    }
}
