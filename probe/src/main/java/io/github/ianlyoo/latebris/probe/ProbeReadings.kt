package io.github.ianlyoo.latebris.probe

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads GPS, Wi-Fi and cell data through the public APIs an ordinary app uses.
 * Each reading is one JSON object; the caller logs it and shows it on screen.
 * Permissions are checked by the caller before [readAll] runs.
 */
@SuppressLint("MissingPermission")
class ProbeReadings(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)

    fun readAll(): List<JSONObject> {
        val current = currentGpsLocation()
        return listOf(
            location("gps", "lm.gps.last", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)),
            location("gps", "lm.gps.current", current),
            location("gps", "fused.current", fusedCurrentLocation()),
            getterCost(current),
            wifiScan(),
            cellInfo(),
            cellInfoUpdate(),
        )
    }

    private fun currentGpsLocation(): Location? {
        val latch = CountDownLatch(1)
        var result: Location? = null
        val executor = Executors.newSingleThreadExecutor()
        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, executor) { location ->
            result = location
            latch.countDown()
        }
        latch.await(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        executor.shutdown()
        return result
    }

    private fun fusedCurrentLocation(): Location? = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        Tasks.await(
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token),
            LOCATION_TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
    }.getOrNull()

    private fun location(signal: String, source: String, location: Location?): JSONObject =
        base(signal, source).apply {
            if (location == null) {
                put("value", JSONObject.NULL)
                return@apply
            }
            put("lat", location.latitude)
            put("lon", location.longitude)
            put("alt", location.altitude)
            put("acc", location.accuracy.toDouble())
            put("mock", location.isMock)
            put("provider", location.provider ?: "")
        }

    /** Wall time of 1000 getLatitude() calls on one Location, the cost a hook adds per getter. */
    private fun getterCost(location: Location?): JSONObject = base("gps", "getter.cost").apply {
        if (location == null) {
            put("value", JSONObject.NULL)
            return@apply
        }
        val start = System.nanoTime()
        var sink = 0.0
        repeat(GETTER_CALLS) { sink += location.latitude }
        put("calls", GETTER_CALLS)
        put("ms", (System.nanoTime() - start) / 1_000_000.0)
        put("checksum", sink / GETTER_CALLS)
    }

    private fun wifiScan(): JSONObject = base("wifi", "wm.scanResults").apply {
        val networks = JSONArray()
        wifiManager.scanResults.forEach { result ->
            networks.put(
                JSONObject().apply {
                    put("bssid", result.BSSID ?: JSONObject.NULL)
                    put("ssid", ssidOf(result))
                    put("capabilities", result.capabilities ?: JSONObject.NULL)
                    put("level", result.level)
                    put("frequency", result.frequency)
                },
            )
        }
        put("count", networks.length())
        put("networks", networks)
    }

    private fun ssidOf(result: android.net.wifi.ScanResult): Any =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.wifiSsid?.toString()?.removeSurrounding("\"") ?: JSONObject.NULL
        } else {
            @Suppress("DEPRECATION")
            result.SSID ?: JSONObject.NULL
        }

    private fun cellInfo(): JSONObject = base("cell", "tm.allCellInfo").apply {
        val cells = JSONArray()
        telephonyManager.allCellInfo.orEmpty().forEach { cells.put(describe(it)) }
        put("count", cells.length())
        put("cells", cells)
    }

    private fun cellInfoUpdate(): JSONObject = base("cell", "tm.requestCellInfoUpdate").apply {
        val latch = CountDownLatch(1)
        var received: List<CellInfo> = emptyList()
        val executor = Executors.newSingleThreadExecutor()
        telephonyManager.requestCellInfoUpdate(
            executor,
            object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                    received = cellInfo
                    latch.countDown()
                }
            },
        )
        latch.await(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        executor.shutdown()
        val cells = JSONArray()
        received.forEach { cells.put(describe(it)) }
        put("count", cells.length())
        put("cells", cells)
    }

    private fun describe(info: CellInfo): JSONObject = JSONObject().apply {
        put("registered", info.isRegistered)
        when (info) {
            is CellInfoLte -> {
                val id = info.cellIdentity
                put("type", "lte")
                put("mcc", id.mccString ?: JSONObject.NULL)
                put("mnc", id.mncString ?: JSONObject.NULL)
                put("ci", id.ci)
                put("pci", id.pci)
                put("tac", id.tac)
                put("earfcn", id.earfcn)
                put("rsrp", info.cellSignalStrength.rsrp)
            }
            is CellInfoNr -> put("type", "nr")
            else -> put("type", info.javaClass.simpleName)
        }
    }

    private fun base(signal: String, source: String) = JSONObject().apply {
        put("pkg", context.packageName)
        put("signal", signal)
        put("source", source)
    }

    private companion object {
        const val LOCATION_TIMEOUT_SECONDS = 10L
        const val GETTER_CALLS = 1000
    }
}
