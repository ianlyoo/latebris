package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing
import io.github.ianlyoo.latebris.ui.theme.LocalSignColors

enum class DashboardTab(val label: String) {
    Status("Status"),
    Move("Move"),
    Presets("Presets"),
    Options("Options"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: LatebrisUiState,
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val isRunning = state.simulationState == SimulationState.Running
    val canStart = state.selectedPreset != null &&
        !isRunning &&
        state.permissionsReady &&
        state.canStartSimulation

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Latebris", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (isRunning) {
                        LivePlate(modifier = Modifier.padding(end = LatebrisSpacing.lg))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            DashboardNavigationBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
        },
        floatingActionButton = {
            if (selectedTab == DashboardTab.Presets && !isRunning) {
                NewPresetButton(onClick = onCreatePreset)
            }
        },
    ) { innerPadding ->
        val content = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (selectedTab) {
            DashboardTab.Status -> StatusTab(
                state = state,
                canStart = canStart,
                onStart = onStart,
                onStop = onStop,
                onRequestPermissions = onRequestPermissions,
                modifier = content.testTag(LatebrisTestTags.DASHBOARD_STATUS_PANEL),
            )

            DashboardTab.Move -> MoveTab(
                state = state,
                onDestinationSelected = onMoveDestinationSelected,
                onTransportModeSelected = onMoveTransportModeSelected,
                onSpeedChanged = onMoveSpeedChanged,
                onStartMovement = onStartMovement,
                onCancelMovement = onCancelMovement,
                modifier = content.testTag(LatebrisTestTags.DASHBOARD_MOVE_PANEL),
            )

            DashboardTab.Presets -> PresetsTab(
                state = state,
                editable = !isRunning,
                onPresetSelected = onPresetSelected,
                onCaptureCurrentState = onCaptureCurrentState,
                onEditPreset = onEditPreset,
                modifier = content.testTag(LatebrisTestTags.DASHBOARD_PRESETS_PANEL),
            )

            DashboardTab.Options -> OptionsTab(
                state = state,
                onFeaturesEnabledChanged = onFeaturesEnabledChanged,
                onGpsMockEnabledChanged = onGpsMockEnabledChanged,
                onWifiMockEnabledChanged = onWifiMockEnabledChanged,
                onCellMockEnabledChanged = onCellMockEnabledChanged,
                onMovementSimulationEnabledChanged = onMovementSimulationEnabledChanged,
                onApplyNow = onApplyNow,
                modifier = content.testTag(LatebrisTestTags.DASHBOARD_OPTIONS_PANEL),
            )
        }
    }
}

@Composable
private fun DashboardNavigationBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
) {
    val signs = LocalSignColors.current
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        DashboardTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { tab.Icon() },
                label = { Text(tab.label) },
                modifier = Modifier.testTag(tab.testTag),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = signs.onYellow,
                    indicatorColor = signs.yellow,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun DashboardTab.Icon() {
    when (this) {
        DashboardTab.Status -> PictogramIcon(Pictogram.Gps)
        DashboardTab.Move -> PictogramIcon(Pictogram.Arrow)
        DashboardTab.Presets -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
        DashboardTab.Options -> Icon(Icons.Filled.Settings, contentDescription = null)
    }
}

private val DashboardTab.testTag: String
    get() = when (this) {
        DashboardTab.Status -> LatebrisTestTags.DASHBOARD_TAB_STATUS
        DashboardTab.Move -> LatebrisTestTags.DASHBOARD_TAB_MOVE
        DashboardTab.Presets -> LatebrisTestTags.DASHBOARD_TAB_PRESETS
        DashboardTab.Options -> LatebrisTestTags.DASHBOARD_TAB_OPTIONS
    }
