package io.github.ianlyoo.latebris.data

import android.content.Context
import io.github.ianlyoo.latebris.BuildConfig

/**
 * Keys for the two optional web services, entered in Options and kept in app-private
 * preferences. A key built in through the Gradle property or environment variable of the
 * same name is used when none is entered. Latebris ships with no key.
 */
class ApiKeyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** OpenRouteService: road routes for movement. Without it, movement is a straight line. */
    fun routeKey(): String = entered(KEY_ROUTE).ifBlank { BuildConfig.OPEN_ROUTE_SERVICE_API_KEY }

    /** OpenCellID: nearby towers for a new preset. Without it, that step is skipped. */
    fun cellKey(): String = entered(KEY_CELL).ifBlank { BuildConfig.OPEN_CELL_ID_API_KEY }

    fun enteredRouteKey(): String = entered(KEY_ROUTE)

    fun enteredCellKey(): String = entered(KEY_CELL)

    fun save(routeKey: String, cellKey: String) {
        preferences.edit()
            .putString(KEY_ROUTE, routeKey.trim())
            .putString(KEY_CELL, cellKey.trim())
            .apply()
    }

    private fun entered(key: String): String = preferences.getString(key, null).orEmpty()

    private companion object {
        const val PREF_NAME = "api_keys"
        const val KEY_ROUTE = "open_route_service"
        const val KEY_CELL = "open_cell_id"
    }
}
