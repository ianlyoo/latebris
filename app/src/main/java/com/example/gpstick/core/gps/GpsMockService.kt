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
import com.example.gpstick.service.ServiceCommandFactory
import com.example.gpstick.service.SimulationNotificationFactory
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

class GpsMockService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(SystemClock.elapsedRealtime())

    private lateinit var locationManager: LocationManager
    private lateinit var notificationFactory: SimulationNotificationFactory

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
        }
        return START_NOT_STICKY
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
        val activePreset = PresetManager.getActivePreset()
        if (activePreset == null) {
            stopSelf(startId)
            return
        }

        startForeground(
            SimulationNotificationFactory.NOTIFICATION_ID,
            notificationFactory.buildActiveNotification(activePreset),
        )
        isInjecting = true
        handler.removeCallbacks(injectionRunnable)
        injectCurrentPresetLocation()
        scheduleNextInjection()
    }

    private fun scheduleNextInjection() {
        val delayMillis = random.nextLong(1_000L, 3_001L)
        handler.postDelayed(injectionRunnable, delayMillis)
    }

    private fun injectCurrentPresetLocation() {
        val preset = PresetManager.getActivePreset() ?: return
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

        pushFusedMockLocation(gpsLocation)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            SimulationNotificationFactory.NOTIFICATION_ID,
            notificationFactory.buildActiveNotification(preset),
        )
    }

    private fun createLocation(provider: String, preset: LocationPreset): Location {
        val latitudeNoise = randomSignedNoise()
        val longitudeNoise = randomSignedNoise()

        return Location(provider).apply {
            latitude = preset.gps.latitude + latitudeNoise
            longitude = preset.gps.longitude + longitudeNoise
            altitude = preset.gps.altitude
            accuracy = random.nextDouble(3.0, 15.0001).toFloat()
            bearing = 0f
            speed = 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    private fun randomSignedNoise(): Double {
        val magnitude = random.nextDouble(0.00001, 0.0000301)
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
