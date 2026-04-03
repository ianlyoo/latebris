package com.example.gpstick.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

class SimulationStateProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != SimulationStateStore.METHOD_GET_STATE) {
            return super.call(method, arg, extras)
        }

        val context = context ?: return Bundle.EMPTY
        val application = context.applicationContext as? com.example.gpstick.ui.GpStickApplication
        return application?.appContainer?.simulationStateStore?.asBundle()
            ?: SimulationStateStore.getInstance(context).asBundle()
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
