package io.github.ianlyoo.latebris.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.ianlyoo.latebris.ui.theme.LatebrisTheme

private enum class LatebrisRoute {
    Dashboard,
    PresetEditor,
}

internal fun sanitizeLatebrisRouteName(raw: String?): String =
    LatebrisRoute.entries.firstOrNull { it.name == raw }?.name ?: LatebrisRoute.Dashboard.name

internal fun sanitizeDashboardTabName(raw: String?): String =
    DashboardTab.entries.firstOrNull { it.name == raw }?.name ?: DashboardTab.Status.name

@Composable
fun LatebrisApp(
    viewModel: LatebrisViewModel,
    onRequestPermissions: () -> Unit = {},
) {
    var route by rememberSaveable { mutableStateOf(LatebrisRoute.Dashboard.name) }
    var dashboardTab by rememberSaveable { mutableStateOf(DashboardTab.Status.name) }
    val context = LocalContext.current
    val resolvedRoute = sanitizeLatebrisRouteName(route)
    val resolvedDashboardTab = sanitizeDashboardTabName(dashboardTab)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LatebrisUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                    viewModel.clearReplayedUiEvents()
                }
            }
        }
    }

    LatebrisTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (LatebrisRoute.valueOf(resolvedRoute)) {
                LatebrisRoute.Dashboard -> {
                    HomeScreen(
                        state = viewModel.uiState,
                        selectedTab = DashboardTab.valueOf(resolvedDashboardTab),
                        onTabSelected = { tab -> dashboardTab = tab.name },
                        onPresetSelected = viewModel::selectPreset,
                        onStart = viewModel::startSimulation,
                        onStop = viewModel::stopSimulation,
                        onRequestPermissions = onRequestPermissions,
                        onCaptureCurrentState = {
                            viewModel.captureCurrentDeviceState { captured ->
                                if (captured) {
                                    route = LatebrisRoute.PresetEditor.name
                                }
                            }
                        },
                        onFeaturesEnabledChanged = viewModel::setFeaturesEnabled,
                        onGpsMockEnabledChanged = viewModel::setGpsMockEnabled,
                        onWifiMockEnabledChanged = viewModel::setWifiMockEnabled,
                        onCellMockEnabledChanged = viewModel::setCellMockEnabled,
                        onMovementSimulationEnabledChanged = viewModel::setMovementSimulationEnabled,
                        onMoveDestinationSelected = viewModel::selectMoveDestination,
                        onMoveTransportModeSelected = viewModel::selectMoveTransportMode,
                        onMoveSpeedChanged = viewModel::updateMoveSpeed,
                        onStartMovement = viewModel::startMovement,
                        onCancelMovement = viewModel::cancelMovement,
                        onApplyNow = viewModel::applyPendingOptions,
                        onCreatePreset = {
                            viewModel.openPresetEditor(presetId = null)
                            route = LatebrisRoute.PresetEditor.name
                        },
                        onEditPreset = { presetId ->
                            viewModel.openPresetEditor(presetId)
                            route = LatebrisRoute.PresetEditor.name
                        },
                    )
                }

                LatebrisRoute.PresetEditor -> {
                    PresetEditorScreen(
                        state = viewModel.presetEditorState,
                        onNavigateBack = {
                            viewModel.closePresetEditor()
                            route = LatebrisRoute.Dashboard.name
                        },
                        onNameChanged = viewModel::updateEditorName,
                        onLatitudeChanged = viewModel::updateEditorLatitude,
                        onLongitudeChanged = viewModel::updateEditorLongitude,
                        onAltitudeChanged = viewModel::updateEditorAltitude,
                        onAddWifiNetwork = viewModel::addWifiNetworkRow,
                        onUpdateWifiNetwork = viewModel::updateWifiNetworkRow,
                        onRemoveWifiNetwork = viewModel::removeWifiNetworkRow,
                        onAddCellTower = viewModel::addCellTowerRow,
                        onUpdateCellTower = viewModel::updateCellTowerRow,
                        onRemoveCellTower = viewModel::removeCellTowerRow,
                        onAutoFill = viewModel::autoFillCellTowers,
                        isAutoFillInProgress = viewModel.presetEditorState.isAutoFillInProgress,
                        onSave = {
                            if (viewModel.savePresetEdits()) {
                                viewModel.closePresetEditor()
                                route = LatebrisRoute.Dashboard.name
                            }
                        },
                        onDelete = {
                            if (viewModel.deleteEditingPreset()) {
                                route = LatebrisRoute.Dashboard.name
                            }
                        },
                    )
                }
            }
        }
    }
}
