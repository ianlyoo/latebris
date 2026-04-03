package com.example.gpstick.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as GpStickApplication).appContainer

    private val viewModel: GpStickViewModel by viewModels {
        viewModelFactory {
            initializer {
                GpStickViewModel(
                    presetRepository = container.presetRepository,
                    serviceController = container.foregroundServiceController,
                    simulationStateStore = container.simulationStateStore,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container.resetStaleSimulationState()

        setContent {
            GpStickApp(viewModel = viewModel)
        }
    }
}
