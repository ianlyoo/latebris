package com.example.gpstick.service.routing

import com.example.gpstick.BuildConfig
import com.example.gpstick.service.MovementTransportMode
import com.google.gson.Gson
import java.net.HttpURLConnection
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

class OpenRouteServiceClient(
    private val gson: Gson = Gson(),
) {
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
        val apiKey = BuildConfig.OPEN_ROUTE_SERVICE_API_KEY.takeIf { it.isNotBlank() } ?: return fallbackRoute
        val profile = mode.toOrsProfile()
        val endpoint = "https://api.openrouteservice.org/v2/directions/$profile/geojson"

        val requestBody = OrsRouteRequest(
            coordinates = listOf(
                listOf(originLongitude, originLatitude),
                listOf(destinationLongitude, destinationLatitude),
            ),
            elevation = true,
        )

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", apiKey)
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        return runCatching {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(gson.toJson(requestBody))
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.errorStream?.close()
                return null
            }

            val response = connection.inputStream.bufferedReader().use { reader ->
                gson.fromJson(reader, OrsRouteResponse::class.java)
            }

            val feature = response.features.orEmpty().firstOrNull() ?: return null
            val coordinates = feature.geometry?.coordinates.orEmpty()
            if (coordinates.size < 2) {
                return null
            }

            val geometry = coordinates.mapNotNull { coordinate ->
                val longitude = coordinate.getOrNull(0) ?: return@mapNotNull null
                val latitude = coordinate.getOrNull(1) ?: return@mapNotNull null
                val altitude = coordinate.getOrNull(2) ?: 0.0
                RoutePoint(
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                )
            }

            if (geometry.size < 2) {
                return null
            }

            val summary = feature.properties?.summary
            RouteResult(
                geometry = geometry,
                distanceMeters = summary?.distance ?: 0.0,
                durationSeconds = summary?.duration ?: 0.0,
            )
        }.getOrNull().also {
            connection.disconnect()
        } ?: fallbackRoute
    }

    private fun buildFallbackRoute(
        originLatitude: Double,
        originLongitude: Double,
        originAltitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double,
        destinationAltitude: Double,
    ): RouteResult? {
        val distanceMeters = android.location.Location("").run {
            val result = FloatArray(1)
            android.location.Location.distanceBetween(
                originLatitude,
                originLongitude,
                destinationLatitude,
                destinationLongitude,
                result,
            )
            result[0].toDouble()
        }

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
