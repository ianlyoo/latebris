package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ianlyoo.latebris.data.ApiKeyStore
import io.github.ianlyoo.latebris.service.MovementPhase
import io.github.ianlyoo.latebris.service.MovementTransportMode
import io.github.ianlyoo.latebris.ui.theme.Figures
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing
import io.github.ianlyoo.latebris.ui.theme.LocalSignColors
import kotlin.math.roundToInt

@Composable
internal fun MoveTab(
    state: LatebrisUiState,
    onDestinationSelected: (String) -> Unit,
    onTransportModeSelected: (MoveTransportOption) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onStartMovement: () -> Unit,
    onCancelMovement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LatebrisSpacing.screen)
            .padding(top = LatebrisSpacing.sm, bottom = LatebrisSpacing.xxl),
    ) {
        if (state.showsMoveProgress) {
            RouteProgress(state = state, onCancelMovement = onCancelMovement)
        } else {
            RoutePlanner(
                state = state,
                onDestinationSelected = onDestinationSelected,
                onTransportModeSelected = onTransportModeSelected,
                onSpeedChanged = onSpeedChanged,
                onStartMovement = onStartMovement,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutePlanner(
    state: LatebrisUiState,
    onDestinationSelected: (String) -> Unit,
    onTransportModeSelected: (MoveTransportOption) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onStartMovement: () -> Unit,
) {
    val context = LocalContext.current
    val hasRouteKey = remember(context) { ApiKeyStore(context).routeKey().isNotBlank() }
    val signs = LocalSignColors.current
    val isRunning = state.simulationState == SimulationState.Running
    val destination = state.moveDestinationPreset
    val mode = state.moveForm.transportMode

    LocationSign {
        Text(text = "From", style = MaterialTheme.typography.labelLarge, color = signs.onInkMuted)
        Text(
            text = when {
                !isRunning -> "Current device location"
                else -> state.activePreset?.name ?: "No live origin yet"
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (isRunning) {
                formatPosition(
                    state.movementCurrentLatitude ?: state.activePreset?.latitude,
                    state.movementCurrentLongitude ?: state.activePreset?.longitude,
                    state.movementCurrentAltitude ?: state.activePreset?.altitude,
                )
            } else {
                "Read from the device when the route starts"
            },
            style = MaterialTheme.typography.bodySmall.merge(Figures),
            color = signs.onInkMuted,
        )
        PictogramIcon(
            pictogram = Pictogram.ArrowDown,
            tint = signs.yellow,
            size = 28.dp,
            modifier = Modifier.padding(vertical = LatebrisSpacing.sm),
        )
        Text(text = "To", style = MaterialTheme.typography.labelLarge, color = signs.onInkMuted)
        Text(
            text = destination?.name ?: "Choose a destination below",
            style = MaterialTheme.typography.titleLarge,
            color = if (destination != null) signs.yellow else signs.onInkMuted,
        )
        destination?.let {
            Text(
                text = formatCoordinates(it.latitude, it.longitude),
                style = MaterialTheme.typography.bodySmall.merge(Figures),
                color = signs.onInkMuted,
            )
        }
    }

    SectionHeading(text = "Destination")
    if (state.presets.isEmpty()) {
        Text(
            text = "No saved presets yet. Add one in Presets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(
        modifier = Modifier
            .selectableGroup()
            .testTag(LatebrisTestTags.MOVE_DESTINATION_CONTROL),
    ) {
        state.presets.forEachIndexed { index, preset ->
            val selected = preset.id == destination?.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = selected,
                        onClick = { onDestinationSelected(preset.id) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = LatebrisSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = LatebrisSpacing.md),
                ) {
                    Text(text = preset.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (isRunning && preset.id == state.activePresetId) {
                            "Live origin · ${formatCoordinates(preset.latitude, preset.longitude)}"
                        } else {
                            formatCoordinates(preset.latitude, preset.longitude)
                        },
                        style = MaterialTheme.typography.bodySmall.merge(Figures),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < state.presets.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    SectionHeading(text = "Travel mode")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        MoveTransportOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == mode,
                onClick = { onTransportModeSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = MoveTransportOption.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = signs.yellow,
                    activeContentColor = signs.onYellow,
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(LatebrisTestTags.moveTransportControl(option)),
            ) {
                Text(option.label)
            }
        }
    }

    SectionHeading(text = "Speed") {
        Text(
            text = formatSpeed(state.moveForm.speedMetersPerSecond.toDouble()),
            style = MaterialTheme.typography.titleMedium.merge(Figures),
        )
    }
    Text(
        text = mode.detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = state.moveForm.speedMetersPerSecond,
        onValueChange = onSpeedChanged,
        valueRange = mode.minSpeedMetersPerSecond..mode.maxSpeedMetersPerSecond,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LatebrisTestTags.MOVE_SPEED_CONTROL),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatSpeed(mode.minSpeedMetersPerSecond.toDouble()),
            style = MaterialTheme.typography.bodySmall.merge(Figures),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatSpeed(mode.maxSpeedMetersPerSecond.toDouble()),
            style = MaterialTheme.typography.bodySmall.merge(Figures),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(LatebrisSpacing.xl))
    Column(verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm)) {
        if (!hasRouteKey) {
            Notice(text = "No OpenRouteService key is saved in Options, so the route is a straight line.")
        }
        state.movementStartBlockedReason?.let { reason ->
            Notice(text = reason, tone = NoticeTone.Problem)
        }
    }
    Button(
        onClick = onStartMovement,
        enabled = state.canStartMovement,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(top = LatebrisSpacing.lg)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(LatebrisTestTags.MOVE_START_CONTROL),
    ) {
        Text(text = "Start movement", style = MaterialTheme.typography.titleMedium)
    }
    Text(
        text = if (isRunning) {
            "The route starts from the live position of the running simulation."
        } else {
            "Start reads the device's location, starts a GPS simulation there and moves toward the destination."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = LatebrisSpacing.sm),
    )
}

@Composable
private fun RouteProgress(
    state: LatebrisUiState,
    onCancelMovement: () -> Unit,
) {
    val signs = LocalSignColors.current
    val phase = state.movementPhase
    val destinationName = state.movementDestinationPreset?.name ?: "the destination"
    val progress = if (phase == MovementPhase.Arrived) 1f else state.movementProgress.toFloat().coerceIn(0f, 1f)
    val mode = state.movementTransportMode?.toOption() ?: state.moveForm.transportMode
    val speed = state.movementSpeedMetersPerSecond.takeIf { it > 0.0 } ?: state.moveForm.speedMetersPerSecond.toDouble()

    LocationSign {
        Text(
            text = phase.label,
            style = MaterialTheme.typography.labelLarge,
            color = signs.yellow,
            modifier = Modifier.testTag(LatebrisTestTags.MOVE_PROGRESS_STATUS),
        )
        Text(
            text = when (phase) {
                MovementPhase.Routing -> "Building the route to $destinationName"
                MovementPhase.Arrived -> "Arrived at $destinationName"
                else -> "Moving toward $destinationName"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(LatebrisSpacing.md))
        if (phase == MovementPhase.Routing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = signs.yellow,
                trackColor = signs.onInk.copy(alpha = 0.2f),
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = signs.yellow,
                trackColor = signs.onInk.copy(alpha = 0.2f),
                drawStopIndicator = {},
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = LatebrisSpacing.xs)) {
            Text(
                text = if (phase == MovementPhase.Routing) "Preparing" else "${(progress * 100f).roundToInt()} %",
                style = MaterialTheme.typography.bodyMedium.merge(Figures),
                color = signs.onInkMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatEta(phase, state.movementEtaEpochMillis),
                style = MaterialTheme.typography.bodyMedium.merge(Figures),
                color = signs.onInkMuted,
            )
        }
    }

    SectionHeading(text = "Route")
    DetailRow("From", state.movementOriginPreset?.name ?: state.activePreset?.name ?: "Live origin")
    DetailRow("To", state.movementDestinationPreset?.name ?: "Not set")
    DetailRow("Travel mode", mode.label)
    DetailRow("Speed", formatSpeed(speed))
    DetailRow(
        "Position",
        formatPosition(state.movementCurrentLatitude, state.movementCurrentLongitude, state.movementCurrentAltitude),
        showDivider = false,
    )
    if (phase == MovementPhase.Arrived) {
        Text(
            text = "The destination is now the active preset. Move returns to planning in a moment.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = LatebrisSpacing.sm),
        )
    }
    if (state.canCancelMovement) {
        OutlinedButton(
            onClick = onCancelMovement,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(top = LatebrisSpacing.xl)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(LatebrisTestTags.MOVE_CANCEL_CONTROL),
        ) {
            Text(text = "Cancel movement", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun DetailRow(label: String, value: String, showDivider: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = LatebrisSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.merge(Figures),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f),
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun MovementTransportMode.toOption(): MoveTransportOption = when (this) {
    MovementTransportMode.Walk -> MoveTransportOption.Walk
    MovementTransportMode.Drive -> MoveTransportOption.Drive
    MovementTransportMode.Transit -> MoveTransportOption.Transit
    MovementTransportMode.Cycle -> MoveTransportOption.Drive
}
