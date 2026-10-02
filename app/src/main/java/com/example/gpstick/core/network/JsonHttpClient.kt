package com.example.gpstick.core.network

import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

/** Owns the connection and streams for a single JSON request. */
internal class JsonHttpClient(
    private val gson: Gson = Gson(),
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    fun <T> get(url: URL, responseType: Class<T>): T? =
        request(url, "GET", responseType)

    fun <T> post(
        url: URL,
        body: Any,
        responseType: Class<T>,
        headers: Map<String, String> = emptyMap(),
    ): T? = request(url, "POST", responseType, gson.toJson(body), headers)

    private fun <T> request(
        url: URL,
        method: String,
        responseType: Class<T>,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): T? {
        val connection = openConnection(url)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }

            if (connection.responseCode !in 200..299) {
                connection.errorStream?.close()
                return null
            }

            return connection.inputStream.bufferedReader().use { gson.fromJson(it, responseType) }
        } finally {
            // Non-success responses, decoding failures and early returns all
            // release the connection before callers choose their fallback.
            connection.disconnect()
        }
    }
}
