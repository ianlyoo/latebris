package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.ianlyoo.latebris.data.ApiKeyStore
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing

@Composable
internal fun OptionsTab(
    state: LatebrisUiState,
    onFeaturesEnabledChanged: (Boolean) -> Unit,
    onGpsMockEnabledChanged: (Boolean) -> Unit,
    onWifiMockEnabledChanged: (Boolean) -> Unit,
    onCellMockEnabledChanged: (Boolean) -> Unit,
    onMovementSimulationEnabledChanged: (Boolean) -> Unit,
    onApplyNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = state.simulationState == SimulationState.Running
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LatebrisSpacing.screen)
            .padding(bottom = LatebrisSpacing.xxl),
    ) {
        Text(
            text = if (isRunning) {
                "Changes reach the running simulation when you press Apply now."
            } else {
                "Changes apply the next time you press Start."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = LatebrisSpacing.sm),
        )
        SectionHeading(text = "Signals")
        SwitchLine(
            label = "Simulation features",
            detail = "Turns every signal below on or off together",
            checked = state.pendingFeaturesEnabled,
            onCheckedChange = onFeaturesEnabledChanged,
            testTag = LatebrisTestTags.FEATURES_ENABLED_TOGGLE,
        )
        SwitchLine(
            label = "GPS position",
            detail = "Mock location on any phone; hook path for apps in scope",
            checked = state.pendingGpsMockEnabled,
            onCheckedChange = onGpsMockEnabledChanged,
            enabled = state.pendingFeaturesEnabled,
            testTag = LatebrisTestTags.GPS_MOCK_ENABLED_TOGGLE,
        )
        SwitchLine(
            label = "Wi-Fi scan",
            detail = "Hook path only: the preset's access points",
            checked = state.pendingWifiMockEnabled,
            onCheckedChange = onWifiMockEnabledChanged,
            enabled = state.pendingFeaturesEnabled,
            testTag = LatebrisTestTags.WIFI_MOCK_ENABLED_TOGGLE,
        )
        SwitchLine(
            label = "LTE cells",
            detail = "Hook path only: the preset's cell list",
            checked = state.pendingCellMockEnabled,
            onCheckedChange = onCellMockEnabledChanged,
            enabled = state.pendingFeaturesEnabled,
            testTag = LatebrisTestTags.CELL_MOCK_ENABLED_TOGGLE,
        )
        SwitchLine(
            label = "GPS drift",
            detail = "Small random steps around the preset point",
            checked = state.pendingMovementSimulationEnabled,
            onCheckedChange = onMovementSimulationEnabledChanged,
            enabled = state.pendingFeaturesEnabled,
            testTag = LatebrisTestTags.MOVEMENT_SIMULATION_ENABLED_TOGGLE,
            showDivider = false,
        )
        if (isRunning) {
            Text(
                text = when {
                    !state.pendingSettingsKeepAnyMockFeatureEnabled ->
                        "Keep GPS, Wi-Fi or cells on to apply: a run with every signal off is not allowed."
                    !state.hasPendingSettingsChanges -> "Nothing has changed since this run started."
                    else -> "Apply now sends these settings to the running simulation and keeps its preset."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = LatebrisSpacing.md),
            )
            Button(
                onClick = onApplyNow,
                enabled = state.canApplyPendingSettings,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(top = LatebrisSpacing.sm)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(LatebrisTestTags.APPLY_NOW_CONTROL),
            ) {
                Text("Apply now")
            }
        }
        ApiKeysSection()
        HowItWorksSection()
    }
}

@Composable
private fun SwitchLine(
    label: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = LatebrisSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.lg),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Optional keys for the two web services; see [ApiKeyStore]. */
@Composable
private fun ApiKeysSection() {
    val context = LocalContext.current
    val keyStore = remember(context) { ApiKeyStore(context) }
    var routeKey by remember { mutableStateOf(keyStore.enteredRouteKey()) }
    var cellKey by remember { mutableStateOf(keyStore.enteredCellKey()) }
    var saved by remember { mutableStateOf(false) }

    SectionHeading(text = "Web service keys")
    Text(
        text = "Both are optional and stay on this device. Move asks OpenRouteService for a road route; " +
            "without a key the route is a straight line. Auto Fill asks OpenCellID for nearby cells; without a key it is skipped.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
        modifier = Modifier.padding(top = LatebrisSpacing.md),
        verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm),
    ) {
        OutlinedTextField(
            value = routeKey,
            onValueChange = { routeKey = it; saved = false },
            label = { Text("OpenRouteService key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = cellKey,
            onValueChange = { cellKey = it; saved = false },
            label = { Text("OpenCellID key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        FilledTonalButton(
            onClick = { keyStore.save(routeKey, cellKey); saved = true },
            enabled = !saved,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(if (saved) "Keys saved" else "Save keys")
        }
    }
}

@Composable
private fun HowItWorksSection() {
    SectionHeading(text = "How it works")
    Column(verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.md)) {
        listOf(
            "Pick a preset in Presets, then press Start on Status. Stop returns the real location and signals.",
            "On a phone without root only GPS changes. Choose Latebris as the mock location app in Developer options.",
            "With Magisk, Zygisk and LSPosed, enable the Latebris module, add the app you test to its scope and reboot. " +
                "Apps in the scope get the preset's GPS, Wi-Fi scan and LTE cells; every other app keeps its real values.",
            "Capture saves what this device sees now as a new preset. Move plays a route from your position to a saved preset.",
        ).forEach { paragraph ->
            Text(text = paragraph, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "Made by Youngin, just for you.",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = LatebrisSpacing.sm),
        )
    }
}
