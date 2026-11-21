package io.github.ianlyoo.latebris.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ianlyoo.latebris.ui.theme.Figures
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing

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
        modifier = Modifier.testTag(LatebrisTestTags.PRESET_EDITOR_SCREEN),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New preset" else "Edit preset") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag(LatebrisTestTags.PRESET_EDITOR_BACK),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = state.isSaveEnabled,
                        modifier = Modifier
                            .padding(end = LatebrisSpacing.sm)
                            .testTag(LatebrisTestTags.SAVE_PRESET_CONTROL),
                    ) {
                        Text(text = if (state.isNew) "Create" else "Save", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LatebrisSpacing.screen)
                .padding(bottom = LatebrisSpacing.xxl),
        ) {
            state.validationMessage?.let { message ->
                Notice(text = message, tone = NoticeTone.Problem, modifier = Modifier.padding(top = LatebrisSpacing.sm))
            }

            SectionHeading(text = "Place")
            Column(verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                EditorField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    label = "Name",
                    testTag = LatebrisTestTags.PRESET_NAME_FIELD,
                    figures = false,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                    EditorField(
                        value = state.latitude,
                        onValueChange = onLatitudeChanged,
                        label = "Latitude",
                        testTag = LatebrisTestTags.PRESET_LATITUDE_FIELD,
                        modifier = Modifier.weight(1f),
                    )
                    EditorField(
                        value = state.longitude,
                        onValueChange = onLongitudeChanged,
                        label = "Longitude",
                        testTag = LatebrisTestTags.PRESET_LONGITUDE_FIELD,
                        modifier = Modifier.weight(1f),
                    )
                }
                EditorField(
                    value = state.altitude,
                    onValueChange = onAltitudeChanged,
                    label = "Altitude (m)",
                    testTag = LatebrisTestTags.PRESET_ALTITUDE_FIELD,
                )
            }

            SectionHeading(text = "Wi-Fi networks") {
                TextButton(
                    onClick = onAddWifiNetwork,
                    modifier = Modifier.testTag(LatebrisTestTags.ADD_WIFI_ROW_CONTROL),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add", modifier = Modifier.padding(start = LatebrisSpacing.xs))
                }
            }
            Text(
                text = "Returned by WifiManager scans in apps in the LSPosed scope.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = LatebrisSpacing.sm),
            )
            if (state.wifiNetworks.isEmpty()) {
                EmptyLine("No Wi-Fi networks. Scans in scope will return an empty list.")
            }
            Column(verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                state.wifiNetworks.forEachIndexed { index, row ->
                    EntryGroup(
                        title = row.ssid.ifBlank { "Network ${index + 1}" },
                        pictogram = Pictogram.Wifi,
                        onRemove = { onRemoveWifiNetwork(index) },
                        removeLabel = "Remove network ${index + 1}",
                        removeTag = LatebrisTestTags.removeWifiRowControl(index),
                    ) {
                        EditorField(
                            value = row.ssid,
                            onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Ssid, it) },
                            label = "SSID",
                            figures = false,
                        )
                        EditorField(
                            value = row.bssid,
                            onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Bssid, it) },
                            label = "BSSID",
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                            EditorField(
                                value = row.level,
                                onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Level, it) },
                                label = "Level (dBm)",
                                modifier = Modifier.weight(1f),
                            )
                            EditorField(
                                value = row.frequency,
                                onValueChange = { onUpdateWifiNetwork(index, WifiNetworkField.Frequency, it) },
                                label = "Frequency (MHz)",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            SectionHeading(text = "LTE cells") {
                TextButton(
                    onClick = onAddCellTower,
                    modifier = Modifier.testTag(LatebrisTestTags.ADD_CELL_ROW_CONTROL),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add", modifier = Modifier.padding(start = LatebrisSpacing.xs))
                }
            }
            Text(
                text = "Returned by getAllCellInfo() in apps in the LSPosed scope. Auto Fill looks up cells near the coordinates on OpenCellID.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onAutoFill,
                enabled = state.canAutoFill,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(vertical = LatebrisSpacing.sm)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(LatebrisTestTags.AUTO_FILL_CONTROL),
            ) {
                if (isAutoFillInProgress) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = LatebrisSpacing.sm).heightIn(max = 18.dp), strokeWidth = 2.dp)
                    Text("Looking up cells…")
                } else {
                    PictogramIcon(Pictogram.Cell, size = 18.dp)
                    Text("Auto Fill", modifier = Modifier.padding(start = LatebrisSpacing.sm))
                }
            }
            if (state.cellTowers.isEmpty()) {
                EmptyLine("No cells. Apps in scope will see no cell information.")
            }
            Column(verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                state.cellTowers.forEachIndexed { index, row ->
                    EntryGroup(
                        title = if (row.ci.isNotBlank()) "Cell ${row.ci}" else "Cell ${index + 1}",
                        pictogram = Pictogram.Cell,
                        onRemove = { onRemoveCellTower(index) },
                        removeLabel = "Remove cell ${index + 1}",
                        removeTag = LatebrisTestTags.removeCellRowControl(index),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                            CellField(row.mcc, "MCC", Modifier.weight(1f)) { onUpdateCellTower(index, CellTowerField.Mcc, it) }
                            CellField(row.mnc, "MNC", Modifier.weight(1f)) { onUpdateCellTower(index, CellTowerField.Mnc, it) }
                            CellField(row.tac, "TAC", Modifier.weight(1f)) { onUpdateCellTower(index, CellTowerField.Tac, it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                            CellField(row.ci, "CI", Modifier.weight(1f)) { onUpdateCellTower(index, CellTowerField.Ci, it) }
                            CellField(row.pci, "PCI", Modifier.weight(1f)) { onUpdateCellTower(index, CellTowerField.Pci, it) }
                            CellField(row.earfcn, "EARFCN", Modifier.weight(1f)) { onUpdateCellTower(index, CellTowerField.Earfcn, it) }
                        }
                    }
                }
            }

            if (state.canDelete) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .padding(top = LatebrisSpacing.sectionGap)
                        .heightIn(min = 48.dp)
                        .testTag(LatebrisTestTags.DELETE_PRESET_CONTROL),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("Delete preset", modifier = Modifier.padding(start = LatebrisSpacing.sm))
                }
            }
        }
    }
}

@Composable
private fun EntryGroup(
    title: String,
    pictogram: Pictogram,
    onRemove: () -> Unit,
    removeLabel: String,
    removeTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = LatebrisSpacing.md, end = LatebrisSpacing.md, bottom = LatebrisSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PictogramIcon(pictogram, size = 18.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = LatebrisSpacing.sm),
                )
                IconButton(onClick = onRemove, modifier = Modifier.testTag(removeTag)) {
                    Icon(Icons.Filled.Close, contentDescription = removeLabel)
                }
            }
            content()
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String? = null,
    figures: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = if (figures) MaterialTheme.typography.bodyLarge.merge(Figures) else MaterialTheme.typography.bodyLarge,
        modifier = if (testTag == null) modifier else modifier.testTag(testTag),
    )
}

@Composable
private fun CellField(value: String, label: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    EditorField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardType = KeyboardType.Number,
        modifier = modifier,
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = LatebrisSpacing.sm),
    )
}
