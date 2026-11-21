package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.ianlyoo.latebris.hook.ModuleStatus
import io.github.ianlyoo.latebris.ui.theme.Figures
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing
import io.github.ianlyoo.latebris.ui.theme.LocalSignColors

@Composable
internal fun StatusTab(
    state: LatebrisUiState,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = state.simulationState == SimulationState.Running
    val preset = if (isRunning) state.activePreset else state.selectedPreset
    val moduleActive = remember { ModuleStatus.isActive() }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LatebrisSpacing.screen)
            .padding(top = LatebrisSpacing.sm, bottom = LatebrisSpacing.xxl),
    ) {
        RunSign(
            state = state,
            preset = preset,
            isRunning = isRunning,
            canStart = canStart,
            onStart = onStart,
            onStop = onStop,
        )
        SignalSection(state = state, preset = preset, isRunning = isRunning, moduleActive = moduleActive)
        ReadinessSection(state = state, moduleActive = moduleActive, onRequestPermissions = onRequestPermissions)
    }
}

@Composable
private fun RunSign(
    state: LatebrisUiState,
    preset: PresetUiModel?,
    isRunning: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val signs = LocalSignColors.current
    val hint = when {
        isRunning -> "Stop the simulation and return the device to its real location and signals."
        !state.permissionsReady -> "Grant the permissions below to enable Start."
        preset == null -> "Select a preset in Presets to enable Start."
        !state.canStartSimulation -> "Turn on at least one signal in Options to enable Start."
        else -> "Start serves this preset with the signals listed below."
    }

    LocationSign {
        Text(
            text = if (isRunning) "Running" else "Stopped",
            style = MaterialTheme.typography.labelLarge,
            color = if (isRunning) signs.yellow else signs.onInkMuted,
            modifier = Modifier.testTag(LatebrisTestTags.SIMULATION_STATUS),
        )
        Text(
            text = preset?.name ?: "No preset selected",
            style = MaterialTheme.typography.headlineMedium,
            color = if (isRunning) signs.yellow else signs.onInk,
            modifier = Modifier.testTag(LatebrisTestTags.SELECTED_PRESET),
        )
        Text(
            text = preset?.let { formatPosition(it.latitude, it.longitude, it.altitude) }
                ?: "Choose a place in Presets.",
            style = MaterialTheme.typography.bodyMedium.merge(Figures),
            color = signs.onInkMuted,
            modifier = if (isRunning) Modifier.testTag(LatebrisTestTags.ACTIVE_PRESET_COORDINATES) else Modifier,
        )
        Spacer(modifier = Modifier.height(LatebrisSpacing.md))
        Button(
            onClick = if (isRunning) onStop else onStart,
            enabled = isRunning || canStart,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = signs.yellow,
                contentColor = signs.onYellow,
                disabledContainerColor = signs.onInk.copy(alpha = 0.12f),
                disabledContentColor = signs.onInk.copy(alpha = 0.45f),
            ),
            contentPadding = PaddingValues(horizontal = LatebrisSpacing.xl, vertical = LatebrisSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(if (isRunning) LatebrisTestTags.STOP_CONTROL else LatebrisTestTags.START_CONTROL),
        ) {
            if (isRunning) {
                PictogramIcon(Pictogram.Stop)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(LatebrisSpacing.sm))
            Text(text = if (isRunning) "Stop" else "Start", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = signs.onInkMuted,
            modifier = Modifier.padding(top = LatebrisSpacing.xs),
        )
    }
}

@Composable
private fun SignalSection(
    state: LatebrisUiState,
    preset: PresetUiModel?,
    isRunning: Boolean,
    moduleActive: Boolean,
) {
    val features = if (isRunning) state.activeFeaturesEnabled else state.pendingFeaturesEnabled
    val gps = if (isRunning) state.activeGpsMockEnabled else state.pendingGpsMockEnabled
    val wifi = if (isRunning) state.activeWifiMockEnabled else state.pendingWifiMockEnabled
    val cell = if (isRunning) state.activeCellMockEnabled else state.pendingCellMockEnabled
    val drift = if (isRunning) state.activeMovementSimulationEnabled else state.pendingMovementSimulationEnabled

    fun stateOf(on: Boolean) = when {
        !features || !on -> SignalState.Off
        isRunning -> SignalState.Live
        else -> SignalState.Staged
    }
    fun labelOf(signal: SignalState) = when (signal) {
        SignalState.Live -> "Live"
        SignalState.Staged -> "On Start"
        SignalState.Off -> "Off"
    }
    // Wi-Fi and cells are served only by the LSPosed module. When Latebris cannot see the
    // module, it does not claim them as live: the line reads "Hook path" instead.
    fun hookStateOf(on: Boolean) = stateOf(on).let { if (!moduleActive && it == SignalState.Live) SignalState.Staged else it }
    fun hookLabelOf(on: Boolean) = stateOf(on).let { if (!moduleActive && it != SignalState.Off) "Hook path" else labelOf(it) }

    SectionHeading(text = if (isRunning) "Signals in this run" else "Signals on next Start")
    SignalBoard {
        val gpsState = stateOf(gps)
        SignalLine(
            pictogram = Pictogram.Gps,
            label = "GPS position",
            detail = preset?.let { "Accuracy ${formatAccuracy(it.accuracyMeters)}" } ?: "No preset",
            state = gpsState,
            stateLabel = labelOf(gpsState),
        )
        SignalLine(
            pictogram = Pictogram.Wifi,
            label = "Wi-Fi scan",
            detail = preset?.let { countLabel(it.wifiCount, "network", "networks", zero = "No networks") } ?: "No preset",
            state = hookStateOf(wifi),
            stateLabel = hookLabelOf(wifi),
        )
        SignalLine(
            pictogram = Pictogram.Cell,
            label = "LTE cells",
            detail = preset?.let { countLabel(it.cellCount, "cell", "cells", zero = "No cells") } ?: "No preset",
            state = hookStateOf(cell),
            stateLabel = hookLabelOf(cell),
        )
        val driftState = stateOf(drift && gps)
        SignalLine(
            pictogram = Pictogram.Arrow,
            label = "GPS drift",
            detail = "Small random steps around the point",
            state = driftState,
            stateLabel = labelOf(driftState),
            showDivider = false,
        )
    }
    Text(
        text = when {
            !features -> "Simulation features are off in Options, so Start has nothing to serve."
            moduleActive -> "The LSPosed module is loaded. Apps in its scope get these values; every other app keeps its real Wi-Fi and cell data."
            else -> "Without the LSPosed module only GPS changes, through Android's mock location. Wi-Fi and cells stay real."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = LatebrisSpacing.sm),
    )
}

@Composable
private fun ReadinessSection(
    state: LatebrisUiState,
    moduleActive: Boolean,
    onRequestPermissions: () -> Unit,
) {
    SectionHeading(text = "Readiness") {
        Text(
            text = if (state.permissionsReady) "Ready" else "Action needed",
            style = MaterialTheme.typography.labelLarge,
            color = if (state.permissionsReady) LocalSignColors.current.ready else MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(LatebrisTestTags.PERMISSION_STATUS),
        )
    }
    RequirementLine(
        label = "Location access",
        value = if (state.locationPermissionGranted) "Granted" else "Required",
        status = if (state.locationPermissionGranted) Requirement.Met else Requirement.Missing,
        modifier = Modifier.testTag(LatebrisTestTags.LOCATION_PERMISSION_STATUS),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    val notificationsMet = !state.notificationPermissionRequired || state.notificationPermissionGranted
    RequirementLine(
        label = "Notifications",
        value = when {
            !state.notificationPermissionRequired -> "Not required"
            state.notificationPermissionGranted -> "Granted"
            else -> "Required"
        },
        status = if (notificationsMet) Requirement.Met else Requirement.Missing,
        detail = "Shows that a simulation is running",
        modifier = Modifier.testTag(LatebrisTestTags.NOTIFICATION_PERMISSION_STATUS),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    RequirementLine(
        label = "LSPosed module",
        value = if (moduleActive) "Active" else "Not loaded",
        status = if (moduleActive) Requirement.Met else Requirement.Optional,
        detail = if (moduleActive) "Loaded into Latebris" else "Needed for Wi-Fi and cells. Add Latebris to its scope so this check can see it.",
    )
    if (!state.permissionsReady) {
        FilledTonalButton(
            onClick = onRequestPermissions,
            modifier = Modifier
                .padding(top = LatebrisSpacing.md)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(LatebrisTestTags.REQUEST_PERMISSIONS_CONTROL),
        ) {
            Text("Grant permissions")
        }
    }
}
