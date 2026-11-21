package io.github.ianlyoo.latebris.hook

import io.github.ianlyoo.latebris.data.preset.CellTower
import io.github.ianlyoo.latebris.data.preset.GpsPreset
import io.github.ianlyoo.latebris.data.preset.LocationPreset
import io.github.ianlyoo.latebris.data.preset.WifiNetwork
import io.github.ianlyoo.latebris.service.MovementPhase
import io.github.ianlyoo.latebris.service.MovementSessionState
import io.github.ianlyoo.latebris.service.SimulationControlState
import io.github.ianlyoo.latebris.service.SimulationFeatureSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HookStateTest {
    private val preset = LocationPreset(
        id = "urban-core",
        name = "Urban core",
        summary = "",
        gps = GpsPreset(latitude = 37.5665, longitude = 126.978, altitude = 38.4, accuracyMeters = 12.5f),
        wifiNetworks = listOf(WifiNetwork("00:11:22:33:44:55", "qa-lab-a", -48, 2412)),
        cellTowers = listOf(CellTower(mcc = 450, mnc = 5, ci = 22011, pci = 101, tac = 101, earfcn = 1800)),
    )
    private val presets = mapOf(preset.id to preset)

    private fun running(
        settings: SimulationFeatureSettings = SimulationFeatureSettings(),
        movement: MovementSessionState = MovementSessionState(),
    ) = SimulationControlState(
        isRunning = true,
        activePresetId = preset.id,
        activeSettings = settings,
        sessionHeartbeatAtMillis = 1_000L,
        movementSession = movement,
    )

    @Test
    fun stoppedSimulationPublishesNothing() {
        assertEquals(HookState.INACTIVE, HookState.from(SimulationControlState(), presets::get))
    }

    @Test
    fun masterSwitchOffPublishesNothing() {
        val state = running(SimulationFeatureSettings(featuresEnabled = false))
        assertEquals(HookState.INACTIVE, HookState.from(state, presets::get))
    }

    @Test
    fun runningPresetPublishesAllThreeSignals() {
        val hookState = HookState.from(running(), presets::get)
        assertEquals(preset.gps, hookState.gps)
        assertEquals(preset.wifiNetworks, hookState.wifiNetworks)
        assertEquals(preset.cellTowers, hookState.cellTowers)
        assertEquals(1_000L, hookState.heartbeatAtMillis)
    }

    @Test
    fun disabledSignalIsLeftUntouched() {
        val hookState = HookState.from(running(SimulationFeatureSettings(isWifiMockEnabled = false)), presets::get)
        assertNull(hookState.wifiNetworks)
        assertEquals(preset.gps, hookState.gps)
    }

    @Test
    fun movementFollowsCurrentPositionWithEmptyWifi() {
        val movement = MovementSessionState(
            phase = MovementPhase.Moving,
            originPresetId = preset.id,
            currentLatitude = 37.5,
            currentLongitude = 127.0,
            currentAltitude = 40.0,
        )
        val hookState = HookState.from(running(movement = movement), presets::get)
        assertEquals(37.5, hookState.gps!!.latitude, 0.0)
        assertEquals(127.0, hookState.gps!!.longitude, 0.0)
        assertEquals(preset.gps.accuracyMeters, hookState.gps!!.accuracyMeters)
        assertEquals(emptyList<WifiNetwork>(), hookState.wifiNetworks)
        assertEquals(preset.cellTowers, hookState.cellTowers)
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val hookState = HookState.from(running(), presets::get)
        assertEquals(hookState, HookState.decode(hookState.encode()))
    }

    @Test
    fun unreadableTextDecodesAsInactive() {
        assertEquals(HookState.INACTIVE, HookState.decode(null))
        assertEquals(HookState.INACTIVE, HookState.decode("not json"))
    }

    @Test
    fun staleHeartbeatIsNotLive() {
        val hookState = HookState(heartbeatAtMillis = 50_000L)
        assertTrue(hookState.isLive(50_000L + HookState.HEARTBEAT_TIMEOUT_MILLIS))
        assertFalse(hookState.isLive(50_001L + HookState.HEARTBEAT_TIMEOUT_MILLIS))
        assertFalse(HookState.INACTIVE.isLive(1L))
    }
}
