package com.example.gpstick.data.preset

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class JsonPresetStore(
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
) {
    private val listType = object : TypeToken<List<LocationPreset>>() {}.type

    fun decode(raw: String): List<LocationPreset> = gson.fromJson(raw, listType) ?: emptyList()

    fun encode(presets: List<LocationPreset>): String = gson.toJson(presets, listType)
}
