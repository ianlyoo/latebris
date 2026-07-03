package com.example.gpstick.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.example.gpstick.data.preset.FilePresetRepository
import com.example.gpstick.data.preset.GpsPreset
import com.example.gpstick.data.preset.LocationPreset

class SimulationStateProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != SimulationStateStore.METHOD_GET_STATE) {
            return super.call(method, arg, extras)
        }

        val context = context ?: return Bundle.EMPTY
        val application = context.applicationContext as? com.example.gpstick.ui.GpStickApplication
        val appContainer = application?.appContainer
        val stateStore = appContainer?.simulationStateStore ?: SimulationStateStore.getInstance(context)
        val snapshot = stateStore.invalidateStaleRunningState()
        val presetRepository = appContainer?.presetRepository ?: FilePresetRepository(context)
        val movement = snapshot.movementSession
        val activePreset = when (movement.phase) {
            MovementPhase.Routing,
            MovementPhase.Moving,
            MovementPhase.Canceled
            -> synthesizeMovementPreset(snapshot, presetRepository)

            else -> snapshot.activePresetId?.let(presetRepository::getPreset)
        }
        return stateStore.asProviderBundle(activePreset)
    }

    private fun synthesizeMovementPreset(
        state: SimulationControlState,
        presetRepository: com.example.gpstick.data.preset.PresetRepository,
    ): LocationPreset? {
        val movement = state.movementSession
        val originPreset = movement.originPresetId?.let(presetRepository::getPreset)
            ?: state.activePresetId?.let(presetRepository::getPreset)
            ?: return null
        val latitude = movement.currentLatitude ?: originPreset.gps.latitude
        val longitude = movement.currentLongitude ?: originPreset.gps.longitude
        val altitude = movement.currentAltitude ?: originPreset.gps.altitude

        return LocationPreset(
            id = "movement-live-${originPreset.id}",
            name = "Movement live",
            summary = "Movement live preset",
            gps = GpsPreset(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                accuracyMeters = originPreset.gps.accuracyMeters,
            ),
            wifiNetworks = emptyList(),
            cellTowers = originPreset.cellTowers,
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
