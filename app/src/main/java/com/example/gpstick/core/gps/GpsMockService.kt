package com.example.gpstick.core.gps

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetManager
import com.example.gpstick.service.SimulationStateStore
import com.example.gpstick.service.ServiceCommandFactory
import com.example.gpstick.service.SimulationNotificationFactory
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

class GpsMockService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(SystemClock.elapsedRealtime())

    private lateinit var locationManager: LocationManager
    private lateinit var notificationFactory: SimulationNotificationFactory
    private var simulatedLatitude = 0.0
    private var simulatedLongitude = 0.0
    private var movementInitialized = false

    private val injectionRunnable = object : Runnable {
        override fun run() {
            if (!isInjecting) {
                return
            }

            injectCurrentPresetLocation()
            scheduleNextInjection()
        }
    }

    private var isInjecting = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        notificationFactory = SimulationNotificationFactory(this)
        notificationFactory.ensureChannel()
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        registerTestProviders()
        enableFusedMockMode()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceCommandFactory.ACTION_START_GPS_MOCK -> startGpsMocking(startId)
            ServiceCommandFactory.ACTION_STOP_GPS_MOCK -> stopSelf(startId)
            null -> restoreGpsMocking(startId)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isInjecting = false
        handler.removeCallbacks(injectionRunnable)
        clearTestProviders()
        if (hasLocationPermission()) {
            disableFusedMockMode()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startGpsMocking(startId: Int) {
        val activePreset = resolveActivePreset()
        if (activePreset == null) {
            stopSelf(startId)
            return
        }

        val sharedState = SimulationStateStore.readFromProvider(this)
        if (!sharedState.activeFeaturesEnabled || !sharedState.activeGpsMockEnabled) {
            stopSelf(startId)
            return
        }

        startForeground(
            SimulationNotificationFactory.NOTIFICATION_ID,
            notificationFactory.buildActiveNotification(activePreset, sharedState),
        )
        isInjecting = true
        handler.removeCallbacks(injectionRunnable)
        injectCurrentPresetLocation()
        scheduleNextInjection()
    }

    private fun restoreGpsMocking(startId: Int) {
        val sharedState = SimulationStateStore.readFromProvider(this)
        if (!sharedState.isRunning || !sharedState.activeGpsMockEnabled || !sharedState.activeFeaturesEnabled) {
            stopSelf(startId)
            return
        }

        startGpsMocking(startId)
    }

    private fun scheduleNextInjection() {
        val delayMillis = random.nextLong(1_000L, 3_001L)
        handler.postDelayed(injectionRunnable, delayMillis)
    }

    private fun injectCurrentPresetLocation() {
        val preset = resolveActivePreset() ?: return
        val sharedState = SimulationStateStore.readFromProvider(this)
        if (!sharedState.activeFeaturesEnabled || !sharedState.activeGpsMockEnabled) {
            return
        }
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        val gpsLocation = createLocation(LocationManager.GPS_PROVIDER, preset)
        val networkLocation = createLocation(LocationManager.NETWORK_PROVIDER, preset)

        runCatching {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLocation)
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, networkLocation)
        }
        
        if (sharedState.activeFeaturesEnabled && sharedState.activeGpsMockEnabled) {
            pushFusedMockLocation(gpsLocation)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            SimulationNotificationFactory.NOTIFICATION_ID,
            notificationFactory.buildActiveNotification(preset, sharedState),
        )
    }

    private fun createLocation(provider: String, preset: LocationPreset): Location {
        val sharedState = SimulationStateStore.readFromProvider(this)
        val isMovementEnabled = sharedState.activeMovementSimulationEnabled &&
            sharedState.activeFeaturesEnabled &&
            sharedState.activeGpsMockEnabled
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

    private fun registerTestProviders() {
        ensureTestProvider(LocationManager.GPS_PROVIDER)
        ensureTestProvider(LocationManager.NETWORK_PROVIDER)
    }

    private fun ensureTestProvider(provider: String) {
        runCatching {
            locationManager.removeTestProvider(provider)
        }
        runCatching {
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
        }
        runCatching {
            locationManager.setTestProviderEnabled(provider, true)
        }
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

    private fun resolveActivePreset(): LocationPreset? {
        PresetManager.getActivePreset()?.let { return it }

        val sharedState = SimulationStateStore.readFromProvider(this)
        val presetId = sharedState.activePresetId ?: return null
        PresetManager.initialize(this)
        return PresetManager.getPreset(presetId)?.also(PresetManager::activatePreset)
    }

    @SuppressLint("MissingPermission")
    private fun enableFusedMockMode() {
        runCatching {
            LocationServices.getFusedLocationProviderClient(this).setMockMode(true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun disableFusedMockMode() {
        runCatching {
            LocationServices.getFusedLocationProviderClient(this).setMockMode(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun pushFusedMockLocation(location: Location) {
        runCatching {
            LocationServices.getFusedLocationProviderClient(this)
                .setMockLocation(location)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }
}
