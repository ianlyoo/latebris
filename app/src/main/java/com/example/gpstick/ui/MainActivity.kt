package com.example.gpstick.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.core.app.ActivityCompat
import com.example.gpstick.service.RuntimePermissionGate

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as GpStickApplication).appContainer

    private var pendingPermissionRationale: Map<String, Boolean> = emptyMap()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        syncRuntimePermissionState()
        maybeOpenPermissionSettings(permissions)
    }

    private val viewModel: GpStickViewModel by viewModels {
        viewModelFactory {
            initializer {
                GpStickViewModel(
                    presetRepository = container.presetRepository,
                    serviceController = container.foregroundServiceController,
                    simulationStateStore = container.simulationStateStore,
                    deviceStateCaptureRepository = container.deviceStateCaptureRepository,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container.resetStaleSimulationState()
        syncRuntimePermissionState()

        setContent {
            GpStickApp(
                viewModel = viewModel,
                onRequestPermissions = ::requestRuntimePermissions,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        syncRuntimePermissionState()
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            if (!RuntimePermissionGate.hasLocationPermission(this@MainActivity)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!RuntimePermissionGate.hasNotificationPermission(this@MainActivity)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isEmpty()) {
            syncRuntimePermissionState()
            return
        }

        pendingPermissionRationale = permissions.associateWith { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun syncRuntimePermissionState() {
        viewModel.updateRuntimePermissionState(
            locationPermissionGranted = RuntimePermissionGate.hasLocationPermission(this),
            notificationPermissionGranted = RuntimePermissionGate.hasNotificationPermission(this),
            notificationPermissionRequired = RuntimePermissionGate.isNotificationPermissionRequired(),
        )
    }


    private fun maybeOpenPermissionSettings(permissionResults: Map<String, Boolean>) {
        if (permissionResults.isEmpty()) {
            return
        }

        val deniedPermanently = permissionResults.entries
            .filter { !it.value }
            .any { entry ->
                val permission = entry.key
                val shouldShowRationaleBeforeRequest = pendingPermissionRationale[permission] == true
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission) && shouldShowRationaleBeforeRequest
            }

        if (deniedPermanently) {
            showOpenSettingsDialog()
        }

        pendingPermissionRationale = emptyMap()
    }

    private fun showOpenSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission required")
            .setMessage("Some permissions were denied permanently. Open app settings to grant them and continue using location simulation.")
            .setPositiveButton("Open settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
