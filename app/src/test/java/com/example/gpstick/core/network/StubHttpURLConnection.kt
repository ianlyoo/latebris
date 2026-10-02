package com.example.gpstick.core.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class StubHttpURLConnection(
    private val status: Int = 200,
    body: String = "{}",
    private val failWriting: Boolean = false,
) : HttpURLConnection(URL("https://fixture.invalid")) {
    var disconnected = false
    var inputClosed = false
    var outputClosed = false
    var errorClosed = false
    val input = object : ByteArrayInputStream(body.toByteArray()) {
        override fun close() { inputClosed = true; super.close() }
    }
    val output = object : ByteArrayOutputStream() {
        override fun close() { outputClosed = true; super.close() }
    }
    private val error = object : ByteArrayInputStream(byteArrayOf()) {
        override fun close() { errorClosed = true; super.close() }
    }

    override fun connect() = Unit
    override fun disconnect() { disconnected = true }
    override fun usingProxy() = false
    override fun getResponseCode() = status
    override fun getInputStream() = input
    override fun getErrorStream() = error
    override fun getOutputStream(): ByteArrayOutputStream {
        if (failWriting) throw IOException("fixture output failure")
        return output
    }
}
