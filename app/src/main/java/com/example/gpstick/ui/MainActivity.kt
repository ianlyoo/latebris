package com.example.gpstick.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gpstick.service.RuntimePermissionGate

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as GpStickApplication).appContainer

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        syncRuntimePermissionState()
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

        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun syncRuntimePermissionState() {
        viewModel.updateRuntimePermissionState(
            locationPermissionGranted = RuntimePermissionGate.hasLocationPermission(this),
            notificationPermissionGranted = RuntimePermissionGate.hasNotificationPermission(this),
            notificationPermissionRequired = RuntimePermissionGate.isNotificationPermissionRequired(),
        )
    }
}
