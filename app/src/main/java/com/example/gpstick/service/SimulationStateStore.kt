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
        if (key == KEY_RUNNING || key == KEY_PRESET_ID) {
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
        preferences.edit()
            .putBoolean(KEY_RUNNING, true)
            .putString(KEY_PRESET_ID, activePresetId)
            .apply()
        _state.value = SimulationControlState(isRunning = true, activePresetId = activePresetId)
    }

    fun setSimulationInactive() {
        preferences.edit()
            .putBoolean(KEY_RUNNING, false)
            .remove(KEY_PRESET_ID)
            .apply()
        _state.value = SimulationControlState()
    }

    fun asBundle(): Bundle = Bundle().apply {
        val snapshot = load()
        putBoolean(KEY_RUNNING, snapshot.isRunning)
        putString(KEY_PRESET_ID, snapshot.activePresetId)
    }

    private fun readFromPreferences(): SimulationControlState = SimulationControlState(
        isRunning = preferences.getBoolean(KEY_RUNNING, false),
        activePresetId = preferences.getString(KEY_PRESET_ID, null),
    )

    companion object {
        @Volatile
        private var instance: SimulationStateStore? = null

        const val PREF_NAME = "simulation_control_state"
        const val KEY_RUNNING = "simulation_running"
        const val KEY_PRESET_ID = "active_preset_id"
        const val METHOD_GET_STATE = "getSimulationState"
        const val AUTHORITY = "com.example.gpstick.simulation.state"

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        fun getInstance(context: Context): SimulationStateStore =
            instance ?: synchronized(this) {
                instance ?: SimulationStateStore(context.applicationContext).also {
                    instance = it
                }
            }

        fun readFromProvider(context: Context): SimulationControlState {
            val bundle = context.contentResolver.call(CONTENT_URI, METHOD_GET_STATE, null, null)
            return SimulationControlState(
                isRunning = bundle?.getBoolean(KEY_RUNNING, false) == true,
                activePresetId = bundle?.getString(KEY_PRESET_ID),
            )
        }
    }
}
