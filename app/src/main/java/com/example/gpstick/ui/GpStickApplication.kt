package com.example.gpstick.ui

import android.app.Application

class GpStickApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.resetStaleSimulationState()
    }
}
