package com.example.gpstick.data.preset

import android.content.Context
import android.net.Uri
import com.example.gpstick.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.max

object PresetManager {
    private const val STORAGE_FILE_NAME = "presets.json"
    private const val DEFAULT_ASSET_FILE_NAME = "presets/default-presets.json"
    private const val OPEN_CELL_ID_ENDPOINT = "https://opencellid.org/cell/getInArea"
    private const val OPEN_CELL_ID_RADIUS_METERS = 2_000.0
    private const val OPEN_CELL_ID_LIMIT = 50

    private lateinit var appContext: Context
    private val store = JsonPresetStore(
        gson = GsonBuilder().setPrettyPrinting().create(),
    )
    private val gson = Gson()
    private var activePreset: LocationPreset? = null

    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        ensureStorageFile()
    }

    @Synchronized
    fun activatePreset(preset: LocationPreset) {
        activePreset = preset
    }

    @Synchronized
    fun getActivePreset(): LocationPreset? = activePreset

    @Synchronized
    fun clearActivePreset() {
        activePreset = null
    }

    @Synchronized
    fun getPresets(): List<LocationPreset> = readPresets()

    @Synchronized
    fun getPreset(id: String): LocationPreset? = readPresets().firstOrNull { it.id == id }

    @Synchronized
    fun savePresets(presets: List<LocationPreset>) {
        val file = ensureStorageFile()
        val payload = store.encode(presets)
        val tempFile = File(file.parentFile ?: requireContext().filesDir, "$STORAGE_FILE_NAME.tmp")
        tempFile.writeText(payload)
        if (!tempFile.renameTo(file)) {
            file.writeText(payload)
            tempFile.delete()
        }
    }

    @Synchronized
    fun createPreset(preset: LocationPreset): LocationPreset {
        val presets = readPresets().toMutableList()
        presets.removeAll { it.id == preset.id }
        presets.add(preset)
        savePresets(presets)
        return preset
    }

    @Synchronized
    fun updatePreset(preset: LocationPreset): LocationPreset {
        val presets = readPresets().map { current ->
            if (current.id == preset.id) preset else current
        }
        savePresets(presets)
        if (activePreset?.id == preset.id) {
            activePreset = preset
        }
        return preset
    }

    @Synchronized
    fun deletePreset(id: String) {
        savePresets(readPresets().filterNot { it.id == id })
        if (activePreset?.id == id) {
            activePreset = null
        }
    }

    @Synchronized
    fun addWifiNetwork(presetId: String, wifiNetwork: WifiNetwork): LocationPreset? {
        val preset = getPreset(presetId) ?: return null
        val updatedPreset = preset.copy(wifiNetworks = preset.wifiNetworks + wifiNetwork)
        return updatePreset(updatedPreset)
    }

    @Synchronized
    fun autoFillCellData(preset: LocationPreset): LocationPreset? {
        val apiKey = BuildConfig.OPEN_CELL_ID_API_KEY.takeIf { it.isNotBlank() } ?: return preset
        val requestUrl = Uri.parse(OPEN_CELL_ID_ENDPOINT)
            .buildUpon()
            .appendQueryParameter("key", apiKey)
            .appendQueryParameter("BBOX", buildBoundingBox(preset.gps.latitude, preset.gps.longitude))
            .appendQueryParameter("format", "json")
            .appendQueryParameter("limit", OPEN_CELL_ID_LIMIT.toString())
            .build()
            .toString()

        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return runCatching {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.errorStream?.close()
                return preset
            }

            val response = connection.inputStream.bufferedReader().use { reader ->
                gson.fromJson(reader, OpenCellIdAreaResponse::class.java)
            }

            val updatedPreset = preset.copy(
                cellTowers = response.cells
                    .orEmpty()
                    .mapNotNull(OpenCellIdCell::toCellTower)
                    .distinctBy { tower -> listOf(tower.mcc, tower.mnc, tower.ci, tower.pci, tower.tac, tower.earfcn) },
            )
            updatedPreset
        }.getOrElse {
            preset
        }.also {
            connection.disconnect()
        }
    }

    private fun readPresets(): List<LocationPreset> {
        val file = ensureStorageFile()
        return runCatching {
            store.decode(file.readText())
        }.getOrElse {
            copyDefaultAssetTo(file)
            store.decode(file.readText())
        }
    }

    private fun ensureStorageFile(): File {
        val context = requireContext()
        val file = File(context.filesDir, STORAGE_FILE_NAME)
        if (!file.exists()) {
            copyDefaultAssetTo(file)
        }
        return file
    }

    private fun copyDefaultAssetTo(file: File) {
        file.parentFile?.mkdirs()
        requireContext().assets.open(DEFAULT_ASSET_FILE_NAME).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun requireContext(): Context {
        check(::appContext.isInitialized) { "PresetManager is not initialized" }
        return appContext
    }

    private fun buildBoundingBox(latitude: Double, longitude: Double): String {
        val latDelta = OPEN_CELL_ID_RADIUS_METERS / 111_320.0
        val cosLatitude = max(cos(Math.toRadians(latitude)), 1.0E-12)
        val lonDelta = OPEN_CELL_ID_RADIUS_METERS / (111_320.0 * cosLatitude)
        val latMin = latitude - latDelta
        val lonMin = longitude - lonDelta
        val latMax = latitude + latDelta
        val lonMax = longitude + lonDelta
        return "$latMin,$lonMin,$latMax,$lonMax"
    }

    private data class OpenCellIdAreaResponse(
        val cells: List<OpenCellIdCell>? = emptyList(),
    )

    private data class OpenCellIdCell(
        val mcc: Int? = null,
        val mnc: Int? = null,
        val pci: Int? = null,
        val tac: Int? = null,
        val earfcn: Int? = null,
        val lac: Int? = null,
        @SerializedName("cellid") val cellId: Int? = null,
    ) {
        fun toCellTower(): CellTower? {
            val resolvedCi = cellId ?: return null
            return CellTower(
                mcc = mcc ?: 0,
                mnc = mnc ?: 0,
                ci = resolvedCi,
                pci = pci ?: 0,
                tac = tac ?: lac ?: 0,
                earfcn = earfcn ?: 0,
            )
        }
    }
}
