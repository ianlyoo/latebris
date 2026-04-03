package com.example.gpstick.data.preset

import android.content.Context

class FilePresetRepository(
    context: Context,
) : PresetRepository {
    init {
        PresetManager.initialize(context)
    }

    override fun getPresets(): List<LocationPreset> = PresetManager.getPresets()

    override fun getPreset(id: String): LocationPreset? = PresetManager.getPreset(id)

    override fun savePresets(presets: List<LocationPreset>) {
        PresetManager.savePresets(presets)
    }

    override fun upsertPreset(preset: LocationPreset) {
        if (PresetManager.getPreset(preset.id) != null) {
            PresetManager.updatePreset(preset)
        } else {
            PresetManager.createPreset(preset)
        }
    }

    override fun deletePreset(id: String) {
        PresetManager.deletePreset(id)
    }

    override fun autoFillCellData(preset: LocationPreset): LocationPreset? = PresetManager.autoFillCellData(preset)
}
