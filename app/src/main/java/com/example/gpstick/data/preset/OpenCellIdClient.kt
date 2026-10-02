package com.example.gpstick.data.preset

import com.example.gpstick.BuildConfig
import com.example.gpstick.core.network.JsonHttpClient
import com.google.gson.annotations.SerializedName
import java.net.URL
import java.net.URLEncoder
import kotlin.math.cos
import kotlin.math.max

internal class OpenCellIdClient(
    private val http: JsonHttpClient = JsonHttpClient(),
    private val apiKey: () -> String = { BuildConfig.OPEN_CELL_ID_API_KEY },
) {
    fun autoFill(preset: LocationPreset): LocationPreset {
        val key = apiKey().takeIf { it.isNotBlank() } ?: return preset
        return runCatching {
            val query = listOf(
                "key" to key,
                "BBOX" to buildBoundingBox(preset.gps.latitude, preset.gps.longitude),
                "format" to "json",
                "limit" to "50",
            ).joinToString("&") { (name, value) ->
                "$name=${URLEncoder.encode(value, "UTF-8").replace("+", "%20")}"
            }
            val response = http.get(URL("https://opencellid.org/cell/getInArea?$query"), AreaResponse::class.java)
                ?: return@runCatching preset
            preset.copy(
                cellTowers = response.cells.orEmpty()
                    .mapNotNull(Cell::toCellTower)
                    .distinctBy { listOf(it.mcc, it.mnc, it.ci, it.pci, it.tac, it.earfcn) },
            )
        }.getOrElse { preset }
    }

    private fun buildBoundingBox(latitude: Double, longitude: Double): String {
        val latDelta = 2_000.0 / 111_320.0
        val cosLatitude = max(cos(Math.toRadians(latitude)), 1.0E-12)
        val lonDelta = 2_000.0 / (111_320.0 * cosLatitude)
        return "${latitude - latDelta},${longitude - lonDelta},${latitude + latDelta},${longitude + lonDelta}"
    }

    private data class AreaResponse(val cells: List<Cell>? = emptyList())

    private data class Cell(
        val mcc: Int? = null,
        val mnc: Int? = null,
        val pci: Int? = null,
        val tac: Int? = null,
        val earfcn: Int? = null,
        val lac: Int? = null,
        @SerializedName("cellid") val cellId: Int? = null,
    ) {
        fun toCellTower(): CellTower? = cellId?.let {
            CellTower(
                mcc = mcc ?: 0,
                mnc = mnc ?: 0,
                ci = it,
                pci = pci ?: 0,
                tac = tac ?: lac ?: 0,
                earfcn = earfcn ?: 0,
            )
        }
    }
}
