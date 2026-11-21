package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.ianlyoo.latebris.ui.theme.Figures
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing
import io.github.ianlyoo.latebris.ui.theme.LocalSignColors

@Composable
internal fun PresetsTab(
    state: LatebrisUiState,
    editable: Boolean,
    onPresetSelected: (String) -> Unit,
    onCaptureCurrentState: () -> Unit,
    onEditPreset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag(LatebrisTestTags.PRESET_LIST),
        contentPadding = PaddingValues(
            start = LatebrisSpacing.screen,
            end = LatebrisSpacing.screen,
            top = LatebrisSpacing.sm,
            bottom = LatebrisSpacing.fabClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
                if (!editable) {
                    Notice(text = "Presets are locked while a simulation runs. Stop it on Status to change them.")
                }
                OutlinedButton(
                    onClick = onCaptureCurrentState,
                    enabled = editable,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(LatebrisTestTags.CAPTURE_CURRENT_STATE_CONTROL),
                ) {
                    PictogramIcon(Pictogram.Gps, size = 18.dp)
                    Text(
                        text = "Capture what this device sees now",
                        modifier = Modifier.padding(start = LatebrisSpacing.sm),
                    )
                }
                if (state.presets.isEmpty()) {
                    Text(
                        text = "No presets yet. Capture the current state or add one with New preset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = LatebrisSpacing.lg),
                    )
                }
            }
        }
        items(items = state.presets, key = { it.id }) { preset ->
            PresetRow(
                preset = preset,
                selected = preset.id == state.selectedPresetId,
                live = state.simulationState == SimulationState.Running && preset.id == state.activePresetId,
                enabled = editable,
                onSelect = { onPresetSelected(preset.id) },
                onEdit = { onEditPreset(preset.id) },
            )
        }
    }
}

@Composable
private fun PresetRow(
    preset: PresetUiModel,
    selected: Boolean,
    live: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    // Selection is a heavier border, never a fill: yellow stays reserved for the live plate.
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
                .padding(start = LatebrisSpacing.xs, end = LatebrisSpacing.xs, top = LatebrisSpacing.sm, bottom = LatebrisSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.padding(LatebrisSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = LatebrisSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (live) {
                        LivePlate(modifier = Modifier.padding(start = LatebrisSpacing.sm))
                    }
                }
                Text(
                    text = formatPosition(preset.latitude, preset.longitude, preset.altitude),
                    style = MaterialTheme.typography.bodySmall.merge(Figures),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preset.summary.isNotBlank()) {
                    Text(
                        text = preset.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = LatebrisSpacing.xs),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = LatebrisSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SignalCount(Pictogram.Gps, formatAccuracy(preset.accuracyMeters), "GPS accuracy ${formatAccuracy(preset.accuracyMeters)}")
                    SignalCount(Pictogram.Wifi, preset.wifiCount.toString(), "${preset.wifiCount} Wi-Fi networks")
                    SignalCount(Pictogram.Cell, preset.cellCount.toString(), "${preset.cellCount} LTE cells")
                }
            }
            IconButton(
                onClick = onEdit,
                enabled = enabled,
                modifier = Modifier.testTag(LatebrisTestTags.editPresetControl(preset.id)),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${preset.name}")
            }
        }
    }
}

@Composable
private fun SignalCount(pictogram: Pictogram, value: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.xs),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        PictogramIcon(pictogram, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.merge(Figures),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NewPresetButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("New preset") },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag(LatebrisTestTags.NEW_PRESET_CONTROL),
    )
}
