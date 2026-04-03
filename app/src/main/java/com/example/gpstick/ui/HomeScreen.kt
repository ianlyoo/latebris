package com.example.gpstick.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gpstick.ui.theme.GpStickSpacing
import java.util.Locale

enum class DashboardTab(val label: String) {
    Presets("Presets"),
    Status("Status"),
    Options("Options"),
    Help("Help"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: GpStickUiState,
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    onPresetSelected: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    onFeaturesEnabledChanged: (Boolean) -> Unit,
    onGpsMockEnabledChanged: (Boolean) -> Unit,
    onWifiMockEnabledChanged: (Boolean) -> Unit,
    onCellMockEnabledChanged: (Boolean) -> Unit,
    onMovementSimulationEnabledChanged: (Boolean) -> Unit,
    onCaptureCurrentState: () -> Unit,
    onCreatePreset: () -> Unit,
    onEditPreset: (String) -> Unit,
) {
    val presetsEditable = state.simulationState == SimulationState.Stopped
    val canStartSimulation = state.selectedPreset != null &&
        presetsEditable &&
        state.permissionsReady &&
        state.canStartSimulation

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                text = "Latebris QA Console",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GpStickSpacing.screen, vertical = GpStickSpacing.section),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
            verticalAlignment = Alignment.Top,
        ) {
            DashboardSidebar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        DashboardContent(
            modifier = Modifier.weight(1f),
            selectedTab = selectedTab,
            state = state,
            presetsEditable = presetsEditable,
            canStartSimulation = canStartSimulation,
            onPresetSelected = onPresetSelected,
            onStart = onStart,
            onStop = onStop,
            onRequestPermissions = onRequestPermissions,
            onFeaturesEnabledChanged = onFeaturesEnabledChanged,
            onGpsMockEnabledChanged = onGpsMockEnabledChanged,
            onWifiMockEnabledChanged = onWifiMockEnabledChanged,
            onCellMockEnabledChanged = onCellMockEnabledChanged,
            onMovementSimulationEnabledChanged = onMovementSimulationEnabledChanged,
            onCaptureCurrentState = onCaptureCurrentState,
            onCreatePreset = onCreatePreset,
            onEditPreset = onEditPreset,
        )
        }
    }
}

@Composable
private fun DashboardSidebar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.compact),
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.titleMedium,
            )
            DashboardTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                if (isSelected) {
                    FilledTonalButton(
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tab.testTag),
                    ) {
                        Text(tab.label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tab.testTag),
                    ) {
                        Text(tab.label)
                    }
                }
            }
        }
    }
}

private val DashboardTab.testTag: String
    get() = when (this) {
        DashboardTab.Presets -> GpStickTestTags.DASHBOARD_TAB_PRESETS
        DashboardTab.Status -> GpStickTestTags.DASHBOARD_TAB_STATUS
        DashboardTab.Options -> GpStickTestTags.DASHBOARD_TAB_OPTIONS
        DashboardTab.Help -> GpStickTestTags.DASHBOARD_TAB_HELP
    }

@Composable
private fun DashboardContent(
    modifier: Modifier,
    selectedTab: DashboardTab,
    state: GpStickUiState,
    presetsEditable: Boolean,
    canStartSimulation: Boolean,
    onPresetSelected: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    onFeaturesEnabledChanged: (Boolean) -> Unit,
    onGpsMockEnabledChanged: (Boolean) -> Unit,
    onWifiMockEnabledChanged: (Boolean) -> Unit,
    onCellMockEnabledChanged: (Boolean) -> Unit,
    onMovementSimulationEnabledChanged: (Boolean) -> Unit,
    onCaptureCurrentState: () -> Unit,
    onCreatePreset: () -> Unit,
    onEditPreset: (String) -> Unit,
) {
    when (selectedTab) {
        DashboardTab.Presets -> PresetPanel(
            modifier = modifier
                .fillMaxHeight()
                .testTag(GpStickTestTags.DASHBOARD_PRESETS_PANEL),
            presets = state.presets,
            selectedPresetId = state.selectedPresetId,
            onPresetSelected = onPresetSelected,
            onCaptureCurrentState = onCaptureCurrentState,
            onCreatePreset = onCreatePreset,
            onEditPreset = onEditPreset,
            enabled = presetsEditable,
        )

        DashboardTab.Status -> StatusPanel(
            state = state,
            canStart = canStartSimulation,
            canStop = state.simulationState == SimulationState.Running,
            onStart = onStart,
            onStop = onStop,
            onRequestPermissions = onRequestPermissions,
            modifier = modifier
                .fillMaxSize()
                .testTag(GpStickTestTags.DASHBOARD_STATUS_PANEL),
        )

        DashboardTab.Options -> ControlPanel(
            state = state,
            onFeaturesEnabledChanged = onFeaturesEnabledChanged,
            onGpsMockEnabledChanged = onGpsMockEnabledChanged,
            onWifiMockEnabledChanged = onWifiMockEnabledChanged,
            onCellMockEnabledChanged = onCellMockEnabledChanged,
            onMovementSimulationEnabledChanged = onMovementSimulationEnabledChanged,
            modifier = modifier
                .fillMaxSize()
                .testTag(GpStickTestTags.DASHBOARD_OPTIONS_PANEL),
        )

        DashboardTab.Help -> HelpPanel(
            modifier = modifier
                .fillMaxSize()
                .testTag(GpStickTestTags.DASHBOARD_HELP_PANEL),
        )
    }
}

@Composable
private fun StatusPanel(
    state: GpStickUiState,
    canStart: Boolean,
    canStop: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = state.simulationState == SimulationState.Running
    val highlightedPreset = if (isRunning) state.activePreset else state.selectedPreset
    val missingPermissionsMessage = buildString {
        if (!state.locationPermissionGranted) {
            append("Location")
        }
        if (state.notificationPermissionRequired && !state.notificationPermissionGranted) {
            if (isNotEmpty()) append(" and ")
            append("notifications")
        }
    }
    val coordinates = if (isRunning) {
        highlightedPreset?.let {
            "${it.latitude.formatCoordinate()}, ${it.longitude.formatCoordinate()}"
        } ?: "Unavailable"
    } else {
        null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            Text(
                text = "Simulation status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isRunning) {
                    "Simulation is running in the background and will continue until you press Stop."
                } else {
                    "Review the current status here, then press Start to apply the pending settings from the Options tab."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledValue(
                label = "State",
                value = if (isRunning) "Running" else "Stopped",
                modifier = Modifier.testTag(GpStickTestTags.SIMULATION_STATUS),
            )
            LabeledValue(
                label = "Permission readiness",
                value = if (state.permissionsReady) "Ready" else "Action required",
                modifier = Modifier.testTag(GpStickTestTags.PERMISSION_STATUS),
            )
            LabeledValue(
                label = if (isRunning) "Active preset" else "Selected preset",
                value = highlightedPreset?.name ?: "None selected",
                modifier = Modifier.testTag(GpStickTestTags.SELECTED_PRESET),
            )
            PermissionStatusSection(state = state)
            if (coordinates != null) {
                LabeledValue(
                    label = "Active coordinates",
                    value = coordinates,
                    modifier = Modifier.testTag(GpStickTestTags.ACTIVE_PRESET_COORDINATES),
                )
            }
            Divider()
            SettingsSummarySection(
                title = if (isRunning) "Applied on current run" else "Will apply on next start",
                featuresEnabled = if (isRunning) state.activeFeaturesEnabled else state.pendingFeaturesEnabled,
                gpsEnabled = if (isRunning) state.activeGpsMockEnabled else state.pendingGpsMockEnabled,
                wifiEnabled = if (isRunning) state.activeWifiMockEnabled else state.pendingWifiMockEnabled,
                cellEnabled = if (isRunning) state.activeCellMockEnabled else state.pendingCellMockEnabled,
                movementEnabled = if (isRunning) state.activeMovementSimulationEnabled else state.pendingMovementSimulationEnabled,
            )
            if (!state.permissionsReady) {
                FilledTonalButton(
                    onClick = onRequestPermissions,
                    modifier = Modifier.testTag(GpStickTestTags.REQUEST_PERMISSIONS_CONTROL),
                ) {
                    Text("Request permissions")
                }
                Text(
                    text = "Grant $missingPermissionsMessage to enable Start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
            ) {
                Button(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(GpStickTestTags.START_CONTROL),
                ) {
                    Text("Start")
                }
                OutlinedButton(
                    onClick = onStop,
                    enabled = canStop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(GpStickTestTags.STOP_CONTROL),
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
private fun SettingsSummarySection(
    title: String,
    featuresEnabled: Boolean,
    gpsEnabled: Boolean,
    wifiEnabled: Boolean,
    cellEnabled: Boolean,
    movementEnabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GpStickSpacing.compact)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        LabeledValue(label = "Master switch", value = if (featuresEnabled) "Enabled" else "Disabled")
        LabeledValue(label = "GPS mock", value = if (gpsEnabled) "On" else "Off")
        LabeledValue(label = "Wi-Fi mock", value = if (wifiEnabled) "On" else "Off")
        LabeledValue(label = "Cell mock", value = if (cellEnabled) "On" else "Off")
        LabeledValue(label = "Movement", value = if (movementEnabled) "On" else "Off")
    }
}

@Composable
private fun PermissionStatusSection(state: GpStickUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(GpStickSpacing.compact)) {
        LabeledValue(
            label = "Location access",
            value = if (state.locationPermissionGranted) "Granted" else "Required",
            modifier = Modifier.testTag(GpStickTestTags.LOCATION_PERMISSION_STATUS),
        )
        LabeledValue(
            label = "Notifications",
            value = when {
                !state.notificationPermissionRequired -> "Not required"
                state.notificationPermissionGranted -> "Granted"
                else -> "Required"
            },
            modifier = Modifier.testTag(GpStickTestTags.NOTIFICATION_PERMISSION_STATUS),
        )
    }
}

@Composable
private fun PresetPanel(
    modifier: Modifier = Modifier,
    presets: List<PresetUiModel>,
    selectedPresetId: String?,
    onPresetSelected: (String) -> Unit,
    onCaptureCurrentState: () -> Unit,
    onCreatePreset: () -> Unit,
    onEditPreset: (String) -> Unit,
    enabled: Boolean,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = GpStickSpacing.card),
                verticalArrangement = Arrangement.spacedBy(GpStickSpacing.compact),
            ) {
                Text(
                    text = "Preset list",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
                    FilledTonalButton(
                        onClick = onCreatePreset,
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(GpStickTestTags.NEW_PRESET_CONTROL),
                    ) {
                        Text("New preset")
                    }
                    Button(
                        onClick = onCaptureCurrentState,
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(GpStickTestTags.CAPTURE_CURRENT_STATE_CONTROL),
                    ) {
                        Text("Capture current state")
                    }
                }
                Text(
                    text = if (enabled) {
                        "Select a simulation profile, open the editor, or capture current device coordinates into a new preset."
                    } else {
                        "Preset actions are locked while the simulation is running."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(GpStickTestTags.PRESET_LIST),
                contentPadding = PaddingValues(
                    start = GpStickSpacing.card,
                    end = GpStickSpacing.card,
                    bottom = GpStickSpacing.card,
                ),
                verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
            ) {
                items(items = presets, key = { it.id }) { preset ->
                    PresetRow(
                        preset = preset,
                        selected = preset.id == selectedPresetId,
                        enabled = enabled,
                        onClick = { onPresetSelected(preset.id) },
                        onEdit = { onEditPreset(preset.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetRow(
    preset: PresetUiModel,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(GpStickSpacing.border, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.compact),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro),
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${preset.latitude.formatCoordinate()}, ${preset.longitude.formatCoordinate()} | Alt ${preset.altitude.formatCoordinate()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.compact),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.extraLarge,
                                )
                                .padding(
                                    horizontal = GpStickSpacing.badgeHorizontal,
                                    vertical = GpStickSpacing.badgeVertical,
                                ),
                        ) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    TextButton(
                        onClick = onEdit,
                        enabled = enabled,
                        modifier = Modifier.testTag(GpStickTestTags.editPresetControl(preset.id)),
                    ) {
                        Text("Edit")
                    }
                }
            }
            Text(
                text = preset.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ControlPanel(
    state: GpStickUiState,
    onFeaturesEnabledChanged: (Boolean) -> Unit,
    onGpsMockEnabledChanged: (Boolean) -> Unit,
    onWifiMockEnabledChanged: (Boolean) -> Unit,
    onCellMockEnabledChanged: (Boolean) -> Unit,
    onMovementSimulationEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            Text(
                text = "Simulation options",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Changes here are saved as pending settings. They are applied the next time you press Start from the Status tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(GpStickSpacing.section))

            Divider()

            ToggleOption(
                label = "Enable simulation features",
                value = state.pendingFeaturesEnabled,
                onValueChange = onFeaturesEnabledChanged,
                testTag = GpStickTestTags.FEATURES_ENABLED_TOGGLE,
            )
            ToggleOption(
                label = "Mock GPS location",
                value = state.pendingGpsMockEnabled,
                onValueChange = onGpsMockEnabledChanged,
                enabled = state.pendingFeaturesEnabled,
                testTag = GpStickTestTags.GPS_MOCK_ENABLED_TOGGLE,
            )
            ToggleOption(
                label = "Mock Wi-Fi scans",
                value = state.pendingWifiMockEnabled,
                onValueChange = onWifiMockEnabledChanged,
                enabled = state.pendingFeaturesEnabled,
                testTag = GpStickTestTags.WIFI_MOCK_ENABLED_TOGGLE,
            )
            ToggleOption(
                label = "Mock cell info",
                value = state.pendingCellMockEnabled,
                onValueChange = onCellMockEnabledChanged,
                enabled = state.pendingFeaturesEnabled,
                testTag = GpStickTestTags.CELL_MOCK_ENABLED_TOGGLE,
            )
            ToggleOption(
                label = "Movement simulation",
                value = state.pendingMovementSimulationEnabled,
                onValueChange = onMovementSimulationEnabledChanged,
                enabled = state.pendingFeaturesEnabled,
                testTag = GpStickTestTags.MOVEMENT_SIMULATION_ENABLED_TOGGLE,
            )
        }
    }
}

@Composable
private fun ToggleOption(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    val switchModifier = testTag?.let(modifier::testTag) ?: modifier
    Row(
        modifier = switchModifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            enabled = enabled,
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
}

@Composable
private fun HelpPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            Text(
                text = "Help",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Use Presets to select and edit profiles, capture current device location as a new profile, and manage the list.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Use Status to check active simulation state and permission readiness.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Use Status to start or stop the simulation. Options only change what will be applied on the next start.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Need a new profile? Capture current state and then save in the editor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.4f", this)
