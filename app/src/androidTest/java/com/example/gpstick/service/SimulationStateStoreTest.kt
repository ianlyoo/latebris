package com.example.gpstick.service

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
