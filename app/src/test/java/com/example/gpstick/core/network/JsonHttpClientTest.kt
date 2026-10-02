package com.example.gpstick.core.network

import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.URL
import org.junit.Assert.*
import org.junit.Test

class JsonHttpClientTest {
    private val url = URL("https://fixture.invalid")
    private data class Response(val value: Int)

    @Test
    fun postsJsonAndClosesBothStreamsAndConnection() {
        val connection = StubHttpURLConnection(body = """{"value":42}""")
        val client = JsonHttpClient(openConnection = { connection })

        val response = client.post(url, mapOf("input" to 1), Response::class.java, mapOf("Authorization" to "fixture"))

        assertEquals(42, response?.value)
        assertEquals("POST", connection.requestMethod)
        assertEquals("application/json", connection.getRequestProperty("Content-Type"))
        assertEquals("fixture", connection.getRequestProperty("Authorization"))
        assertEquals("""{"input":1}""", connection.output.toString("UTF-8"))
        assertEquals(10_000, connection.connectTimeout)
        assertEquals(10_000, connection.readTimeout)
        assertTrue(connection.inputClosed && connection.outputClosed && connection.disconnected)
    }

    @Test
    fun unsuccessfulResponseClosesErrorStreamAndDisconnects() {
        val connection = StubHttpURLConnection(status = 503)
        val client = JsonHttpClient(openConnection = { connection })

        assertNull(client.get(url, Response::class.java))

        assertTrue(connection.errorClosed && connection.disconnected)
        assertFalse(connection.doOutput)
    }

    @Test
    fun malformedJsonStillClosesInputAndConnection() {
        val connection = StubHttpURLConnection(body = "{invalid-json")
        val client = JsonHttpClient(openConnection = { connection })

        assertThrows(JsonSyntaxException::class.java) { client.get(url, Response::class.java) }

        assertTrue(connection.inputClosed && connection.disconnected)
    }

    @Test
    fun outputFailureStillDisconnects() {
        val connection = StubHttpURLConnection(failWriting = true)
        val client = JsonHttpClient(openConnection = { connection })

        assertThrows(IOException::class.java) { client.post(url, emptyMap<String, String>(), Response::class.java) }

        assertTrue(connection.disconnected)
    }
}
