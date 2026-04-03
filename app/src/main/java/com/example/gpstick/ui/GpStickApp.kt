package com.example.gpstick.ui

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
import com.example.gpstick.ui.theme.GpStickTheme

private enum class GpStickRoute {
    Dashboard,
    PresetEditor,
}

internal fun sanitizeGpStickRouteName(raw: String?): String =
    GpStickRoute.entries.firstOrNull { it.name == raw }?.name ?: GpStickRoute.Dashboard.name

internal fun sanitizeDashboardTabName(raw: String?): String =
    DashboardTab.entries.firstOrNull { it.name == raw }?.name ?: DashboardTab.Presets.name

@Composable
fun GpStickApp(
    viewModel: GpStickViewModel,
    onRequestPermissions: () -> Unit = {},
) {
    var route by rememberSaveable { mutableStateOf(GpStickRoute.Dashboard.name) }
    var dashboardTab by rememberSaveable { mutableStateOf(DashboardTab.Presets.name) }
    val context = LocalContext.current
    val resolvedRoute = sanitizeGpStickRouteName(route)
    val resolvedDashboardTab = sanitizeDashboardTabName(dashboardTab)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GpStickUiEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    GpStickTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (GpStickRoute.valueOf(resolvedRoute)) {
                GpStickRoute.Dashboard -> {
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
