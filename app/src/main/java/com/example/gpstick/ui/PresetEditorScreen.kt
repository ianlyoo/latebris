package com.example.gpstick.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Scaffold(
        modifier = Modifier.testTag(GpStickTestTags.PRESET_EDITOR_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isNew) "Create preset" else "Edit preset",
                        style = MaterialTheme.typography.headlineSmall,
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GpStickSpacing.screen, vertical = GpStickSpacing.section),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.section),
        ) {
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
                Text(
                    text = validationMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
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

@Composable
private fun PresetDetailsSection(
    state: PresetEditorUiState,
    onNameChanged: (String) -> Unit,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onAltitudeChanged: (String) -> Unit,
) {
    SectionCard(title = "Preset details") {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            OutlinedTextField(
                value = state.latitude,
                onValueChange = onLatitudeChanged,
                modifier = Modifier.weight(1f),
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.longitude,
                onValueChange = onLongitudeChanged,
                modifier = Modifier.weight(1f),
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = state.altitude,
            onValueChange = onAltitudeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Altitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
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
        title = "Wi-Fi networks",
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(GpStickSpacing.card),
                    verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
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
                    OutlinedTextField(
                        value = row.ssid,
                        onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Ssid, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("SSID") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = row.bssid,
                        onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Bssid, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("BSSID") },
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                    ) {
                        OutlinedTextField(
                            value = row.level,
                            onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Level, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Level") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = row.frequency,
                            onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Frequency, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Frequency") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }
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
        title = "Cell towers",
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(GpStickSpacing.card),
                    verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
                ) {
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
}

@Composable
private fun SectionCard(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    action?.invoke()
                }
                content()
            },
        )
    }
}

@Composable
private fun NumberField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
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
