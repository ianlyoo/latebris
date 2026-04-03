package com.example.gpstick.data.preset

import android.content.Context

class PresetAssetLoader(
    private val context: Context,
    private val fileName: String = DEFAULT_FILE_NAME,
    private val store: JsonPresetStore = JsonPresetStore(),
) {
    fun load(): List<LocationPreset> {
        val raw = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return store.decode(raw)
    }

    companion object {
        const val DEFAULT_FILE_NAME = "presets/default-presets.json"
    }
}
