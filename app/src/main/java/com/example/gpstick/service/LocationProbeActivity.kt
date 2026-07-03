package com.example.gpstick.service

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

class LocationProbeActivity : ComponentActivity() {
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LocationProbeRunner.TAG, "Location probe activity created.")
    }

    override fun onResume() {
        super.onResume()
        if (started) {
            return
        }
        started = true
        Thread {
            try {
                Log.i(LocationProbeRunner.TAG, "Location probe activity started.")
                LocationProbeRunner(applicationContext).runProbe()
                Log.i(LocationProbeRunner.TAG, "Location probe activity finished.")
            } catch (throwable: Throwable) {
                Log.e(LocationProbeRunner.TAG, "Location probe activity failed.", throwable)
            } finally {
                finish()
            }
        }.start()
    }
}
