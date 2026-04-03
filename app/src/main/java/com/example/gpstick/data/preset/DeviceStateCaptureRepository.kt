package com.example.gpstick.data.preset

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlin.math.cos

data class CapturedDeviceState(
    val gps: GpsPreset,
    val wifiNetworks: List<WifiNetwork>,
    val cellTowers: List<CellTower>,
)

interface DeviceStateCaptureDataSource {
    fun captureCurrentState(): CapturedDeviceState?
}

class DeviceStateCaptureRepository(context: Context) : DeviceStateCaptureDataSource {
    private val appContext = context.applicationContext

    override
    fun captureCurrentState(): CapturedDeviceState? {
        val location = captureLocation() ?: return null

        return CapturedDeviceState(
            gps = GpsPreset(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracyMeters = if (location.accuracy >= 0f) location.accuracy else DEFAULT_ACCURACY,
            ),
            wifiNetworks = captureWifiNetworks(),
            cellTowers = captureCellTowers(),
        )
    }

    private fun captureLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return readLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private fun readLastKnownLocation(): Location? {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )

        return providers
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .minByOrNull { it.accuracy }
    }

    private fun captureWifiNetworks(): List<WifiNetwork> {
        if (!hasWifiScanAccess()) {
            return emptyList()
        }

        return readWifiNetworks()
    }

    @SuppressLint("MissingPermission")
    private fun readWifiNetworks(): List<WifiNetwork> = runCatching {
        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.scanResults
            .mapNotNull(::toWifiNetwork)
            .filter { it.ssid.isNotBlank() && it.bssid.isNotBlank() }
            .distinctBy { Pair(it.bssid, it.ssid) }
            .take(MAX_WIFI_NETWORKS)
    }.getOrNull().orEmpty()

    private fun captureCellTowers(): List<CellTower> {
        if (!hasLocationPermission()) {
            return emptyList()
        }

        return readCellTowers()
    }

    @SuppressLint("MissingPermission")
    private fun readCellTowers(): List<CellTower> {
        val telephonyManager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return runCatching { telephonyManager.allCellInfo.orEmpty() }
            .getOrNull()
            .orEmpty()
            .mapNotNull(this::toCellTower)
            .distinctBy { listOf(it.mcc, it.mnc, it.ci, it.pci, it.tac, it.earfcn) }
            .take(MAX_CELL_TOWERS)
    }

    private fun toWifiNetwork(scanResult: ScanResult): WifiNetwork = WifiNetwork(
        bssid = scanResult.BSSID,
        ssid = scanResult.SSID,
        level = scanResult.level,
        frequency = scanResult.frequency,
    )

    private fun toCellTower(info: CellInfo): CellTower? {
        if (info !is CellInfoLte) {
            return null
        }

        val identity = runCatching { info.cellIdentity }.getOrNull() ?: return null
        return CellTower(
            mcc = readCellIdentityInt(identity, listOf("getMcc", "mcc", "mccString")) ?: return null,
            mnc = readCellIdentityInt(identity, listOf("getMnc", "mnc", "mncString")) ?: return null,
            ci = readCellIdentityInt(identity, listOf("getCi", "ci", "cellId")) ?: return null,
            pci = readCellIdentityInt(identity, listOf("getPci", "pci")) ?: return null,
            tac = readCellIdentityInt(identity, listOf("getTac", "tac", "getLac", "lac")) ?: 0,
            earfcn = readCellIdentityInt(identity, listOf("getEarfcn", "earfcn")) ?: 0,
        )
    }

    private fun readCellIdentityInt(identity: Any, methodNames: List<String>): Int? {
        for (name in methodNames) {
            val value = runCatching {
                identity.javaClass.methods
                    .firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
                    ?.invoke(identity)
            }.getOrNull()
            value?.let { converted ->
                when (converted) {
                    is Int -> return converted
                    is String -> converted.toIntOrNull()
                }
            }

            val fieldValue = runCatching {
                val field = identity.javaClass.getDeclaredField(name)
                field.isAccessible = true
                field.get(identity)
            }.getOrNull()
            fieldValue?.let { converted ->
                when (converted) {
                    is Int -> return converted
                    is String -> converted.toIntOrNull()
                }
            }
        }

        return null
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun hasWifiScanAccess(): Boolean {
        val wifiStateGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_WIFI_STATE,
        ) == PackageManager.PERMISSION_GRANTED
        return hasLocationPermission() && wifiStateGranted
    }

    private companion object {
        const val MAX_WIFI_NETWORKS = 30
        const val MAX_CELL_TOWERS = 50
        const val DEFAULT_ACCURACY = 12.5f
        private val EARTH_METERS_PER_DEGREE_LAT = 111_320.0
        fun metersToLatitudeDegrees(meters: Double): Double = meters / EARTH_METERS_PER_DEGREE_LAT

        fun metersToLongitudeDegrees(latitudeDegrees: Double, meters: Double): Double =
            meters / (cos(Math.toRadians(latitudeDegrees)) * EARTH_METERS_PER_DEGREE_LAT.coerceAtLeast(1.0E-12))
    }
}
