package com.example.gpstick.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.gpstick.ui.theme.GpStickSpacing
import java.util.Locale
import kotlinx.coroutines.launch

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(GpStickSpacing.drawerWidth),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                DashboardDrawerContent(
                    state = state,
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        onTabSelected(tab)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                DashboardTopBar(
                    selectedTab = selectedTab,
                    isRunning = state.simulationState == SimulationState.Running,
                    onOpenNavigation = {
                        scope.launch { drawerState.open() }
                    },
                )
            },
        ) { innerPadding ->
            ConsoleScreenBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GpStickSpacing.screen, vertical = GpStickSpacing.section),
                    verticalArrangement = Arrangement.spacedBy(GpStickSpacing.section),
                ) {
                    DashboardHeroCard(
                        state = state,
                        selectedTab = selectedTab,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    selectedTab: DashboardTab,
    isRunning: Boolean,
    onOpenNavigation: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        navigationIcon = {
            TextButton(
                onClick = onOpenNavigation,
                modifier = Modifier.testTag(GpStickTestTags.DASHBOARD_DRAWER_OPEN),
            ) {
                Text("Menu")
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro)) {
                Text(
                    text = "Latebris QA Console",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = selectedTab.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            ConsoleBadge(
                text = if (isRunning) "Simulation live" else "Ready",
                highlighted = isRunning,
                modifier = Modifier.padding(end = GpStickSpacing.screen),
            )
        },
    )
}

@Composable
private fun DashboardDrawerContent(
    state: GpStickUiState,
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GpStickSpacing.card, vertical = GpStickSpacing.hero),
        verticalArrangement = Arrangement.spacedBy(GpStickSpacing.section),
    ) {
        ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            ConsoleSectionHeader(
                eyebrow = "Workspace",
                title = "Navigation",
                description = "Open the drawer when you need it and keep the dashboard focused when you do not.",
            )
        }
        ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surface) {
            Text(
                text = "Views",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DashboardTab.values().forEach { tab ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.testTag(tab.testTag),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            ConsoleSectionHeader(
                eyebrow = "Snapshot",
                title = if (state.simulationState == SimulationState.Running) "Simulation running" else "Idle and ready",
                description = state.selectedPreset?.name ?: "No preset selected yet.",
            )
            LabeledValue(
                label = "Permissions",
                value = if (state.permissionsReady) "Ready" else "Action required",
            )
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

private val DashboardTab.description: String
    get() = when (this) {
        DashboardTab.Presets -> "Manage profiles, capture current state, and open the editor."
        DashboardTab.Status -> "Track live simulation state, permissions, and runtime actions."
        DashboardTab.Options -> "Stage the next simulation run without changing live behavior."
        DashboardTab.Help -> "Review the workflow guidance for presets, status, and options."
    }

@Composable
private fun DashboardHeroCard(
    state: GpStickUiState,
    selectedTab: DashboardTab,
) {
    ConsolePanelCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ConsoleSectionHeader(
            eyebrow = "Dashboard",
            title = selectedTab.label,
            description = selectedTab.description,
            trailing = {
                ConsoleBadge(
                    text = if (state.permissionsReady) "Permissions ready" else "Permissions needed",
                    highlighted = state.permissionsReady,
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            DashboardMetric(
                modifier = Modifier.weight(1f),
                label = "Run state",
                value = if (state.simulationState == SimulationState.Running) "Running" else "Stopped",
            )
            DashboardMetric(
                modifier = Modifier.weight(1f),
                label = "Preset library",
                value = "${state.presets.size} profiles",
            )
            DashboardMetric(
                modifier = Modifier.weight(1f),
                label = "Current preset",
                value = state.selectedPreset?.name ?: "None selected",
            )
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    ConsolePanelCard(
        modifier = modifier.verticalScroll(rememberScrollState()),
        containerColor = MaterialTheme.colorScheme.surface,
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

    ConsolePanelCard(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        containerColor = if (isRunning) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        ConsoleSectionHeader(
            eyebrow = if (isRunning) "Live simulation" else "Runtime control",
            title = "Simulation status",
            description = if (isRunning) {
                "Simulation is running in the background and will continue until you press Stop."
            } else {
                "Review the current status here, then press Start to apply the pending settings from the Options tab."
            },
            trailing = {
                ConsoleBadge(
                    text = if (isRunning) "Running" else "Stopped",
                    highlighted = isRunning,
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            LabeledValue(
                label = "State",
                value = if (isRunning) "Running" else "Stopped",
                modifier = Modifier
                    .weight(1f)
                    .testTag(GpStickTestTags.SIMULATION_STATUS),
            )
            LabeledValue(
                label = "Permission readiness",
                value = if (state.permissionsReady) "Ready" else "Action required",
                modifier = Modifier
                    .weight(1f)
                    .testTag(GpStickTestTags.PERMISSION_STATUS),
            )
            LabeledValue(
                label = if (isRunning) "Active preset" else "Selected preset",
                value = highlightedPreset?.name ?: "None selected",
                modifier = Modifier
                    .weight(1f)
                    .testTag(GpStickTestTags.SELECTED_PRESET),
            )
        }
        ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            PermissionStatusSection(state = state)
            coordinates?.let {
                LabeledValue(
                    label = "Active coordinates",
                    value = it,
                    modifier = Modifier.testTag(GpStickTestTags.ACTIVE_PRESET_COORDINATES),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            SettingsSummarySection(
                title = if (isRunning) "Applied on current run" else "Will apply on next start",
                featuresEnabled = if (isRunning) state.activeFeaturesEnabled else state.pendingFeaturesEnabled,
                gpsEnabled = if (isRunning) state.activeGpsMockEnabled else state.pendingGpsMockEnabled,
                wifiEnabled = if (isRunning) state.activeWifiMockEnabled else state.pendingWifiMockEnabled,
                cellEnabled = if (isRunning) state.activeCellMockEnabled else state.pendingCellMockEnabled,
                movementEnabled = if (isRunning) state.activeMovementSimulationEnabled else state.pendingMovementSimulationEnabled,
            )
        }
        if (!state.permissionsReady) {
            ConsolePanelCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    text = "Grant $missingPermissionsMessage to enable Start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                FilledTonalButton(
                    onClick = onRequestPermissions,
                    modifier = Modifier.testTag(GpStickTestTags.REQUEST_PERMISSIONS_CONTROL),
                ) {
                    Text("Request permissions")
                }
            }
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
    ConsolePanelCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ConsoleSectionHeader(
            eyebrow = "Profiles",
            title = "Preset library",
            description = if (enabled) {
                "Select a simulation profile, open the editor, or capture current device coordinates into a new preset."
            } else {
                "Preset actions are locked while the simulation is running."
            },
            trailing = {
                ConsoleBadge(
                    text = "${presets.size} total",
                    highlighted = selectedPresetId != null,
                )
            },
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag(GpStickTestTags.CAPTURE_CURRENT_STATE_CONTROL),
            ) {
                Text("Capture current state")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(GpStickTestTags.PRESET_LIST),
            contentPadding = PaddingValues(vertical = GpStickSpacing.micro),
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
                        ConsoleBadge(text = "Selected", highlighted = true)
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
    ConsolePanelCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ConsoleSectionHeader(
            eyebrow = "Staged controls",
            title = "Simulation options",
            description = "Changes here are saved as pending settings. They are applied the next time you press Start from the Status tab.",
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

@Composable
private fun ToggleOption(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    ConsolePanelCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
            )
        }
    }
}

@Composable
private fun HelpPanel(modifier: Modifier = Modifier) {
    ConsolePanelCard(
        modifier = modifier.verticalScroll(rememberScrollState()),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ConsoleSectionHeader(
            eyebrow = "Operator notes",
            title = "Help",
            description = "Quick reminders for how the dashboard, presets, and simulation controls work together.",
        )
        HelpCallout(text = "Use Presets to select and edit profiles, capture current device location as a new profile, and manage the list.")
        HelpCallout(text = "Use Status to check active simulation state and permission readiness.")
        HelpCallout(text = "Use Status to start or stop the simulation. Options only change what will be applied on the next start.")
        HelpCallout(
            text = "Need a new profile? Capture current state and then save in the editor.",
            highlighted = true,
        )
    }
}

@Composable
private fun HelpCallout(
    text: String,
    highlighted: Boolean = false,
) {
    ConsolePanelCard(
        containerColor = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
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
