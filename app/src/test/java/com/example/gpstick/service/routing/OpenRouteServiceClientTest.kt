package com.example.gpstick.service.routing

import com.example.gpstick.core.network.JsonHttpClient
import com.example.gpstick.core.network.StubHttpURLConnection
import com.example.gpstick.service.MovementTransportMode
import java.net.URL
import org.junit.Assert.*
import org.junit.Test

class OpenRouteServiceClientTest {
    @Test
    fun retainsPublicNoArgumentConstructorForJvmCallers() {
        assertNotNull(OpenRouteServiceClient::class.java.getConstructor().newInstance())
    }

    private fun fetch(client: OpenRouteServiceClient, mode: MovementTransportMode = MovementTransportMode.Walk) =
        client.fetchRoute(37.0, 127.0, 10.0, 38.0, 128.0, 20.0, mode)

    private fun client(connection: StubHttpURLConnection, distance: Double = 100.0) =
        OpenRouteServiceClient(JsonHttpClient(openConnection = { connection }), { "fixture" }, { _, _ -> distance })

    @Test
    fun unsuccessfulAndUnusableResponsesReturnFallbackAndDisconnect() {
        val responses = listOf(
            StubHttpURLConnection(status = 503),
            StubHttpURLConnection(body = """{"features":[]}"""),
            StubHttpURLConnection(body = """{"features":[{"geometry":{"coordinates":[[127,37]]}}]}"""),
            StubHttpURLConnection(body = """{"features":[{"geometry":{"coordinates":[[127],[128,38]]}}]}"""),
            StubHttpURLConnection(body = "{invalid"),
            StubHttpURLConnection(failWriting = true),
        )
        for (connection in responses) {
            val route = fetch(client(connection))!!
            assertEquals(100.0, route.distanceMeters, 0.0)
            assertEquals(0.0, route.durationSeconds, 0.0)
            assertEquals(5, route.geometry.size)
            assertEquals(RoutePoint(37.0, 127.0, 10.0), route.geometry.first())
            assertEquals(RoutePoint(38.0, 128.0, 20.0), route.geometry.last())
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun successfulResponsePreservesCoordinateOrderElevationAndSummary() {
        val connection = StubHttpURLConnection(body = """{"features":[{
            "geometry":{"coordinates":[[127,37,10],[128,38]]},
            "properties":{"summary":{"distance":250,"duration":40}}
        }]}""")
        val route = fetch(client(connection))!!
        assertEquals(listOf(RoutePoint(37.0, 127.0, 10.0), RoutePoint(38.0, 128.0, 0.0)), route.geometry)
        assertEquals(250.0, route.distanceMeters, 0.0)
        assertEquals(40.0, route.durationSeconds, 0.0)
        assertTrue(connection.disconnected)
    }

    @Test
    fun transportProfilesAndRequestCoordinateOrderStayCompatible() {
        for ((mode, profile) in mapOf(
            MovementTransportMode.Drive to "driving-car",
            MovementTransportMode.Transit to "driving-car",
            MovementTransportMode.Cycle to "cycling-regular",
            MovementTransportMode.Walk to "foot-walking",
        )) {
            val connection = StubHttpURLConnection()
            var url: URL? = null
            val client = OpenRouteServiceClient(
                JsonHttpClient(openConnection = { url = it; connection }), { "fixture" }, { _, _ -> 100.0 },
            )
            fetch(client, mode)
            assertEquals("/v2/directions/$profile/geojson", url?.path)
            assertEquals("fixture", connection.getRequestProperty("Authorization"))
            assertEquals("""{"coordinates":[[127.0,37.0],[128.0,38.0]],"elevation":true}""", connection.output.toString("UTF-8"))
        }
    }

    @Test
    fun missingKeyUsesBoundedInterpolatedFallbackWithoutNetwork() {
        for ((distance, pointCount) in listOf(1.0 to 3, 100.0 to 5, 100_000.0 to 241)) {
            val client = OpenRouteServiceClient(
                JsonHttpClient(openConnection = { error("Unexpected request") }), { "" }, { _, _ -> distance },
            )
            val route = fetch(client)!!
            assertEquals(pointCount, route.geometry.size)
            assertEquals(RoutePoint(37.5, 127.5, 15.0), route.geometry[pointCount / 2])
        }
    }

    @Test
    fun zeroDistanceHasNoFallbackRoute() {
        val client = OpenRouteServiceClient(
            JsonHttpClient(openConnection = { error("Unexpected request") }), { "" }, { _, _ -> 0.0 },
        )
        assertNull(fetch(client))
    }
}
