package io.github.ianlyoo.latebris.probe

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs one probe each time it comes to the front (launch, or `am start` while on top)
 * and prints every reading to logcat under [TAG] and on screen.
 */
class ProbeActivity : Activity() {
    private lateinit var output: TextView
    private val running = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(PADDING, PADDING * 3, PADDING, PADDING)
            setTextIsSelectable(true)
        }
        setContentView(ScrollView(this).apply { addView(output) })
    }

    override fun onResume() {
        super.onResume()
        probe()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        probe()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        probe()
    }

    private fun probe() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            output.text = getString(R.string.probe_needs_permission)
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
            return
        }
        if (!running.compareAndSet(false, true)) {
            return
        }
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        output.text = getString(R.string.probe_running, packageName, stamp)
        Thread {
            val lines = runCatching { ProbeReadings(this).readAll().map { it.toString() } }
                .getOrElse { listOf("""{"pkg":"$packageName","error":"${it.javaClass.simpleName}: ${it.message}"}""") }
            Log.i(TAG, """{"pkg":"$packageName","run":"$stamp"}""")
            lines.forEach { Log.i(TAG, it) }
            val text = buildString {
                append(getString(R.string.probe_title, packageName, stamp)).append("\n\n")
                lines.forEach { append(pretty(it)).append("\n\n") }
            }
            runOnUiThread {
                output.text = text
                running.set(false)
            }
        }.start()
    }

    private fun pretty(line: String): String = runCatching { org.json.JSONObject(line).toString(2) }.getOrDefault(line)

    private companion object {
        const val TAG = "LatebrisProbe"
        const val PADDING = 24
    }
}
