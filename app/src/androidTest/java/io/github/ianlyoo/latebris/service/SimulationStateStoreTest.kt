package io.github.ianlyoo.latebris.service

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimulationStateStoreTest {

    @Test
    fun invalidateStaleRunningState_clearsExpiredRunningSession() {
        val store = prepareStore()
        store.setSimulationActive(
            activePresetId = "preset-1",
            sessionId = "session-1",
            heartbeatAtMillis = 1_000L,
        )

        val snapshot = store.invalidateStaleRunningState(
            nowMillis = 20_500L,
            timeoutMillis = 10_000L,
        )

        assertFalse(snapshot.isRunning)
        assertNull(snapshot.activePresetId)
        assertEquals(
            "Simulation session expired because the background runtime stopped responding.",
            snapshot.failureMessage,
        )
        assertEquals(1L, snapshot.failureEventId)
    }

    @Test
    fun invalidateStaleRunningState_keepsFreshRunningSession() {
        val store = prepareStore()
        store.setSimulationActive(
            activePresetId = "preset-2",
            sessionId = "session-2",
            heartbeatAtMillis = 10_000L,
        )

        val snapshot = store.invalidateStaleRunningState(
            nowMillis = 15_000L,
            timeoutMillis = 10_000L,
        )

        assertTrue(snapshot.isRunning)
        assertEquals("preset-2", snapshot.activePresetId)
        assertEquals("session-2", snapshot.sessionId)
        assertEquals(10_000L, snapshot.sessionHeartbeatAtMillis)
    }

    @Test
    fun promotePendingSettingsToActive_copiesPendingSettingsIntoLiveState() {
        val store = prepareStore()
        store.setSimulationActive(
            activePresetId = "preset-3",
            sessionId = "session-3",
        )
        store.setGpsMockEnabled(false)
        store.setMovementSimulationEnabled(true)

        store.promotePendingSettingsToActive()

        val snapshot = store.load()
        assertTrue(snapshot.isRunning)
        assertFalse(snapshot.activeGpsMockEnabled)
        assertTrue(snapshot.activeWifiMockEnabled)
        assertTrue(snapshot.activeCellMockEnabled)
        assertTrue(snapshot.activeMovementSimulationEnabled)
    }

    @Test
    fun movementPositionKeepsDoublePrecision() {
        val store = prepareStore()
        store.setSimulationActive(activePresetId = "preset-1", sessionId = "session-1")
        store.setMovementRouting(
            originPresetId = "preset-1",
            destinationPresetId = "preset-2",
            transportMode = MovementTransportMode.Walk,
            speedMetersPerSecond = 1.4,
            currentLatitude = 37.566512345678,
            currentLongitude = 126.978012345678,
            currentAltitude = 38.123456,
        )
        store.updateMovementProgress(
            progress = 0.5,
            etaEpochMillis = 0L,
            currentLatitude = 37.566598765432,
            currentLongitude = 126.978098765432,
            currentAltitude = 39.654321,
        )

        val movement = store.load().movementSession
        assertEquals(37.566598765432, movement.currentLatitude!!, 0.0)
        assertEquals(126.978098765432, movement.currentLongitude!!, 0.0)
        assertEquals(39.654321, movement.currentAltitude!!, 0.0)
    }

    private fun prepareStore(): SimulationStateStore {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences(
            SimulationStateStore.PREF_NAME,
            Context.MODE_PRIVATE,
        )
        preferences.edit().clear().commit()

        val store = SimulationStateStore.getInstance(context)
        store.load()
        store.setFeaturesEnabled(true)
        store.setGpsMockEnabled(true)
        store.setWifiMockEnabled(true)
        store.setCellMockEnabled(true)
        store.setMovementSimulationEnabled(false)
        return store
    }
}
