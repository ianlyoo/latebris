package io.github.ianlyoo.latebris.service

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ianlyoo.latebris.data.preset.FilePresetRepository
import io.github.ianlyoo.latebris.hook.HookState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HookStatePublisherTest {

    @Test
    fun publishesTheActivePresetAndClearsItOnStop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences(SimulationStateStore.PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        val store = SimulationStateStore.getInstance(context).also { it.load() }
        val presetRepository = FilePresetRepository(context)
        val preset = presetRepository.getPresets().first()

        // The app's own publisher is already running; read what it writes.
        store.setSimulationActive(activePresetId = preset.id, sessionId = "publisher-test")
        val published = awaitPublished(context) { it.gps != null }
        assertEquals(preset.gps, published.gps)
        assertEquals(preset.wifiNetworks, published.wifiNetworks)
        assertEquals(preset.cellTowers, published.cellTowers)

        store.setSimulationInactive()
        assertEquals(HookState.INACTIVE, awaitPublished(context) { it == HookState.INACTIVE })
    }

    private fun awaitPublished(context: Context, accept: (HookState) -> Boolean): HookState {
        // Without LSPosed the publisher falls back to a private file with the same name.
        val preferences = context.getSharedPreferences(HookState.PREF_NAME, Context.MODE_PRIVATE)
        var last = HookState.INACTIVE
        repeat(50) {
            last = HookState.decode(preferences.getString(HookState.KEY_STATE, null))
            if (accept(last)) {
                return last
            }
            Thread.sleep(100)
        }
        return last
    }
}
