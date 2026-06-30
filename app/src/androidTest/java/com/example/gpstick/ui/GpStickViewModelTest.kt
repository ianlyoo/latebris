package com.example.gpstick.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gpstick.data.preset.DeviceStateCaptureRepository
import com.example.gpstick.data.preset.DeviceStateCaptureDataSource
import com.example.gpstick.data.preset.GpsPreset
import com.example.gpstick.data.preset.InMemoryPresetRepository
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.service.ForegroundServiceController
import com.example.gpstick.service.SimulationStateStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GpStickViewModelTest {

    @Test
    fun startSimulation_emitsMessage_whenAlreadyRunning() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)
        val preset = LocationPreset(
            id = "preset-1",
            name = "Demo",
            summary = "Demo",
            gps = GpsPreset(1.0, 2.0, 3.0, 4f),
        )

        simulationStateStore.setSimulationActive(
            activePresetId = preset.id,
            sessionId = "session-running-test",
        )

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(listOf(preset)),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        viewModel.startSimulation()

        assertEquals("Simulation is already running.", messageDeferred.await())
    }

    @Test
    fun startSimulation_emitsMessage_whenNoPresetSelected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(emptyList()),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        viewModel.startSimulation()

        assertEquals("No preset selected.", messageDeferred.await())
    }

    @Test
    fun stopSimulation_emitsMessage_whenAlreadyStopped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(emptyList()),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        viewModel.stopSimulation()

        assertEquals("Simulation is already stopped.", messageDeferred.await())
    }

    @Test
    fun startSimulation_emitsMessage_whenControllerReturnsFailure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)

        val preset = LocationPreset(
            id = "preset-2",
            name = "Demo",
            summary = "Demo",
            gps = GpsPreset(1.0, 2.0, 3.0, 4f),
        )

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(listOf(preset)),
            serviceController = FakeServiceController(startResult = false),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        viewModel.startSimulation()

        assertEquals(
            "Unable to start simulation. Check required permissions and active features.",
            messageDeferred.await(),
        )
    }

    @Test
    fun coordinateEditorFields_normalizeDeviceKeyboardDecimalInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)
        val repository = InMemoryPresetRepository(emptyList())

        val viewModel = GpStickViewModel(
            presetRepository = repository,
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        viewModel.openPresetEditor(presetId = null)
        viewModel.updateEditorName("Seoul")
        viewModel.updateEditorLatitude("３７,５６６５")
        viewModel.updateEditorLongitude("−１２６,９７８０")
        viewModel.updateEditorAltitude("１０,５")

        assertEquals("３７,５６６５", viewModel.presetEditorState.latitude)
        assertEquals("−１２６,９７８０", viewModel.presetEditorState.longitude)
        assertEquals("１０,５", viewModel.presetEditorState.altitude)
        assertTrue(viewModel.presetEditorState.isSaveEnabled)
        assertTrue(viewModel.savePresetEdits())

        val savedPreset = repository.getPresets().single()
        assertEquals(37.5665, savedPreset.gps.latitude, 0.0)
        assertEquals(-126.9780, savedPreset.gps.longitude, 0.0)
        assertEquals(10.5, savedPreset.gps.altitude, 0.0)
    }

    private suspend fun captureFirstUiEventMessage(viewModel: GpStickViewModel): String {
        return coroutineScope {
            val message = CompletableDeferred<String>()
            val collector = launch(Dispatchers.Main.immediate) {
                viewModel.events.collect { event ->
                    if (event is GpStickUiEvent.ShowMessage) {
                        message.complete(event.message)
                    }
                }
            }

            try {
                withTimeout(1000) {
                    message.await()
                }
            } finally {
                collector.cancel()
            }
        }
    }

    @Test
    fun captureCurrentDeviceState_emitsMessage_whenSimulationIsRunning() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)
        simulationStateStore.setSimulationActive(
            activePresetId = "preset-running",
            sessionId = "capture-running-session",
        )

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(emptyList()),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        viewModel.captureCurrentDeviceState { }

        assertEquals(
            "Stop the current simulation before capturing device state.",
            messageDeferred.await(),
        )
    }

    @Test
    fun captureCurrentDeviceState_emitsMessage_whenCaptureFails() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(emptyList()),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(capturedState = null),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        viewModel.captureCurrentDeviceState { }

        assertEquals(
            "Unable to capture current state. Check location permission and current device location.",
            messageDeferred.await(),
        )
    }

    @Test
    fun simulationFailure_emitsMessage_whenStoreReportsNewFailure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(emptyList()),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        simulationStateStore.setSimulationInactive("Simulation stopped because GPS mocking failed.")

        assertEquals(
            "Simulation stopped because GPS mocking failed.",
            messageDeferred.await(),
        )
    }

    @Test
    fun persistedSimulationFailure_emitsMessage_whenViewModelStarts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val simulationStateStore = prepareStore(context)
        simulationStateStore.setSimulationInactive("Simulation stopped because GPS mocking failed.")

        val viewModel = GpStickViewModel(
            presetRepository = InMemoryPresetRepository(emptyList()),
            serviceController = FakeServiceController(startResult = true),
            simulationStateStore = simulationStateStore,
            deviceStateCaptureRepository = FakeCaptureRepository(),
        )

        val messageDeferred = async(Dispatchers.Main.immediate) {
            captureFirstUiEventMessage(viewModel)
        }

        assertEquals(
            "Simulation stopped because GPS mocking failed.",
            messageDeferred.await(),
        )
    }

    private fun prepareStore(context: Context): SimulationStateStore {
        val prefs = context.getSharedPreferences(
            SimulationStateStore.PREF_NAME,
            Context.MODE_PRIVATE,
        )
        prefs.edit().clear().commit()

        val store = SimulationStateStore.getInstance(context)
        store.load()
        store.setFeaturesEnabled(true)
        store.setGpsMockEnabled(true)
        store.setWifiMockEnabled(true)
        store.setCellMockEnabled(true)
        store.setMovementSimulationEnabled(false)
        return store
    }

    private class FakeServiceController(private val startResult: Boolean) : ForegroundServiceController {
        override fun start(preset: LocationPreset): Boolean = startResult

        override fun stop() {
            // no-op in tests
        }
    }

    private class FakeCaptureRepository(
        private val capturedState: com.example.gpstick.data.preset.CapturedDeviceState? = null,
    ) : DeviceStateCaptureDataSource {
        override fun captureCurrentState() = capturedState
    }
}
