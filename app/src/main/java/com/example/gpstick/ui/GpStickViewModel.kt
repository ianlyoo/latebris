package com.example.gpstick.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.gpstick.data.preset.CapturedDeviceState
import com.example.gpstick.data.preset.CellTower
import com.example.gpstick.data.preset.DeviceStateCaptureDataSource
import com.example.gpstick.data.preset.GpsPreset
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetRepository
import com.example.gpstick.data.preset.WifiNetwork
import com.example.gpstick.service.ForegroundServiceController
import com.example.gpstick.service.SimulationControlState
import com.example.gpstick.service.SimulationFeatureSettings
import com.example.gpstick.service.SimulationStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val DEFAULT_ACCURACY_METERS = 12.5f

@Immutable
data class PresetUiModel(
    val id: String,
    val name: String,
    val summary: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
)

@Immutable
data class WifiNetworkEditorUiState(
    val bssid: String = "",
    val ssid: String = "",
    val level: String = "",
    val frequency: String = "",
)

@Immutable
data class CellTowerEditorUiState(
    val mcc: String = "",
    val mnc: String = "",
    val ci: String = "",
    val pci: String = "",
    val tac: String = "",
    val earfcn: String = "",
)

enum class WifiNetworkField {
    Bssid,
    Ssid,
    Level,
    Frequency,
}

enum class CellTowerField {
    Mcc,
    Mnc,
    Ci,
    Pci,
    Tac,
    Earfcn,
}

@Immutable
data class PresetEditorUiState(
    val id: String? = null,
    val isNew: Boolean = true,
    val name: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val altitude: String = "",
    val wifiNetworks: List<WifiNetworkEditorUiState> = emptyList(),
    val cellTowers: List<CellTowerEditorUiState> = emptyList(),
    val isAutoFillInProgress: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val validationMessage: String? = null,
) {
    val canDelete: Boolean
        get() = !isNew && id != null

    val canAutoFill: Boolean
        get() = isSaveEnabled && !isAutoFillInProgress
}

enum class SimulationState {
    Stopped,
    Running,
}

sealed interface GpStickUiEvent {
    data class ShowMessage(val message: String) : GpStickUiEvent
}

@Immutable
data class GpStickUiState(
    val presets: List<PresetUiModel> = emptyList(),
    val selectedPresetId: String? = null,
    val activePresetId: String? = null,
    val simulationState: SimulationState = SimulationState.Stopped,
    val pendingFeaturesEnabled: Boolean = true,
    val pendingGpsMockEnabled: Boolean = true,
    val pendingWifiMockEnabled: Boolean = true,
    val pendingCellMockEnabled: Boolean = true,
    val pendingMovementSimulationEnabled: Boolean = false,
    val activeFeaturesEnabled: Boolean = true,
    val activeGpsMockEnabled: Boolean = true,
    val activeWifiMockEnabled: Boolean = true,
    val activeCellMockEnabled: Boolean = true,
    val activeMovementSimulationEnabled: Boolean = false,
    val locationPermissionGranted: Boolean = true,
    val notificationPermissionGranted: Boolean = true,
    val notificationPermissionRequired: Boolean = false,
    val canStartSimulation: Boolean = true,
) {
    val selectedPreset: PresetUiModel?
        get() = presets.firstOrNull { it.id == selectedPresetId }

    val activePreset: PresetUiModel?
        get() = presets.firstOrNull { it.id == activePresetId }

    val permissionsReady: Boolean
        get() = locationPermissionGranted &&
            (!notificationPermissionRequired || notificationPermissionGranted)
}

class GpStickViewModel(
    private val presetRepository: PresetRepository,
    private val serviceController: ForegroundServiceController,
    private val simulationStateStore: SimulationStateStore,
    private val deviceStateCaptureRepository: DeviceStateCaptureDataSource,
) : ViewModel() {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialSimulationState = simulationStateStore.load()
    private val initialPresets = presetRepository.getPresets()
    private val _events = MutableSharedFlow<GpStickUiEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<GpStickUiEvent> = _events.asSharedFlow()
    private var lastHandledFailureEventId = if (initialSimulationState.failureMessage == null) {
        initialSimulationState.failureEventId
    } else {
        initialSimulationState.failureEventId - 1
    }

    var uiState by mutableStateOf(
        GpStickUiState(
            presets = initialPresets.map(LocationPreset::toUiModel),
            selectedPresetId = initialSimulationState.activePresetId ?: initialPresets.firstOrNull()?.id,
            activePresetId = initialSimulationState.activePresetId,
            simulationState = initialSimulationState.toUiState(),
            pendingFeaturesEnabled = initialSimulationState.featuresEnabled,
            pendingGpsMockEnabled = initialSimulationState.isGpsMockEnabled,
            pendingWifiMockEnabled = initialSimulationState.isWifiMockEnabled,
            pendingCellMockEnabled = initialSimulationState.isCellMockEnabled,
            pendingMovementSimulationEnabled = initialSimulationState.isMovementSimulationEnabled,
            activeFeaturesEnabled = initialSimulationState.activeFeaturesEnabled,
            activeGpsMockEnabled = initialSimulationState.activeGpsMockEnabled,
            activeWifiMockEnabled = initialSimulationState.activeWifiMockEnabled,
            activeCellMockEnabled = initialSimulationState.activeCellMockEnabled,
            activeMovementSimulationEnabled = initialSimulationState.activeMovementSimulationEnabled,
            canStartSimulation = initialSimulationState.hasAnyMockFeatureEnabled,
        )
    )
        private set

    var presetEditorState by mutableStateOf(emptyPresetEditorState())
        private set

    init {
        simulationStateStore.state
            .onEach { state ->
                refreshPresetList(
                    selectedPresetId = state.activePresetId ?: uiState.selectedPresetId,
                    activePresetId = state.activePresetId,
                    simulationState = state,
                )
                maybeEmitFailureMessage(state)
            }
            .launchIn(viewModelScope)
    }

    fun selectPreset(presetId: String) {
        if (uiState.simulationState == SimulationState.Running) {
            return
        }

        val presetExists = uiState.presets.any { it.id == presetId }
        if (!presetExists) {
            return
        }

        uiState = uiState.copy(selectedPresetId = presetId)
    }

    fun openPresetEditor(presetId: String?) {
        val preset = presetId?.let(presetRepository::getPreset)
        presetEditorState = (preset?.toEditorState() ?: emptyPresetEditorState()).validated()
    }

    fun captureCurrentDeviceState(onCaptured: (Boolean) -> Unit) {
        if (uiState.simulationState == SimulationState.Running) {
            emitUiEvent(GpStickUiEvent.ShowMessage("Stop the current simulation before capturing device state."))
            onCaptured(false)
            return
        }

        viewModelScope.launch {
            val capturedState = withContext(Dispatchers.IO) {
                deviceStateCaptureRepository.captureCurrentState()
            }

            if (capturedState == null) {
                emitUiEvent(
                    GpStickUiEvent.ShowMessage(
                        "Unable to capture current state. Check location permission and current device location.",
                    ),
                )
                onCaptured(false)
                return@launch
            }

            applyCapturedState(capturedState)
            onCaptured(true)
        }
    }

    private fun applyCapturedState(capturedState: CapturedDeviceState) {
        presetEditorState = LocationPreset(
            id = UUID.randomUUID().toString(),
            name = "Captured state ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}",
            summary = buildPresetSummary(
                latitude = capturedState.gps.latitude,
                longitude = capturedState.gps.longitude,
                wifiCount = capturedState.wifiNetworks.size,
                cellCount = capturedState.cellTowers.size,
            ),
            gps = capturedState.gps.copy(
                accuracyMeters = if (capturedState.gps.accuracyMeters > 0f) capturedState.gps.accuracyMeters else DEFAULT_ACCURACY_METERS,
            ),
            wifiNetworks = capturedState.wifiNetworks,
            cellTowers = capturedState.cellTowers,
        ).toNewEditorState().validated()
    }

    fun setFeaturesEnabled(enabled: Boolean) {
        simulationStateStore.setFeaturesEnabled(enabled)
    }

    fun setGpsMockEnabled(enabled: Boolean) {
        simulationStateStore.setGpsMockEnabled(enabled)
    }

    fun setWifiMockEnabled(enabled: Boolean) {
        simulationStateStore.setWifiMockEnabled(enabled)
    }

    fun setCellMockEnabled(enabled: Boolean) {
        simulationStateStore.setCellMockEnabled(enabled)
    }

    fun setMovementSimulationEnabled(enabled: Boolean) {
        simulationStateStore.setMovementSimulationEnabled(enabled)
    }

    fun closePresetEditor() {
        presetEditorState = emptyPresetEditorState()
    }

    fun updateEditorName(value: String) {
        updateEditorState { copy(name = value) }
    }

    fun updateEditorLatitude(value: String) {
        updateEditorState { copy(latitude = value.toSignedDecimalInput()) }
    }

    fun updateEditorLongitude(value: String) {
        updateEditorState { copy(longitude = value.toSignedDecimalInput()) }
    }

    fun updateEditorAltitude(value: String) {
        updateEditorState { copy(altitude = value.toSignedDecimalInput()) }
    }

    fun addWifiNetworkRow() {
        updateEditorState {
            copy(wifiNetworks = wifiNetworks + WifiNetworkEditorUiState())
        }
    }

    fun updateWifiNetworkRow(index: Int, field: WifiNetworkField, value: String) {
        updateEditorState {
            copy(
                wifiNetworks = wifiNetworks.updateAt(index) { row ->
                    when (field) {
                        WifiNetworkField.Bssid -> row.copy(bssid = value)
                        WifiNetworkField.Ssid -> row.copy(ssid = value)
                        WifiNetworkField.Level -> row.copy(level = value.toSignedIntInput())
                        WifiNetworkField.Frequency -> row.copy(frequency = value)
                    }
                }
            )
        }
    }

    fun removeWifiNetworkRow(index: Int) {
        updateEditorState {
            copy(wifiNetworks = wifiNetworks.removeAt(index))
        }
    }

    fun addCellTowerRow() {
        updateEditorState {
            copy(cellTowers = cellTowers + CellTowerEditorUiState())
        }
    }

    fun updateCellTowerRow(index: Int, field: CellTowerField, value: String) {
        updateEditorState {
            copy(
                cellTowers = cellTowers.updateAt(index) { row ->
                    when (field) {
                        CellTowerField.Mcc -> row.copy(mcc = value)
                        CellTowerField.Mnc -> row.copy(mnc = value)
                        CellTowerField.Ci -> row.copy(ci = value)
                        CellTowerField.Pci -> row.copy(pci = value)
                        CellTowerField.Tac -> row.copy(tac = value)
                        CellTowerField.Earfcn -> row.copy(earfcn = value)
                    }
                }
            )
        }
    }

    fun removeCellTowerRow(index: Int) {
        updateEditorState {
            copy(cellTowers = cellTowers.removeAt(index))
        }
    }

    fun savePresetEdits(): Boolean {
        val preset = buildPresetFromEditor() ?: return false
        upsertPreset(preset)
        refreshPresetList(selectedPresetId = preset.id)
        presetEditorState = preset.toEditorState().validated()
        return true
    }

    fun deleteEditingPreset(): Boolean {
        val presetId = presetEditorState.id ?: return false
        presetRepository.deletePreset(presetId)
        val nextSelectedPresetId = uiState.selectedPresetId.takeUnless { it == presetId }
        refreshPresetList(selectedPresetId = nextSelectedPresetId)
        closePresetEditor()
        return true
    }

    fun autoFillCellTowers() {
        val preset = buildPresetFromEditor() ?: return
        presetEditorState = presetEditorState.copy(isAutoFillInProgress = true)

        viewModelScope.launch {
            val resolvedPreset = runCatching {
                withContext(Dispatchers.IO) {
                    presetRepository.autoFillCellData(preset) ?: preset
                }
            }.getOrElse {
                preset
            }

            presetEditorState = presetEditorState.copy(
                cellTowers = resolvedPreset.cellTowers.map(CellTower::toEditorState),
                isAutoFillInProgress = false,
            ).validated()
        }
    }

    fun startSimulation() {
        if (uiState.simulationState == SimulationState.Running) {
            emitUiEvent(GpStickUiEvent.ShowMessage("Simulation is already running."))
            return
        }

        if (!uiState.canStartSimulation) {
            emitUiEvent(GpStickUiEvent.ShowMessage("Cannot start: permissions or features are not ready."))
            return
        }

        val selectedPreset = uiState.selectedPresetId?.let(presetRepository::getPreset) ?: run {
            emitUiEvent(GpStickUiEvent.ShowMessage("No preset selected."))
            return
        }

        if (!serviceController.start(selectedPreset)) {
            emitUiEvent(GpStickUiEvent.ShowMessage("Unable to start simulation. Check required permissions and active features."))
        }
    }

    fun updateRuntimePermissionState(
        locationPermissionGranted: Boolean,
        notificationPermissionGranted: Boolean,
        notificationPermissionRequired: Boolean,
    ) {
        val simulationState = SimulationControlState(
            pendingSettings = SimulationFeatureSettings(
                featuresEnabled = uiState.pendingFeaturesEnabled,
                isGpsMockEnabled = uiState.pendingGpsMockEnabled,
                isWifiMockEnabled = uiState.pendingWifiMockEnabled,
                isCellMockEnabled = uiState.pendingCellMockEnabled,
                isMovementSimulationEnabled = uiState.pendingMovementSimulationEnabled,
            ),
        )
        val canStartSimulation = canStartSimulation(
            locationPermissionGranted = locationPermissionGranted,
            notificationPermissionGranted = notificationPermissionGranted,
            notificationPermissionRequired = notificationPermissionRequired,
            simulationState = simulationState,
        )

        uiState = uiState.copy(
            locationPermissionGranted = locationPermissionGranted,
            notificationPermissionGranted = notificationPermissionGranted,
            notificationPermissionRequired = notificationPermissionRequired,
            canStartSimulation = canStartSimulation,
        )
    }

    fun stopSimulation() {
        if (uiState.simulationState == SimulationState.Stopped) {
            emitUiEvent(GpStickUiEvent.ShowMessage("Simulation is already stopped."))
            return
        }

        serviceController.stop()
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun updateEditorState(transform: PresetEditorUiState.() -> PresetEditorUiState) {
        presetEditorState = presetEditorState.transform().validated()
    }

    private fun refreshPresetList(
        selectedPresetId: String? = uiState.selectedPresetId,
        activePresetId: String? = uiState.activePresetId,
        simulationState: SimulationControlState = simulationStateStore.state.value,
    ) {
        val presets = presetRepository.getPresets().map(LocationPreset::toUiModel)
        val presetIds = presets.map(PresetUiModel::id).toSet()
        val resolvedSelectedPresetId = when {
            selectedPresetId != null && selectedPresetId in presetIds -> selectedPresetId
            activePresetId != null && activePresetId in presetIds -> activePresetId
            else -> presets.firstOrNull()?.id
        }

        uiState = GpStickUiState(
            presets = presets,
            selectedPresetId = resolvedSelectedPresetId,
            activePresetId = activePresetId,
            simulationState = simulationState.toUiState(),
            pendingFeaturesEnabled = simulationState.featuresEnabled,
            pendingGpsMockEnabled = simulationState.isGpsMockEnabled,
            pendingWifiMockEnabled = simulationState.isWifiMockEnabled,
            pendingCellMockEnabled = simulationState.isCellMockEnabled,
            pendingMovementSimulationEnabled = simulationState.isMovementSimulationEnabled,
            activeFeaturesEnabled = simulationState.activeFeaturesEnabled,
            activeGpsMockEnabled = simulationState.activeGpsMockEnabled,
            activeWifiMockEnabled = simulationState.activeWifiMockEnabled,
            activeCellMockEnabled = simulationState.activeCellMockEnabled,
            activeMovementSimulationEnabled = simulationState.activeMovementSimulationEnabled,
            locationPermissionGranted = uiState.locationPermissionGranted,
            notificationPermissionGranted = uiState.notificationPermissionGranted,
            notificationPermissionRequired = uiState.notificationPermissionRequired,
            canStartSimulation = canStartSimulation(
                locationPermissionGranted = uiState.locationPermissionGranted,
                notificationPermissionGranted = uiState.notificationPermissionGranted,
                notificationPermissionRequired = uiState.notificationPermissionRequired,
                simulationState = simulationState,
            ),
        )
    }

    private fun emitUiEvent(event: GpStickUiEvent) {
        _events.tryEmit(event)
    }

    private fun maybeEmitFailureMessage(state: SimulationControlState) {
        val failureMessage = state.failureMessage ?: return
        if (state.failureEventId <= lastHandledFailureEventId) {
            return
        }

        lastHandledFailureEventId = state.failureEventId
        emitUiEvent(GpStickUiEvent.ShowMessage(failureMessage))
    }

    private fun canStartSimulation(
        locationPermissionGranted: Boolean,
        notificationPermissionGranted: Boolean,
        notificationPermissionRequired: Boolean,
        simulationState: SimulationControlState,
    ): Boolean {
        val permissionsReady = locationPermissionGranted &&
            (!notificationPermissionRequired || notificationPermissionGranted)
        return permissionsReady && simulationState.hasAnyMockFeatureEnabled
    }

    private fun upsertPreset(preset: LocationPreset) {
        presetRepository.upsertPreset(preset)
    }

    private fun buildPresetFromEditor(): LocationPreset? {
        val validationMessage = presetEditorState.validationMessage
        if (validationMessage != null) {
            return null
        }

        val latitude = presetEditorState.latitude.toDoubleOrNull() ?: return null
        val longitude = presetEditorState.longitude.toDoubleOrNull() ?: return null
        val altitude = presetEditorState.altitude.toDoubleOrNull() ?: return null
        val id = presetEditorState.id ?: UUID.randomUUID().toString()
        val wifiNetworks = presetEditorState.wifiNetworks.mapNotNull(WifiNetworkEditorUiState::toWifiNetwork)
        val cellTowers = presetEditorState.cellTowers.mapNotNull(CellTowerEditorUiState::toCellTower)

        return LocationPreset(
            id = id,
            name = presetEditorState.name.trim(),
            summary = buildPresetSummary(
                latitude = latitude,
                longitude = longitude,
                wifiCount = wifiNetworks.size,
                cellCount = cellTowers.size,
            ),
            gps = GpsPreset(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                accuracyMeters = presetRepository.getPreset(id)?.gps?.accuracyMeters ?: DEFAULT_ACCURACY_METERS,
            ),
            wifiNetworks = wifiNetworks,
            cellTowers = cellTowers,
        )
    }
}

private fun LocationPreset.toUiModel(): PresetUiModel = PresetUiModel(
    id = id,
    name = name,
    summary = summary,
    latitude = gps.latitude,
    longitude = gps.longitude,
    altitude = gps.altitude,
)

private fun LocationPreset.toEditorState(): PresetEditorUiState = PresetEditorUiState(
    id = id,
    isNew = false,
    name = name,
    latitude = gps.latitude.toEditorNumber(),
    longitude = gps.longitude.toEditorNumber(),
    altitude = gps.altitude.toEditorNumber(),
    wifiNetworks = wifiNetworks.map(WifiNetwork::toEditorState),
    cellTowers = cellTowers.map(CellTower::toEditorState),
)

private fun LocationPreset.toNewEditorState(): PresetEditorUiState = toEditorState().copy(
    isNew = true,
)

private fun WifiNetwork.toEditorState(): WifiNetworkEditorUiState = WifiNetworkEditorUiState(
    bssid = bssid,
    ssid = ssid,
    level = level.toString(),
    frequency = frequency.toString(),
)

private fun CellTower.toEditorState(): CellTowerEditorUiState = CellTowerEditorUiState(
    mcc = mcc.toString(),
    mnc = mnc.toString(),
    ci = ci.toString(),
    pci = pci.toString(),
    tac = tac.toString(),
    earfcn = earfcn.toString(),
)

private fun emptyPresetEditorState(): PresetEditorUiState = PresetEditorUiState()

private fun PresetEditorUiState.validated(): PresetEditorUiState {
    val validationMessage = validationMessage()
    return copy(
        isSaveEnabled = validationMessage == null && !isAutoFillInProgress,
        validationMessage = validationMessage,
    )
}

private fun PresetEditorUiState.validationMessage(): String? {
    if (name.isBlank()) {
        return "Enter a preset name."
    }

    val latitudeValue = latitude.toDoubleOrNull()
    val longitudeValue = longitude.toDoubleOrNull()
    val altitudeValue = altitude.toDoubleOrNull()
    if (latitudeValue == null || latitudeValue !in -90.0..90.0) {
        return "Enter a valid latitude between -90 and 90."
    }
    if (longitudeValue == null || longitudeValue !in -180.0..180.0) {
        return "Enter a valid longitude between -180 and 180."
    }
    if (altitudeValue == null) {
        return "Enter a valid altitude."
    }
    if (wifiNetworks.any(WifiNetworkEditorUiState::isInvalid)) {
        return "Complete or remove any Wi-Fi rows with partial values."
    }
    if (cellTowers.any(CellTowerEditorUiState::isInvalid)) {
        return "Complete or remove any cell tower rows with partial values."
    }
    return null
}

private fun WifiNetworkEditorUiState.isBlank(): Boolean =
    bssid.isBlank() && ssid.isBlank() && level.isBlank() && frequency.isBlank()

private fun WifiNetworkEditorUiState.isInvalid(): Boolean =
    !isBlank() && toWifiNetwork() == null

private fun WifiNetworkEditorUiState.toWifiNetwork(): WifiNetwork? {
    if (isBlank()) {
        return null
    }

    return WifiNetwork(
        bssid = bssid.trim().takeIf(String::isNotEmpty) ?: return null,
        ssid = ssid.trim().takeIf(String::isNotEmpty) ?: return null,
        level = level.toIntOrNull() ?: return null,
        frequency = frequency.toIntOrNull() ?: return null,
    )
}

private fun CellTowerEditorUiState.isBlank(): Boolean =
    mcc.isBlank() && mnc.isBlank() && ci.isBlank() && pci.isBlank() && tac.isBlank() && earfcn.isBlank()

private fun CellTowerEditorUiState.isInvalid(): Boolean =
    !isBlank() && toCellTower() == null

private fun CellTowerEditorUiState.toCellTower(): CellTower? {
    if (isBlank()) {
        return null
    }

    return CellTower(
        mcc = mcc.toIntOrNull() ?: return null,
        mnc = mnc.toIntOrNull() ?: return null,
        ci = ci.toIntOrNull() ?: return null,
        pci = pci.toIntOrNull() ?: return null,
        tac = tac.toIntOrNull() ?: return null,
        earfcn = earfcn.toIntOrNull() ?: return null,
    )
}

private fun buildPresetSummary(
    latitude: Double,
    longitude: Double,
    wifiCount: Int,
    cellCount: Int,
): String = "Lat ${latitude.formatCoordinate()}, Lon ${longitude.formatCoordinate()} | $wifiCount Wi-Fi | $cellCount cells"

private fun Double.toEditorNumber(): String = toString()

private fun String.toSignedDecimalInput(): String {
    val filtered = buildString {
        forEachIndexed { index, char ->
            when {
                char.isDigit() -> append(char)
                char == '-' && index == 0 && '-' !in this -> append(char)
                char == '.' && '.' !in this -> append(char)
            }
        }
    }

    return when {
        filtered.startsWith(".") -> "0$filtered"
        filtered.startsWith("-.") -> filtered.replaceFirst("-.", "-0.")
        else -> filtered
    }
}

private fun String.toSignedIntInput(): String = buildString {
    forEachIndexed { index, char ->
        when {
            char.isDigit() -> append(char)
            char == '-' && index == 0 && '-' !in this -> append(char)
        }
    }
}

private fun Double.formatCoordinate(): String = String.format(java.util.Locale.US, "%.4f", this)

private fun <T> List<T>.updateAt(index: Int, transform: (T) -> T): List<T> {
    if (index !in indices) {
        return this
    }

    return mapIndexed { currentIndex, item ->
        if (currentIndex == index) {
            transform(item)
        } else {
            item
        }
    }
}

private fun <T> List<T>.removeAt(index: Int): List<T> {
    if (index !in indices) {
        return this
    }

    return filterIndexed { currentIndex, _ -> currentIndex != index }
}

private fun SimulationControlState.toUiState(): SimulationState = when {
    isRunning -> SimulationState.Running
    else -> SimulationState.Stopped
}
