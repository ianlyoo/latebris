package com.example.gpstick.service.routing

import com.example.gpstick.BuildConfig
import com.example.gpstick.core.network.JsonHttpClient
import com.example.gpstick.service.MovementTransportMode
import com.google.gson.Gson
import java.net.URL
import kotlin.math.ceil

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
)

data class RouteResult(
    val geometry: List<RoutePoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

class OpenRouteServiceClient internal constructor(
    private val http: JsonHttpClient,
    private val apiKey: () -> String,
    private val distanceBetween: (RoutePoint, RoutePoint) -> Double,
) {
    constructor() : this(Gson())

    constructor(gson: Gson = Gson()) : this(
        http = JsonHttpClient(gson),
        apiKey = { BuildConfig.OPEN_ROUTE_SERVICE_API_KEY },
        distanceBetween = { origin, destination ->
            val result = FloatArray(1)
            android.location.Location.distanceBetween(
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude,
                result,
            )
            result[0].toDouble()
        },
    )

    fun fetchRoute(
        originLatitude: Double,
        originLongitude: Double,
        originAltitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double,
        destinationAltitude: Double,
        mode: MovementTransportMode,
    ): RouteResult? {
        val fallbackRoute = buildFallbackRoute(
            originLatitude = originLatitude,
            originLongitude = originLongitude,
            originAltitude = originAltitude,
            destinationLatitude = destinationLatitude,
            destinationLongitude = destinationLongitude,
            destinationAltitude = destinationAltitude,
        )
        val key = apiKey().takeIf { it.isNotBlank() } ?: return fallbackRoute
        val profile = mode.toOrsProfile()
        val endpoint = "https://api.openrouteservice.org/v2/directions/$profile/geojson"

        val requestBody = OrsRouteRequest(
            coordinates = listOf(
                listOf(originLongitude, originLatitude),
                listOf(destinationLongitude, destinationLatitude),
            ),
            elevation = true,
        )

        return runCatching {
            http.post(
                URL(endpoint),
                requestBody,
                OrsRouteResponse::class.java,
                headers = mapOf("Authorization" to key),
            )?.toRouteResult()
        }.getOrNull() ?: fallbackRoute
    }

    private fun OrsRouteResponse.toRouteResult(): RouteResult? {
        val feature = features.orEmpty().firstOrNull() ?: return null
        val geometry = feature.geometry?.coordinates.orEmpty().mapNotNull { coordinate ->
            val longitude = coordinate.getOrNull(0) ?: return@mapNotNull null
            val latitude = coordinate.getOrNull(1) ?: return@mapNotNull null
            RoutePoint(
                latitude = latitude,
                longitude = longitude,
                altitude = coordinate.getOrNull(2) ?: 0.0,
            )
        }
        if (geometry.size < 2) {
            return null
        }
        val summary = feature.properties?.summary
        return RouteResult(
            geometry = geometry,
            distanceMeters = summary?.distance ?: 0.0,
            durationSeconds = summary?.duration ?: 0.0,
        )
    }

    private fun buildFallbackRoute(
        originLatitude: Double,
        originLongitude: Double,
        originAltitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double,
        destinationAltitude: Double,
    ): RouteResult? {
        val distanceMeters = distanceBetween(
            RoutePoint(originLatitude, originLongitude, originAltitude),
            RoutePoint(destinationLatitude, destinationLongitude, destinationAltitude),
        )

        if (distanceMeters <= 0.0) {
            return null
        }

        val segmentCount = ceil(distanceMeters / FALLBACK_SEGMENT_LENGTH_METERS)
            .toInt()
            .coerceIn(2, MAX_FALLBACK_SEGMENTS)
        val geometry = buildList(segmentCount + 1) {
            for (index in 0..segmentCount) {
                val ratio = index.toDouble() / segmentCount.toDouble()
                add(
                    RoutePoint(
                        latitude = originLatitude + (destinationLatitude - originLatitude) * ratio,
                        longitude = originLongitude + (destinationLongitude - originLongitude) * ratio,
                        altitude = originAltitude + (destinationAltitude - originAltitude) * ratio,
                    ),
                )
            }
        }

        return RouteResult(
            geometry = geometry,
            distanceMeters = distanceMeters,
            durationSeconds = 0.0,
        )
    }

    private fun MovementTransportMode.toOrsProfile(): String = when (this) {
        MovementTransportMode.Drive -> "driving-car"
        MovementTransportMode.Cycle -> "cycling-regular"
        MovementTransportMode.Walk -> "foot-walking"
        MovementTransportMode.Transit -> "driving-car"
    }

    private data class OrsRouteRequest(
        val coordinates: List<List<Double>>,
        val elevation: Boolean,
    )

    private data class OrsRouteResponse(
        val features: List<OrsFeature>? = emptyList(),
    )

    private data class OrsFeature(
        val geometry: OrsGeometry? = null,
        val properties: OrsProperties? = null,
    )

    private data class OrsGeometry(
        val coordinates: List<List<Double>>? = emptyList(),
    )

    private data class OrsProperties(
        val summary: OrsSummary? = null,
    )

    private data class OrsSummary(
        val distance: Double? = null,
        val duration: Double? = null,
    )

    private companion object {
        const val FALLBACK_SEGMENT_LENGTH_METERS = 25.0
        const val MAX_FALLBACK_SEGMENTS = 240
    }
}
