package com.example.gpstick.data.preset

class InMemoryPresetRepository(
    initialPresets: List<LocationPreset>,
    private val store: JsonPresetStore = JsonPresetStore(),
) : PresetRepository {
    private var presets: List<LocationPreset> = initialPresets

    override fun getPresets(): List<LocationPreset> = presets

    override fun getPreset(id: String): LocationPreset? = presets.firstOrNull { it.id == id }

    override fun savePresets(presets: List<LocationPreset>) {
        this.presets = store.decode(store.encode(presets))
    }

    override fun upsertPreset(preset: LocationPreset) {
        val updated = presets.toMutableList()
        val existingIndex = updated.indexOfFirst { it.id == preset.id }
        if (existingIndex >= 0) {
            updated[existingIndex] = preset
        } else {
            updated.add(preset)
        }
        savePresets(updated)
    }

    override fun deletePreset(id: String) {
        savePresets(presets.filterNot { it.id == id })
    }

    override fun autoFillCellData(preset: LocationPreset): LocationPreset? = preset
}
