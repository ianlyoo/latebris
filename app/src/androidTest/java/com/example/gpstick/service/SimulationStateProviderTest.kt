package com.example.gpstick.service

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gpstick.data.preset.FilePresetRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimulationStateProviderTest {

    @Test
    fun providerCall_returnsOnlyActiveRuntimeFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = prepareStore(context)
        val presetRepository = FilePresetRepository(context)
        val preset = presetRepository.getPresets().first()

        store.setSimulationActive(
            activePresetId = preset.id,
            sessionId = "provider-session",
        )

        val bundle = context.contentResolver.call(
            SimulationStateStore.CONTENT_URI,
            SimulationStateStore.METHOD_GET_STATE,
            null,
            null,
        )

        assertTrue(bundle?.getBoolean(SimulationStateStore.KEY_RUNNING, false) == true)
        assertNotNull(bundle?.getString(SimulationStateStore.KEY_ACTIVE_PRESET_JSON))
        assertTrue(bundle?.containsKey(SimulationStateStore.KEY_ACTIVE_GPS_MOCK_ENABLED) == true)
        assertFalse(bundle?.containsKey(SimulationStateStore.KEY_PENDING_GPS_MOCK_ENABLED) == true)
        assertFalse(bundle?.containsKey(SimulationStateStore.KEY_SESSION_ID) == true)
        assertFalse(bundle?.containsKey(SimulationStateStore.KEY_LAST_FAILURE_MESSAGE) == true)
    }

    @Test
    fun providerCall_returnsEmptyForUnknownMethod() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val bundle = context.contentResolver.call(
            SimulationStateStore.CONTENT_URI,
            "unknownMethod",
            null,
            null,
        )

        assertNull(bundle)
    }

    private fun prepareStore(context: Context): SimulationStateStore {
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
