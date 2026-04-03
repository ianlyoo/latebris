package com.example.gpstick.core.gps

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.gpstick.data.preset.LocationPreset
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

class GpsMockRunner(
    context: Context,
    private val onRuntimeFailure: () -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(SystemClock.elapsedRealtime())
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var simulatedLatitude = 0.0
    private var simulatedLongitude = 0.0
    private var movementInitialized = false
    private var isInjecting = false
    private var activePreset: LocationPreset? = null
    private var activeSettings = com.example.gpstick.service.SimulationFeatureSettings()

    private val injectionRunnable = object : Runnable {
        override fun run() {
            if (!isInjecting) {
                return
            }

            if (!injectCurrentPresetLocation()) {
                stop()
                onRuntimeFailure()
                return
            }
            scheduleNextInjection()
        }
    }

    fun start(
        preset: LocationPreset,
        settings: com.example.gpstick.service.SimulationFeatureSettings,
    ): Boolean {
        stop()
        if (!settings.featuresEnabled || !settings.isGpsMockEnabled || !hasLocationPermission()) {
            return false
        }

        activePreset = preset
        activeSettings = settings
        movementInitialized = false
        if (!registerTestProviders()) {
            stop()
            return false
        }
        if (!enableFusedMockMode()) {
            stop()
            return false
        }
        isInjecting = true
        return injectCurrentPresetLocation().also { started ->
            if (started) {
                scheduleNextInjection()
            } else {
                stop()
            }
        }
    }

    fun stop() {
        isInjecting = false
        handler.removeCallbacks(injectionRunnable)
        activePreset = null
        movementInitialized = false
        clearTestProviders()
        if (hasLocationPermission()) {
            disableFusedMockMode()
        }
    }

    private fun scheduleNextInjection() {
        val delayMillis = random.nextLong(1_000L, 3_001L)
        handler.postDelayed(injectionRunnable, delayMillis)
    }

    private fun injectCurrentPresetLocation(): Boolean {
        val preset = activePreset ?: return false
        if (!activeSettings.featuresEnabled || !activeSettings.isGpsMockEnabled || !hasLocationPermission()) {
            return false
        }

        val gpsLocation = createLocation(LocationManager.GPS_PROVIDER, preset)
        val networkLocation = createLocation(LocationManager.NETWORK_PROVIDER, preset)

        val providerInjectionSucceeded = runCatching {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLocation)
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, networkLocation)
        }.isSuccess

        if (!providerInjectionSucceeded) {
            return false
        }

        return pushFusedMockLocation(gpsLocation)
    }

    private fun createLocation(provider: String, preset: LocationPreset): Location {
        val isMovementEnabled = activeSettings.isMovementSimulationEnabled &&
            activeSettings.featuresEnabled &&
            activeSettings.isGpsMockEnabled
        val coordinates = if (isMovementEnabled) {
            nextMovementCoordinate(preset.gps.latitude, preset.gps.longitude)
        } else {
            movementInitialized = false
            Pair(preset.gps.latitude, preset.gps.longitude)
        }

        return Location(provider).apply {
            latitude = coordinates.first
            longitude = coordinates.second
            altitude = preset.gps.altitude
            accuracy = random.nextDouble(3.0, 15.0001).toFloat()
            bearing = 0f
            speed = 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    private fun nextMovementCoordinate(baseLatitude: Double, baseLongitude: Double): Pair<Double, Double> {
        if (!movementInitialized) {
            simulatedLatitude = baseLatitude
            simulatedLongitude = baseLongitude
            movementInitialized = true
        }

        val latitudeStep = randomSignedDistance(0.000001, 0.00003)
        val longitudeStep = randomSignedDistance(0.000001, 0.00003)
        simulatedLatitude += latitudeStep
        simulatedLongitude += longitudeStep
        return Pair(simulatedLatitude, simulatedLongitude)
    }

    private fun randomSignedDistance(minMeters: Double, maxMeters: Double): Double {
        val magnitude = random.nextDouble(minMeters, maxMeters)
        return if (random.nextBoolean()) magnitude else -magnitude
    }

    private fun registerTestProviders(): Boolean {
        val gpsRegistered = ensureTestProvider(LocationManager.GPS_PROVIDER)
        val networkRegistered = ensureTestProvider(LocationManager.NETWORK_PROVIDER)
        return gpsRegistered && networkRegistered
    }

    private fun ensureTestProvider(provider: String): Boolean {
        runCatching {
            locationManager.removeTestProvider(provider)
        }
        val added = runCatching {
            locationManager.addTestProvider(
                provider,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE,
            )
        }.isSuccess
        val enabled = runCatching {
            locationManager.setTestProviderEnabled(provider, true)
        }.isSuccess
        return added && enabled
    }

    private fun clearTestProviders() {
        clearTestProvider(LocationManager.GPS_PROVIDER)
        clearTestProvider(LocationManager.NETWORK_PROVIDER)
    }

    private fun clearTestProvider(provider: String) {
        runCatching {
            locationManager.removeTestProvider(provider)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableFusedMockMode(): Boolean {
        return runCatching {
            LocationServices.getFusedLocationProviderClient(appContext).setMockMode(true)
        }.isSuccess
    }

    @SuppressLint("MissingPermission")
    private fun disableFusedMockMode() {
        runCatching {
            LocationServices.getFusedLocationProviderClient(appContext).setMockMode(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun pushFusedMockLocation(location: Location): Boolean {
        return runCatching {
            LocationServices.getFusedLocationProviderClient(appContext)
                .setMockLocation(location)
        }.isSuccess
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }
}
