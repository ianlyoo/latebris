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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpstick.service.MovementPhase
import com.example.gpstick.service.MovementTransportMode
import com.example.gpstick.ui.theme.GpStickSpacing
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class DashboardTab(val label: String) {
    Status("Status"),
    Move("Move"),
    Presets("Presets"),
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
    onMoveDestinationSelected: (String) -> Unit,
    onMoveTransportModeSelected: (MoveTransportOption) -> Unit,
    onMoveSpeedChanged: (Float) -> Unit,
    onStartMovement: () -> Unit,
    onCancelMovement: () -> Unit,
    onApplyNow: () -> Unit,
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
                        onMoveDestinationSelected = onMoveDestinationSelected,
                        onMoveTransportModeSelected = onMoveTransportModeSelected,
                        onMoveSpeedChanged = onMoveSpeedChanged,
                        onStartMovement = onStartMovement,
                        onCancelMovement = onCancelMovement,
                        onApplyNow = onApplyNow,
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
            Text(
                text = "Latebris",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                ),
            )
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
                description = "Open the drawer when you need it and keep the main workspace focused when you do not.",
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
        DashboardTab.Move -> GpStickTestTags.DASHBOARD_TAB_MOVE
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
    onMoveDestinationSelected: (String) -> Unit,
    onMoveTransportModeSelected: (MoveTransportOption) -> Unit,
    onMoveSpeedChanged: (Float) -> Unit,
    onStartMovement: () -> Unit,
    onCancelMovement: () -> Unit,
    onApplyNow: () -> Unit,
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

        DashboardTab.Move -> MovePanel(
            state = state,
            onDestinationSelected = onMoveDestinationSelected,
            onTransportModeSelected = onMoveTransportModeSelected,
            onSpeedChanged = onMoveSpeedChanged,
            onStartMovement = onStartMovement,
            onCancelMovement = onCancelMovement,
            modifier = modifier
                .fillMaxSize()
                .testTag(GpStickTestTags.DASHBOARD_MOVE_PANEL),
        )

        DashboardTab.Options -> ControlPanel(
            state = state,
            onFeaturesEnabledChanged = onFeaturesEnabledChanged,
            onGpsMockEnabledChanged = onGpsMockEnabledChanged,
            onWifiMockEnabledChanged = onWifiMockEnabledChanged,
            onCellMockEnabledChanged = onCellMockEnabledChanged,
            onMovementSimulationEnabledChanged = onMovementSimulationEnabledChanged,
            onApplyNow = onApplyNow,
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
private fun MovePanel(
    state: GpStickUiState,
    onDestinationSelected: (String) -> Unit,
    onTransportModeSelected: (MoveTransportOption) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onStartMovement: () -> Unit,
    onCancelMovement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConsolePanelCard(
        modifier = modifier.verticalScroll(rememberScrollState()),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ConsoleSectionHeader(
            eyebrow = if (state.showsMoveProgress) "Live route" else "Route movement",
            title = "Move",
            description = if (state.showsMoveProgress) {
                "Track the active route in real time, monitor ETA, and stop playback before arrival if needed."
            } else {
                "Pick a saved destination and launch route playback. If no simulation is running yet, Move captures the current device location and starts one automatically."
            },
            trailing = {
                ConsoleBadge(
                    text = when {
                        state.movementPhase == MovementPhase.Arrived -> "Arrived"
                        state.canCancelMovement -> "In motion"
                        state.simulationState == SimulationState.Running -> "Ready"
                        else -> "Unavailable"
                    },
                    highlighted = state.canCancelMovement || state.movementPhase == MovementPhase.Arrived,
                )
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (state.showsMoveProgress) {
            MovementProgressPanel(
                state = state,
                onCancelMovement = onCancelMovement,
            )
        } else {
            MovePlannerPanel(
                state = state,
                onDestinationSelected = onDestinationSelected,
                onTransportModeSelected = onTransportModeSelected,
                onSpeedChanged = onSpeedChanged,
                onStartMovement = onStartMovement,
            )
        }
    }
}

@Composable
private fun MovePlannerPanel(
    state: GpStickUiState,
    onDestinationSelected: (String) -> Unit,
    onTransportModeSelected: (MoveTransportOption) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onStartMovement: () -> Unit,
) {
    val originPreset = state.activePreset
    val originLatitude = state.movementCurrentLatitude ?: originPreset?.latitude
    val originLongitude = state.movementCurrentLongitude ?: originPreset?.longitude
    val originAltitude = state.movementCurrentAltitude ?: originPreset?.altitude
    val originSourceLabel = when {
        state.simulationState != SimulationState.Running -> "Current device position"
        state.movementCurrentLatitude != null && state.movementCurrentLongitude != null -> "Current live coordinate"
        else -> "Active preset anchor"
    }
    val availabilityMessage = state.movementStartBlockedReason ?: if (state.simulationState == SimulationState.Running) {
        "Movement is ready. The route will start from the active live session and stream toward the selected destination."
    } else {
        "Movement is ready. Start will capture the current device location, launch a GPS session, and move toward the selected destination."
    }
    val selectedMode = state.moveForm.transportMode
    val selectedDestination = state.moveDestinationPreset

    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "Origin",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ConsoleBadge(
                text = originSourceLabel,
                highlighted = state.hasLiveMovementOrigin,
            )
        }
        Text(
            text = originPreset?.name ?: if (state.simulationState == SimulationState.Running) "No live origin available" else "Current device location",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (state.simulationState == SimulationState.Running) {
                "Uses the active simulation coordinate as the route origin."
            } else {
                "Starts from the device's current real location."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LabeledValue(
            label = "Coordinates",
            value = formatMoveCoordinates(
                latitude = originLatitude,
                longitude = originLongitude,
                altitude = originAltitude,
            ),
        )
        originPreset?.summary?.let { summary ->
            LabeledValue(label = "Preset context", value = summary)
        }
    }

    MoveDestinationPicker(
        presets = state.presets,
        selectedDestination = selectedDestination,
        activeOriginPresetId = state.activePresetId,
        onDestinationSelected = onDestinationSelected,
    )

    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        ConsoleSectionHeader(
            eyebrow = "Transport",
            title = "Movement mode",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.compact),
        ) {
            MoveTransportOption.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onTransportModeSelected(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag(GpStickTestTags.moveTransportControl(mode)),
                )
            }
        }
    }

    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        ConsoleSectionHeader(
            eyebrow = "Playback speed",
            title = formatMoveSpeed(selectedMode, state.moveForm.speedMetersPerSecond.toDouble()),
            description = "Adjust how quickly the route should advance once playback begins. The range adapts to the chosen transport mode.",
            trailing = {
                ConsoleBadge(
                    text = selectedMode.label,
                    highlighted = true,
                )
            },
        )
        Slider(
            value = state.moveForm.speedMetersPerSecond,
            onValueChange = onSpeedChanged,
            valueRange = selectedMode.minSpeedMetersPerSecond..selectedMode.maxSpeedMetersPerSecond,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GpStickTestTags.MOVE_SPEED_CONTROL),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMoveSpeedValue(selectedMode.minSpeedMetersPerSecond.toDouble()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMoveSpeedValue(selectedMode.maxSpeedMetersPerSecond.toDouble()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    ConsolePanelCard(
        containerColor = if (state.canStartMovement) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Text(
            text = availabilityMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.canStartMovement) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
        )
    }

    ConsolePanelCard(
        containerColor = if (state.canStartMovement) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
    ) {
        ConsoleSectionHeader(
            eyebrow = if (state.canStartMovement) "Ready to route" else "Start unavailable",
            title = "Start movement",
            description = if (state.canStartMovement) {
                "This sends the selected destination, transport mode, and playback speed to the foreground runtime."
            } else {
                "Resolve the availability note above before launching route playback."
            },
        )
        Button(
            onClick = onStartMovement,
            enabled = state.canStartMovement,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GpStickTestTags.MOVE_START_CONTROL),
            contentPadding = PaddingValues(
                horizontal = GpStickSpacing.card,
                vertical = GpStickSpacing.card,
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro),
            ) {
                Text(
                    text = "Start movement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = selectedDestination?.name ?: "Choose a saved destination first",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MoveDestinationPicker(
    presets: List<PresetUiModel>,
    selectedDestination: PresetUiModel?,
    activeOriginPresetId: String?,
    onDestinationSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        ConsoleSectionHeader(
            eyebrow = "Destination",
            title = selectedDestination?.name ?: "Select a preset destination",
            description = selectedDestination?.summary ?: "Choose any saved preset as the route destination.",
        )
        Box {
            FilledTonalButton(
                onClick = { expanded = true },
                enabled = presets.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(GpStickTestTags.MOVE_DESTINATION_CONTROL),
            ) {
                Text(
                    text = if (selectedDestination == null) {
                        "Browse saved destinations"
                    } else {
                        "Change destination"
                    },
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro)) {
                                Text(text = preset.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (preset.id == activeOriginPresetId) {
                                        "Current live origin · ${preset.summary}"
                                    } else {
                                        preset.summary
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onDestinationSelected(preset.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MovementProgressPanel(
    state: GpStickUiState,
    onCancelMovement: () -> Unit,
) {
    val statusLabel = when (state.movementPhase) {
        MovementPhase.Routing -> "Routing"
        MovementPhase.Moving -> "Moving"
        MovementPhase.Arrived -> "Arrived"
        MovementPhase.Canceled -> "Canceled"
        MovementPhase.None -> "Idle"
    }
    val progressValue = state.movementProgress.toFloat().coerceIn(0f, 1f)
    val originName = state.movementOriginPreset?.name ?: state.activePreset?.name ?: "Live origin"
    val destinationName = state.movementDestinationPreset?.name ?: "Destination pending"
    val routeMode = state.movementTransportMode?.toMoveTransportOption() ?: state.moveForm.transportMode
    val playbackSpeed = state.movementSpeedMetersPerSecond.takeIf { it > 0.0 }
        ?: state.moveForm.speedMetersPerSecond.toDouble()

    ConsolePanelCard(
        containerColor = if (state.movementPhase == MovementPhase.Arrived) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        ConsoleSectionHeader(
            eyebrow = when (state.movementPhase) {
                MovementPhase.Routing -> "Route setup"
                MovementPhase.Moving -> "Movement active"
                MovementPhase.Arrived -> "Arrival complete"
                else -> "Movement"
            },
            title = when (state.movementPhase) {
                MovementPhase.Routing -> "Building route to $destinationName"
                MovementPhase.Moving -> "Moving toward $destinationName"
                MovementPhase.Arrived -> "Arrived at $destinationName"
                else -> "Movement session"
            },
            description = when (state.movementPhase) {
                MovementPhase.Routing -> "The runtime is requesting geometry and preparing playback for the live session."
                MovementPhase.Moving -> "The live simulation is advancing along the routed path."
                MovementPhase.Arrived -> "The runtime will return to the planner shortly with the destination applied as the new active preset."
                else -> "Movement progress will appear here while a route is active."
            },
            trailing = {
                ConsoleBadge(text = statusLabel, highlighted = true)
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            LabeledValue(
                label = "Origin",
                value = originName,
                modifier = Modifier.weight(1f),
            )
            LabeledValue(
                label = "Destination",
                value = destinationName,
                modifier = Modifier.weight(1f),
            )
        }
    }

    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            LabeledValue(
                label = "Status",
                value = statusLabel,
                modifier = Modifier
                    .weight(1f)
                    .testTag(GpStickTestTags.MOVE_PROGRESS_STATUS),
            )
            LabeledValue(
                label = "Transport",
                value = routeMode.label,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            LabeledValue(
                label = "ETA",
                value = formatMoveEta(state.movementPhase, state.movementEtaEpochMillis),
                modifier = Modifier.weight(1f),
            )
            LabeledValue(
                label = "Playback speed",
                value = formatMoveSpeed(routeMode, playbackSpeed),
                modifier = Modifier.weight(1f),
            )
        }
        if (state.movementPhase == MovementPhase.Routing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Requesting route geometry and preparing the first live point…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                progress = if (state.movementPhase == MovementPhase.Arrived) 1f else progressValue,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (state.movementPhase == MovementPhase.Arrived) {
                    "Route complete"
                } else {
                    "${(progressValue * 100f).roundToInt()}% complete"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        LabeledValue(
            label = "Current coordinates",
            value = formatMoveCoordinates(
                latitude = state.movementCurrentLatitude,
                longitude = state.movementCurrentLongitude,
                altitude = state.movementCurrentAltitude,
            ),
        )
    }

    if (state.canCancelMovement) {
        Button(
            onClick = onCancelMovement,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GpStickTestTags.MOVE_CANCEL_CONTROL),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            contentPadding = PaddingValues(
                horizontal = GpStickSpacing.card,
                vertical = GpStickSpacing.card,
            ),
        ) {
            Text(
                text = "Cancel movement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
    val primaryActionEnabled = if (isRunning) canStop else canStart
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
    val primaryActionDescription = when {
        isRunning -> "Stop the active simulation and return the device to its idle QA state."
        !state.permissionsReady -> "Grant the required permissions before starting the simulation."
        highlightedPreset == null -> "Select a preset from Presets to enable Start."
        !state.canStartSimulation -> "Review the staged options before starting the simulation."
        else -> "Start applies the selected preset and the current staged options from the Options tab."
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
        ConsolePanelCard(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ) {
            ConsoleSectionHeader(
                eyebrow = if (isRunning) "Primary action" else "Ready to launch",
                title = if (isRunning) "Stop simulation" else "Start simulation",
                description = primaryActionDescription,
            )
            Button(
                onClick = if (isRunning) onStop else onStart,
                enabled = primaryActionEnabled,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isRunning) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    disabledContainerColor = if (isRunning) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    disabledContentColor = if (isRunning) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
                contentPadding = PaddingValues(
                    horizontal = GpStickSpacing.card,
                    vertical = GpStickSpacing.card,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GpStickSpacing.hero * 2)
                    .testTag(
                        if (isRunning) {
                            GpStickTestTags.STOP_CONTROL
                        } else {
                            GpStickTestTags.START_CONTROL
                        },
                    ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro),
                ) {
                    Text(
                        text = if (isRunning) "Stop" else "Start",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isRunning) {
                            "End the active simulation run"
                        } else {
                            "Launch the selected preset"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
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
                    .heightIn(min = 56.dp)
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
                    .heightIn(min = 56.dp)
                    .testTag(GpStickTestTags.CAPTURE_CURRENT_STATE_CONTROL),
            ) {
                Text("Capture")
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
    onApplyNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = state.simulationState == SimulationState.Running
    val applyNowDescription = when {
        !state.pendingSettingsKeepAnyMockFeatureEnabled -> {
            "Apply now stays disabled if the staged settings would turn off every mock feature. Keep GPS, Wi-Fi, or Cell enabled before applying live changes."
        }

        !state.hasPendingSettingsChanges -> {
            "Apply now stays disabled until the staged settings differ from the live simulation."
        }

        else -> {
            "Apply now updates the active simulation session immediately and keeps the current live preset in place."
        }
    }

    ConsolePanelCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ConsoleSectionHeader(
            eyebrow = "Staged controls",
            title = "Simulation options",
            description = if (isRunning) {
                "Changes here are saved as pending settings. Press Apply now to send them to the active simulation. It stays disabled when nothing has changed or when the staged setup would disable every mock feature."
            } else {
                "Changes here are saved as pending settings. They are applied the next time you press Start from the Status tab."
            },
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
        if (isRunning) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(
                    text = applyNowDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onApplyNow,
                    enabled = state.canApplyPendingSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(GpStickTestTags.APPLY_NOW_CONTROL),
                ) {
                    Text("Apply now")
                }
            }
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
        HelpCallout(text = "Use Status to start or stop the simulation. Options change what will be applied on the next start, or use Apply now during a live run.")
        HelpCallout(
            text = "Need a new profile? Capture current state and then save in the editor.",
            highlighted = true,
        )
        HelpCallout(text = "Made by Youngin, just for you.")
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

private fun MovementTransportMode.toMoveTransportOption(): MoveTransportOption = when (this) {
    MovementTransportMode.Walk -> MoveTransportOption.Walk
    MovementTransportMode.Drive -> MoveTransportOption.Drive
    MovementTransportMode.Transit -> MoveTransportOption.Transit
    MovementTransportMode.Cycle -> MoveTransportOption.Drive
}

private fun formatMoveSpeed(
    mode: MoveTransportOption,
    speedMetersPerSecond: Double,
): String {
    return "${formatMoveSpeedValue(speedMetersPerSecond)} · ${mode.detail}"
}

private fun formatMoveSpeedValue(speedMetersPerSecond: Double): String =
    "${(speedMetersPerSecond * 3.6).roundToInt()} km/h"

private fun formatMoveEta(
    phase: MovementPhase,
    etaEpochMillis: Long,
): String {
    return when {
        phase == MovementPhase.Arrived -> "Complete"
        etaEpochMillis <= 0L -> {
            if (phase == MovementPhase.Routing) "Calculating route…" else "Resolving live ETA…"
        }

        else -> {
            val remainingMillis = (etaEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val totalSeconds = remainingMillis / 1_000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            if (minutes > 0L) "$minutes m $seconds s remaining" else "$seconds s remaining"
        }
    }
}

private fun formatMoveCoordinates(
    latitude: Double?,
    longitude: Double?,
    altitude: Double?,
): String {
    if (latitude == null || longitude == null) {
        return "Waiting for live coordinates…"
    }

    val altitudeSegment = altitude?.let { " · Alt ${it.formatAltitude()} m" } ?: ""
    return "${latitude.formatPreciseCoordinate()}, ${longitude.formatPreciseCoordinate()}$altitudeSegment"
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

private fun Double.formatPreciseCoordinate(): String = String.format(Locale.US, "%.5f", this)

private fun Double.formatAltitude(): String = String.format(Locale.US, "%.1f", this)
