package io.github.ianlyoo.latebris.data.preset

interface PresetRepository {
    fun getPresets(): List<LocationPreset>

    fun getPreset(id: String): LocationPreset?

    fun savePresets(presets: List<LocationPreset>)

    fun upsertPreset(preset: LocationPreset)

    fun deletePreset(id: String)

    fun autoFillCellData(preset: LocationPreset): LocationPreset?
}
