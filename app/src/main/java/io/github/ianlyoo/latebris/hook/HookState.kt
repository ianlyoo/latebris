package io.github.ianlyoo.latebris.hook

import com.google.gson.Gson
import io.github.ianlyoo.latebris.data.preset.CellTower
import io.github.ianlyoo.latebris.data.preset.GpsPreset
import io.github.ianlyoo.latebris.data.preset.LocationPreset
import io.github.ianlyoo.latebris.service.MovementPhase
import io.github.ianlyoo.latebris.service.SimulationControlState
import io.github.ianlyoo.latebris.data.preset.WifiNetwork

/**
 * Everything the hooks inside a target app need, as one value.
 *
 * Latebris publishes it (see [io.github.ianlyoo.latebris.service.HookStatePublisher]) to a
 * preferences file that LSPosed makes readable to scoped apps; [HookStateReader] reads it
 * inside the target. A signal whose value is null is left untouched by its hook.
 */
data class HookState(
    val gps: GpsPreset? = null,
    val wifiNetworks: List<WifiNetwork>? = null,
    val cellTowers: List<CellTower>? = null,
    val heartbeatAtMillis: Long = 0L,
) {
    /** False once Latebris stops refreshing the heartbeat, for example after a force-stop. */
    fun isLive(nowMillis: Long): Boolean =
        heartbeatAtMillis > 0L && nowMillis - heartbeatAtMillis <= HEARTBEAT_TIMEOUT_MILLIS

    fun encode(): String = gson.toJson(this)

    companion object {
        const val PREF_NAME = "hook_state"
        const val KEY_STATE = "state"
        const val HEARTBEAT_TIMEOUT_MILLIS = 10_000L

        val INACTIVE = HookState()

        private val gson = Gson()

        fun decode(json: String?): HookState =
            json?.let { runCatching { gson.fromJson(it, HookState::class.java) }.getOrNull() } ?: INACTIVE

        /**
         * What the hooks should report for [state]. While a movement runs, GPS follows the
         * current movement position, the Wi-Fi list is empty and the origin's cells stay.
         */
        fun from(state: SimulationControlState, presetOf: (String) -> LocationPreset?): HookState {
            if (!state.isRunning || !state.activeFeaturesEnabled) {
                return INACTIVE
            }
            val movement = state.movementSession
            val moving = movement.phase in MOVING_PHASES
            val preset = (if (moving) movement.originPresetId?.let(presetOf) else null)
                ?: state.activePresetId?.let(presetOf)
                ?: return INACTIVE
            val gps = if (moving) {
                preset.gps.copy(
                    latitude = movement.currentLatitude ?: preset.gps.latitude,
                    longitude = movement.currentLongitude ?: preset.gps.longitude,
                    altitude = movement.currentAltitude ?: preset.gps.altitude,
                )
            } else {
                preset.gps
            }
            return HookState(
                gps = gps.takeIf { state.activeGpsMockEnabled },
                wifiNetworks = (if (moving) emptyList() else preset.wifiNetworks).takeIf { state.activeWifiMockEnabled },
                cellTowers = preset.cellTowers.takeIf { state.activeCellMockEnabled },
                heartbeatAtMillis = state.sessionHeartbeatAtMillis,
            )
        }

        private val MOVING_PHASES = setOf(MovementPhase.Routing, MovementPhase.Moving, MovementPhase.Canceled)
    }
}
