package com.example.gpstick.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LocationProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PROBE) {
            return
        }

        val pendingResult = goAsync()
        Thread {
            try {
                Log.i(LocationProbeRunner.TAG, "Location probe receiver started.")
                LocationProbeRunner(context.applicationContext).runProbe()
                Log.i(LocationProbeRunner.TAG, "Location probe receiver finished.")
            } catch (throwable: Throwable) {
                Log.e(LocationProbeRunner.TAG, "Location probe receiver failed.", throwable)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private companion object {
        const val ACTION_PROBE = "com.example.gpstick.action.PROBE_LOCATION"
    }
}
