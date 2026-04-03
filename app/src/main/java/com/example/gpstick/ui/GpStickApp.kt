package com.example.gpstick.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gpstick.ui.theme.GpStickTheme

private enum class GpStickRoute {
    Dashboard,
    PresetEditor,
}

@Composable
fun GpStickApp(
    viewModel: GpStickViewModel,
    onRequestPermissions: () -> Unit = {},
) {
    var route by rememberSaveable { mutableStateOf(GpStickRoute.Dashboard.name) }
    var dashboardTab by rememberSaveable { mutableStateOf(DashboardTab.Presets.name) }

    GpStickTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (GpStickRoute.valueOf(route)) {
                GpStickRoute.Dashboard -> {
                    HomeScreen(
                        state = viewModel.uiState,
                        selectedTab = DashboardTab.valueOf(dashboardTab),
                        onTabSelected = { tab -> dashboardTab = tab.name },
                        onPresetSelected = viewModel::selectPreset,
                        onStart = viewModel::startSimulation,
                        onStop = viewModel::stopSimulation,
                        onRequestPermissions = onRequestPermissions,
                        onCaptureCurrentState = {
                            viewModel.captureCurrentDeviceState { captured ->
                                if (captured) {
                                    route = GpStickRoute.PresetEditor.name
                                }
                            }
                        },
                        onFeaturesEnabledChanged = viewModel::setFeaturesEnabled,
                        onGpsMockEnabledChanged = viewModel::setGpsMockEnabled,
                        onWifiMockEnabledChanged = viewModel::setWifiMockEnabled,
                        onCellMockEnabledChanged = viewModel::setCellMockEnabled,
                        onMovementSimulationEnabledChanged = viewModel::setMovementSimulationEnabled,
                        onCreatePreset = {
                            viewModel.openPresetEditor(presetId = null)
                            route = GpStickRoute.PresetEditor.name
                        },
                        onEditPreset = { presetId ->
                            viewModel.openPresetEditor(presetId)
                            route = GpStickRoute.PresetEditor.name
                        },
                    )
                }

                GpStickRoute.PresetEditor -> {
                    PresetEditorScreen(
                        state = viewModel.presetEditorState,
                        onNavigateBack = {
                            viewModel.closePresetEditor()
                            route = GpStickRoute.Dashboard.name
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
                                route = GpStickRoute.Dashboard.name
                            }
                        },
                        onDelete = {
                            if (viewModel.deleteEditingPreset()) {
                                route = GpStickRoute.Dashboard.name
                            }
                        },
                    )
                }
            }
        }
    }
}
