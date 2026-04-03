package com.example.gpstick.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.gpstick.ui.theme.GpStickSpacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: GpStickUiState,
    onPresetSelected: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCreatePreset: () -> Unit,
    onEditPreset: (String) -> Unit,
) {
    val presetsEditable = state.simulationState == SimulationState.Stopped

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GPStick QA Console",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GpStickSpacing.screen, vertical = GpStickSpacing.section),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.section),
        ) {
            StatusPanel(state = state)
            PresetPanel(
                modifier = Modifier.weight(1f),
                presets = state.presets,
                selectedPresetId = state.selectedPresetId,
                onPresetSelected = onPresetSelected,
                onCreatePreset = onCreatePreset,
                onEditPreset = onEditPreset,
                enabled = presetsEditable,
            )
            ControlPanel(
                canStart = state.selectedPreset != null && presetsEditable,
                canStop = state.simulationState == SimulationState.Running,
                onStart = onStart,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun StatusPanel(state: GpStickUiState) {
    val isRunning = state.simulationState == SimulationState.Running
    val highlightedPreset = if (isRunning) state.activePreset else state.selectedPreset
    val coordinates = if (isRunning) {
        highlightedPreset?.let {
            "${it.latitude.formatCoordinate()}, ${it.longitude.formatCoordinate()}"
        } ?: "Unavailable"
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            LabeledValue(
                label = "State",
                value = if (isRunning) "Running" else "Stopped",
                modifier = Modifier.testTag(GpStickTestTags.SIMULATION_STATUS),
            )
            LabeledValue(
                label = if (isRunning) "Active preset" else "Selected preset",
                value = highlightedPreset?.name ?: "None selected",
                modifier = Modifier.testTag(GpStickTestTags.SELECTED_PRESET),
            )
            if (coordinates != null) {
                LabeledValue(
                    label = "Active coordinates",
                    value = coordinates,
                    modifier = Modifier.testTag(GpStickTestTags.ACTIVE_PRESET_COORDINATES),
                )
            }
        }
    }
}

@Composable
private fun PresetPanel(
    modifier: Modifier = Modifier,
    presets: List<PresetUiModel>,
    selectedPresetId: String?,
    onPresetSelected: (String) -> Unit,
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
                .fillMaxSize()
                .padding(top = GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = GpStickSpacing.card),
                verticalArrangement = Arrangement.spacedBy(GpStickSpacing.compact),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Preset list",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FilledTonalButton(
                        onClick = onCreatePreset,
                        enabled = enabled,
                        modifier = Modifier.testTag(GpStickTestTags.NEW_PRESET_CONTROL),
                    ) {
                        Text("New preset")
                    }
                }
                Text(
                    text = if (enabled) {
                        "Select a simulation profile or open the editor to create and tune presets."
                    } else {
                        "Preset selection and editing stay locked while the simulation is running."
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
    canStart: Boolean,
    canStop: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(GpStickSpacing.card),
            verticalArrangement = Arrangement.spacedBy(GpStickSpacing.stack),
        ) {
            Text(
                text = "Service controls",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Start or stop the simulation service without leaving the dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
