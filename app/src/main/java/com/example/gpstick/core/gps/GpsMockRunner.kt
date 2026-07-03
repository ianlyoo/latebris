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
import com.example.gpstick.service.SimulationFeatureSettings
import com.example.gpstick.service.routing.RoutePoint
import com.google.android.gms.location.LocationServices
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class MovementProgressSnapshot(
    val progress: Double,
    val etaEpochMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
)

data class MovementRouteSpec(
    val geometry: List<RoutePoint>,
    val speedMetersPerSecond: Double,
    val fallbackDurationSeconds: Double,
)

class GpsMockRunner(
    context: Context,
    private val onRuntimeFailure: () -> Unit = {},
    private val onMovementProgress: (MovementProgressSnapshot) -> Unit = {},
    private val onMovementArrived: (MovementProgressSnapshot) -> Unit = {},
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
    private var activeSettings = SimulationFeatureSettings()
    private var staticOverride: RoutePoint? = null
    private var routePlayback: RoutePlaybackState? = null
    private var forceStaticCoordinate = false

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
        settings: SimulationFeatureSettings,
    ): Boolean {
        stop()
        if (!settings.featuresEnabled || !settings.isGpsMockEnabled || !hasLocationPermission()) {
            return false
        }

        activePreset = preset
        activeSettings = settings
        movementInitialized = false
        routePlayback = null
        staticOverride = null
        forceStaticCoordinate = false

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
        routePlayback = null
        staticOverride = null
        forceStaticCoordinate = false
        clearTestProviders()
        if (hasLocationPermission()) {
            disableFusedMockMode()
        }
    }

    fun updateSettings(settings: SimulationFeatureSettings) {
        activeSettings = settings
    }

    fun startRoutePlayback(spec: MovementRouteSpec): Boolean {
        if (!isInjecting || !activeSettings.featuresEnabled || !activeSettings.isGpsMockEnabled) {
            return false
        }
        if (spec.geometry.size < 2) {
            return false
        }

        val cumulativeDistances = mutableListOf(0.0)
        var totalDistance = 0.0
        for (index in 1 until spec.geometry.size) {
            val segmentDistance = distanceMeters(spec.geometry[index - 1], spec.geometry[index])
            totalDistance += segmentDistance
            cumulativeDistances += totalDistance
        }

        routePlayback = RoutePlaybackState(
            points = spec.geometry,
            cumulativeDistancesMeters = cumulativeDistances,
            totalDistanceMeters = totalDistance,
            speedMetersPerSecond = spec.speedMetersPerSecond.coerceAtLeast(0.0),
            fallbackDurationSeconds = spec.fallbackDurationSeconds.coerceAtLeast(0.0),
            startedAtElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
        )
        movementInitialized = false
        staticOverride = null
        forceStaticCoordinate = false
        return true
    }

    fun cancelRoutePlayback(): RoutePoint? {
        val route = routePlayback ?: return staticOverride
        val coordinate = routeCoordinateAtElapsed(route, SystemClock.elapsedRealtime()).point
        routePlayback = null
        staticOverride = coordinate
        forceStaticCoordinate = true
        return coordinate
    }

    fun currentCoordinateOrNull(): RoutePoint? {
        val route = routePlayback
        return when {
            route != null -> routeCoordinateAtElapsed(route, SystemClock.elapsedRealtime()).point
            staticOverride != null -> staticOverride
            movementInitialized -> RoutePoint(
                latitude = simulatedLatitude,
                longitude = simulatedLongitude,
                altitude = activePreset?.gps?.altitude ?: 0.0,
            )

            else -> activePreset?.let {
                RoutePoint(
                    latitude = it.gps.latitude,
                    longitude = it.gps.longitude,
                    altitude = it.gps.altitude,
                )
            }
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

        val coordinate = resolveCoordinate(preset)
        val gpsLocation = createLocation(LocationManager.GPS_PROVIDER, coordinate)
        val networkLocation = createLocation(LocationManager.NETWORK_PROVIDER, coordinate)

        val providerInjectionSucceeded = runCatching {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLocation)
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, networkLocation)
        }.isSuccess

        if (!providerInjectionSucceeded) {
            return false
        }

        return pushFusedMockLocation(gpsLocation)
    }

    private fun resolveCoordinate(preset: LocationPreset): InjectedCoordinate {
        val route = routePlayback
        if (route != null) {
            val nowElapsed = SystemClock.elapsedRealtime()
            val routeCoordinate = routeCoordinateAtElapsed(route, nowElapsed)
            val etaEpochMillis = routeCoordinate.etaEpochMillis
            val snapshot = MovementProgressSnapshot(
                progress = routeCoordinate.progress,
                etaEpochMillis = etaEpochMillis,
                latitude = routeCoordinate.point.latitude,
                longitude = routeCoordinate.point.longitude,
                altitude = routeCoordinate.point.altitude,
            )
            onMovementProgress(snapshot)

            if (routeCoordinate.progress >= 1.0) {
                routePlayback = null
                staticOverride = route.points.lastOrNull() ?: routeCoordinate.point
                forceStaticCoordinate = true
                onMovementArrived(snapshot.copy(progress = 1.0, etaEpochMillis = 0L))
            }

            return InjectedCoordinate(
                latitude = routeCoordinate.point.latitude,
                longitude = routeCoordinate.point.longitude,
                altitude = routeCoordinate.point.altitude,
                speedMetersPerSecond = route.speedMetersPerSecond.toFloat(),
                bearingDegrees = routeCoordinate.bearingDegrees,
            )
        }

        val isRandomDriftEnabled = activeSettings.isMovementSimulationEnabled &&
            activeSettings.featuresEnabled &&
            activeSettings.isGpsMockEnabled

        if (isRandomDriftEnabled && !forceStaticCoordinate) {
            val point = nextRandomMovementCoordinate(preset.gps.latitude, preset.gps.longitude)
            staticOverride = point
            return InjectedCoordinate(
                latitude = point.latitude,
                longitude = point.longitude,
                altitude = preset.gps.altitude,
                speedMetersPerSecond = 0f,
                bearingDegrees = 0f,
            )
        }

        movementInitialized = false
        val point = staticOverride ?: RoutePoint(
            latitude = preset.gps.latitude,
            longitude = preset.gps.longitude,
            altitude = preset.gps.altitude,
        )
        return InjectedCoordinate(
            latitude = point.latitude,
            longitude = point.longitude,
            altitude = point.altitude,
            speedMetersPerSecond = 0f,
            bearingDegrees = 0f,
        )
    }

    private fun createLocation(provider: String, coordinate: InjectedCoordinate): Location {
        return Location(provider).apply {
            latitude = coordinate.latitude
            longitude = coordinate.longitude
            altitude = coordinate.altitude
            accuracy = random.nextDouble(3.0, 15.0001).toFloat()
            bearing = coordinate.bearingDegrees
            speed = coordinate.speedMetersPerSecond
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    private fun nextRandomMovementCoordinate(baseLatitude: Double, baseLongitude: Double): RoutePoint {
        if (!movementInitialized) {
            simulatedLatitude = baseLatitude
            simulatedLongitude = baseLongitude
            movementInitialized = true
        }

        val latitudeStep = randomSignedDistance(0.000001, 0.00003)
        val longitudeStep = randomSignedDistance(0.000001, 0.00003)
        simulatedLatitude += latitudeStep
        simulatedLongitude += longitudeStep
        return RoutePoint(
            latitude = simulatedLatitude,
            longitude = simulatedLongitude,
            altitude = activePreset?.gps?.altitude ?: 0.0,
        )
    }

    private fun routeCoordinateAtElapsed(
        route: RoutePlaybackState,
        nowElapsedRealtimeMillis: Long,
    ): RoutePlaybackCoordinate {
        val elapsedSeconds = (nowElapsedRealtimeMillis - route.startedAtElapsedRealtimeMillis).coerceAtLeast(0L) / 1_000.0
        val expectedDistance = when {
            route.speedMetersPerSecond > 0.0 -> elapsedSeconds * route.speedMetersPerSecond
            route.fallbackDurationSeconds > 0.0 -> {
                val ratio = (elapsedSeconds / route.fallbackDurationSeconds).coerceIn(0.0, 1.0)
                route.totalDistanceMeters * ratio
            }

            else -> route.totalDistanceMeters
        }

        val traveledDistance = expectedDistance.coerceIn(0.0, route.totalDistanceMeters)
        val progress = if (route.totalDistanceMeters <= 0.0) 1.0 else {
            (traveledDistance / route.totalDistanceMeters).coerceIn(0.0, 1.0)
        }
        val point = coordinateAtDistance(route, traveledDistance)
        val remainingDistance = (route.totalDistanceMeters - traveledDistance).coerceAtLeast(0.0)
        val etaEpochMillis = if (route.speedMetersPerSecond > 0.0) {
            System.currentTimeMillis() + (remainingDistance / route.speedMetersPerSecond * 1_000.0).toLong()
        } else if (route.fallbackDurationSeconds > 0.0) {
            val remainingSeconds = route.fallbackDurationSeconds * (1.0 - progress)
            System.currentTimeMillis() + (remainingSeconds * 1_000.0).toLong()
        } else {
            0L
        }

        return RoutePlaybackCoordinate(
            point = point.point,
            progress = progress,
            etaEpochMillis = if (progress >= 1.0) 0L else etaEpochMillis,
            bearingDegrees = point.bearingDegrees,
        )
    }

    private fun coordinateAtDistance(route: RoutePlaybackState, distanceMeters: Double): CoordinateWithBearing {
        if (distanceMeters <= 0.0) {
            val first = route.points.first()
            val second = route.points.getOrElse(1) { first }
            return CoordinateWithBearing(first, bearingDegrees(first, second))
        }
        if (distanceMeters >= route.totalDistanceMeters) {
            val last = route.points.last()
            val beforeLast = route.points.getOrElse(route.points.lastIndex - 1) { last }
            return CoordinateWithBearing(last, bearingDegrees(beforeLast, last))
        }

        val distances = route.cumulativeDistancesMeters
        var targetSegmentIndex = 1
        while (targetSegmentIndex < distances.size && distances[targetSegmentIndex] < distanceMeters) {
            targetSegmentIndex += 1
        }

        val segmentEndIndex = targetSegmentIndex.coerceAtMost(route.points.lastIndex)
        val segmentStartIndex = (segmentEndIndex - 1).coerceAtLeast(0)
        val segmentStartDistance = distances[segmentStartIndex]
        val segmentEndDistance = distances[segmentEndIndex]
        val segmentLength = (segmentEndDistance - segmentStartDistance).coerceAtLeast(1.0E-6)
        val ratio = ((distanceMeters - segmentStartDistance) / segmentLength).coerceIn(0.0, 1.0)

        val start = route.points[segmentStartIndex]
        val end = route.points[segmentEndIndex]
        val point = RoutePoint(
            latitude = start.latitude + (end.latitude - start.latitude) * ratio,
            longitude = start.longitude + (end.longitude - start.longitude) * ratio,
            altitude = start.altitude + (end.altitude - start.altitude) * ratio,
        )
        return CoordinateWithBearing(point, bearingDegrees(start, end))
    }

    private fun distanceMeters(from: RoutePoint, to: RoutePoint): Double {
        val result = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
        return result[0].toDouble()
    }

    private fun bearingDegrees(from: RoutePoint, to: RoutePoint): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        val theta = Math.toDegrees(atan2(y, x))
        val normalized = (theta + 360.0) % 360.0
        return normalized.toFloat()
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

    private data class InjectedCoordinate(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val speedMetersPerSecond: Float,
        val bearingDegrees: Float,
    )

    private data class RoutePlaybackState(
        val points: List<RoutePoint>,
        val cumulativeDistancesMeters: List<Double>,
        val totalDistanceMeters: Double,
        val speedMetersPerSecond: Double,
        val fallbackDurationSeconds: Double,
        val startedAtElapsedRealtimeMillis: Long,
    )

    private data class RoutePlaybackCoordinate(
        val point: RoutePoint,
        val progress: Double,
        val etaEpochMillis: Long,
        val bearingDegrees: Float,
    )

    private data class CoordinateWithBearing(
        val point: RoutePoint,
        val bearingDegrees: Float,
    )
}
