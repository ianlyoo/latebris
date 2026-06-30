package com.example.gpstick.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.example.gpstick.ui.theme.GpStickSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorScreen(
    state: PresetEditorUiState,
    onNavigateBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onAltitudeChanged: (String) -> Unit,
    onAddWifiNetwork: () -> Unit,
    onUpdateWifiNetwork: (Int, WifiNetworkField, String) -> Unit,
    onRemoveWifiNetwork: (Int) -> Unit,
    onAddCellTower: () -> Unit,
    onUpdateCellTower: (Int, CellTowerField, String) -> Unit,
    onRemoveCellTower: (Int) -> Unit,
    onAutoFill: () -> Unit,
    isAutoFillInProgress: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    BackHandler(onBack = onNavigateBack)
    Scaffold(
        modifier = Modifier.testTag(GpStickTestTags.PRESET_EDITOR_SCREEN),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(GpStickSpacing.micro)) {
                        Text(
                            text = if (state.isNew) "Create preset" else "Edit preset",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Tune coordinates, Wi-Fi data, and cell rows in one consistent workspace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag(GpStickTestTags.PRESET_EDITOR_BACK),
                    ) {
                        Text("Back")
                    }
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GpStickSpacing.screen, vertical = GpStickSpacing.section),
                verticalArrangement = Arrangement.spacedBy(GpStickSpacing.section),
            ) {
                EditorOverviewCard(state = state)
                PresetDetailsSection(
                    state = state,
                    onNameChanged = onNameChanged,
                    onLatitudeChanged = onLatitudeChanged,
                    onLongitudeChanged = onLongitudeChanged,
                    onAltitudeChanged = onAltitudeChanged,
                )
                WifiNetworksSection(
                    wifiNetworks = state.wifiNetworks,
                    onAddWifiNetwork = onAddWifiNetwork,
                    onUpdateWifiNetwork = onUpdateWifiNetwork,
                    onRemoveWifiNetwork = onRemoveWifiNetwork,
                )
                CellTowersSection(
                    cellTowers = state.cellTowers,
                    canAutoFill = state.canAutoFill,
                    onAddCellTower = onAddCellTower,
                    onUpdateCellTower = onUpdateCellTower,
                    onRemoveCellTower = onRemoveCellTower,
                    onAutoFill = onAutoFill,
                    isAutoFillInProgress = isAutoFillInProgress,
                )
                state.validationMessage?.let { validationMessage ->
                    ConsolePanelCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = validationMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
                    Button(
                        onClick = onSave,
                        enabled = state.isSaveEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(GpStickTestTags.SAVE_PRESET_CONTROL),
                    ) {
                        Text(if (state.isNew) "Create" else "Save")
                    }
                    if (state.canDelete) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(GpStickTestTags.DELETE_PRESET_CONTROL),
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorOverviewCard(state: PresetEditorUiState) {
    ConsolePanelCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ConsoleSectionHeader(
            eyebrow = "Editor",
            title = if (state.isNew) "New preset draft" else "Preset workspace",
            description = if (state.isNew) {
                "Start with coordinates, then add surrounding Wi-Fi or cell data before saving."
            } else {
                "Review and refine the preset details without leaving the editor flow."
            },
            trailing = {
                ConsoleBadge(
                    text = if (state.isSaveEnabled) "Ready to save" else "Needs review",
                    highlighted = state.isSaveEnabled,
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            EditorMetric(
                modifier = Modifier.weight(1f),
                label = "Wi-Fi rows",
                value = state.wifiNetworks.size.toString(),
            )
            EditorMetric(
                modifier = Modifier.weight(1f),
                label = "Cell rows",
                value = state.cellTowers.size.toString(),
            )
            EditorMetric(
                modifier = Modifier.weight(1f),
                label = "Mode",
                value = if (state.isNew) "Create" else "Edit",
            )
        }
    }
}

@Composable
private fun EditorMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    ConsolePanelCard(
        modifier = modifier,
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
private fun PresetDetailsSection(
    state: PresetEditorUiState,
    onNameChanged: (String) -> Unit,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onAltitudeChanged: (String) -> Unit,
) {
    SectionCard(
        eyebrow = "Coordinates",
        title = "Preset details",
        description = "Define the location core before layering in captured network context.",
    ) {
        EditorTextField(
            value = state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = "Name",
            testTag = GpStickTestTags.PRESET_NAME_FIELD,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            EditorTextField(
                value = state.latitude,
                onValueChange = onLatitudeChanged,
                modifier = Modifier.weight(1f),
                label = "Latitude",
                keyboardType = KeyboardType.Text,
                testTag = GpStickTestTags.PRESET_LATITUDE_FIELD,
            )
            EditorTextField(
                value = state.longitude,
                onValueChange = onLongitudeChanged,
                modifier = Modifier.weight(1f),
                label = "Longitude",
                keyboardType = KeyboardType.Text,
                testTag = GpStickTestTags.PRESET_LONGITUDE_FIELD,
            )
        }
        EditorTextField(
            value = state.altitude,
            onValueChange = onAltitudeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = "Altitude",
            keyboardType = KeyboardType.Text,
            testTag = GpStickTestTags.PRESET_ALTITUDE_FIELD,
        )
    }
}

@Composable
private fun WifiNetworksSection(
    wifiNetworks: List<WifiNetworkEditorUiState>,
    onAddWifiNetwork: () -> Unit,
    onUpdateWifiNetwork: (Int, WifiNetworkField, String) -> Unit,
    onRemoveWifiNetwork: (Int) -> Unit,
) {
    SectionCard(
        eyebrow = "Radio context",
        title = "Wi-Fi networks",
        description = "Add one or more nearby networks to make the preset feel grounded in a real environment.",
        action = {
            TextButton(
                onClick = onAddWifiNetwork,
                modifier = Modifier.testTag(GpStickTestTags.ADD_WIFI_ROW_CONTROL),
            ) {
                Text("Add row")
            }
        },
    ) {
        if (wifiNetworks.isEmpty()) {
            EmptySectionText(text = "No Wi-Fi networks added yet.")
        }

        wifiNetworks.forEachIndexed { index, row ->
            ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Network ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TextButton(
                        onClick = { onRemoveWifiNetwork(index) },
                        modifier = Modifier.testTag(GpStickTestTags.removeWifiRowControl(index)),
                    ) {
                        Text("Remove")
                    }
                }
                EditorTextField(
                    value = row.ssid,
                    onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Ssid, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "SSID",
                )
                EditorTextField(
                    value = row.bssid,
                    onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Bssid, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "BSSID",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
                    EditorTextField(
                        value = row.level,
                        onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Level, it) },
                        modifier = Modifier.weight(1f),
                        label = "Level",
                        keyboardType = KeyboardType.Text,
                    )
                    EditorTextField(
                        value = row.frequency,
                        onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Frequency, it) },
                        modifier = Modifier.weight(1f),
                        label = "Frequency",
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
        }
    }
}

@Composable
private fun CellTowersSection(
    cellTowers: List<CellTowerEditorUiState>,
    canAutoFill: Boolean,
    onAddCellTower: () -> Unit,
    onUpdateCellTower: (Int, CellTowerField, String) -> Unit,
    onRemoveCellTower: (Int) -> Unit,
    onAutoFill: () -> Unit,
    isAutoFillInProgress: Boolean,
) {
    SectionCard(
        eyebrow = "Carrier context",
        title = "Cell towers",
        description = "Layer in tower metadata manually or pull a quick auto-fill where supported.",
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.compact)) {
                TextButton(
                    onClick = onAddCellTower,
                    modifier = Modifier.testTag(GpStickTestTags.ADD_CELL_ROW_CONTROL),
                ) {
                    Text("Add row")
                }
                TextButton(
                    onClick = onAutoFill,
                    enabled = canAutoFill,
                    modifier = Modifier.testTag(GpStickTestTags.AUTO_FILL_CONTROL),
                ) {
                    Text(if (isAutoFillInProgress) "Filling…" else "Auto Fill")
                }
            }
        },
    ) {
        if (cellTowers.isEmpty()) {
            EmptySectionText(text = "No cell towers added yet.")
        }

        cellTowers.forEachIndexed { index, row ->
            ConsolePanelCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Tower ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TextButton(
                        onClick = { onRemoveCellTower(index) },
                        modifier = Modifier.testTag(GpStickTestTags.removeCellRowControl(index)),
                    ) {
                        Text("Remove")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = row.mcc,
                        onValueChange = { onUpdateCellTower(index, CellTowerField.Mcc, it) },
                        label = "MCC",
                    )
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = row.mnc,
                        onValueChange = { onUpdateCellTower(index, CellTowerField.Mnc, it) },
                        label = "MNC",
                    )
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = row.ci,
                        onValueChange = { onUpdateCellTower(index, CellTowerField.Ci, it) },
                        label = "CI",
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = row.pci,
                        onValueChange = { onUpdateCellTower(index, CellTowerField.Pci, it) },
                        label = "PCI",
                    )
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = row.tac,
                        onValueChange = { onUpdateCellTower(index, CellTowerField.Tac, it) },
                        label = "TAC",
                    )
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = row.earfcn,
                        onValueChange = { onUpdateCellTower(index, CellTowerField.Earfcn, it) },
                        label = "EARFCN",
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ConsolePanelCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ConsoleSectionHeader(
            title = title,
            eyebrow = eyebrow,
            description = description,
            trailing = action,
        )
        content()
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = if (testTag == null) modifier else modifier.testTag(testTag),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = consoleOutlinedTextFieldColors(),
    )
}

@Composable
private fun NumberField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    EditorTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun EmptySectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
