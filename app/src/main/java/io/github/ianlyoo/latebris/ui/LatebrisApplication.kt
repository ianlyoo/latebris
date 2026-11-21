package io.github.ianlyoo.latebris.ui

import android.app.Application

class LatebrisApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.resetStaleSimulationState()
    }
}
