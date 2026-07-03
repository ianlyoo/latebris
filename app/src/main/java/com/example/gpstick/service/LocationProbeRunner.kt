package com.example.gpstick.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class LocationProbeRunner(
    private val context: Context,
) {
    fun runProbe() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location probe skipped because location permission is missing.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        logLocation("lm:last:gps", runCatching {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull())
        logLocation("lm:last:network", runCatching {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull())
        logLocation("lm:last:passive", runCatching {
            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }.getOrNull())

        logLocation("lm:current:gps", awaitCurrentLocation(locationManager, LocationManager.GPS_PROVIDER))
        logLocation("lm:current:network", awaitCurrentLocation(locationManager, LocationManager.NETWORK_PROVIDER))
        logLocation("lm:update:gps", awaitSingleUpdate(locationManager, LocationManager.GPS_PROVIDER))
        logLocation("lm:update:network", awaitSingleUpdate(locationManager, LocationManager.NETWORK_PROVIDER))

        logLocation("fused:last", runCatching {
            Tasks.await(fusedClient.lastLocation, 10, TimeUnit.SECONDS)
        }.getOrNull())
        logLocation("fused:current", runCatching {
            val cancellation = CancellationTokenSource()
            Tasks.await(
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token),
                15,
                TimeUnit.SECONDS,
            )
        }.getOrNull())
    }

    private fun awaitCurrentLocation(
        locationManager: LocationManager,
        provider: String,
    ): Location? {
        val latch = CountDownLatch(1)
        var capturedLocation: Location? = null
        val executor = Executor { command -> command.run() }
        runCatching {
            locationManager.getCurrentLocation(
                provider,
                CancellationSignal(),
                executor,
            ) { location ->
                capturedLocation = location
                latch.countDown()
            }
        }.onFailure {
            Log.w(TAG, "getCurrentLocation failed for $provider", it)
            return null
        }
        latch.await(10, TimeUnit.SECONDS)
        return capturedLocation
    }

    private fun awaitSingleUpdate(
        locationManager: LocationManager,
        provider: String,
    ): Location? {
        val latch = CountDownLatch(1)
        var capturedLocation: Location? = null
        val executor = Executor { command -> command.run() }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                capturedLocation = location
                latch.countDown()
            }
        }
        runCatching {
            locationManager.requestLocationUpdates(
                provider,
                0L,
                0f,
                executor,
                listener,
            )
        }.onFailure {
            Log.w(TAG, "requestLocationUpdates failed for $provider", it)
            return null
        }
        latch.await(10, TimeUnit.SECONDS)
        runCatching {
            locationManager.removeUpdates(listener)
        }
        return capturedLocation
    }

    private fun logLocation(label: String, location: Location?) {
        if (location == null) {
            Log.i(TAG, "$label -> null")
            return
        }

        val isMock = runCatching { location.isMock }.getOrDefault(false)
        val isFromMockProvider = runCatching { location.isFromMockProvider }.getOrDefault(false)
        Log.i(
            TAG,
            "$label -> provider=${location.provider}," +
                " lat=${location.latitude}," +
                " lon=${location.longitude}," +
                " alt=${location.altitude}," +
                " acc=${location.accuracy}," +
                " mock=$isMock," +
                " fromMock=$isFromMockProvider," +
                " raw=$location",
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    companion object {
        const val TAG = "GpStickLocationProbe"
    }
}
