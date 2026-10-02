package com.example.gpstick.data.preset

import android.content.Context
import com.google.gson.GsonBuilder
import java.io.File

object PresetManager {
    private const val STORAGE_FILE_NAME = "presets.json"
    private const val DEFAULT_ASSET_FILE_NAME = "presets/default-presets.json"

    private lateinit var appContext: Context
    private val store = JsonPresetStore(
        gson = GsonBuilder().setPrettyPrinting().create(),
    )
    private val cellDataClient = OpenCellIdClient()
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
    fun autoFillCellData(preset: LocationPreset): LocationPreset? = cellDataClient.autoFill(preset)

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
}
