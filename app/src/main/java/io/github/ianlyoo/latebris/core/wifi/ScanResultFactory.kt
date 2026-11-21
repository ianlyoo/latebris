package io.github.ianlyoo.latebris.core.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiSsid
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.github.ianlyoo.latebris.data.preset.WifiNetwork

/**
 * Builds the [ScanResult] an app receives for a preset network. Public fields carry
 * BSSID, SSID, capabilities, level, frequency, timestamp and channel width; on API 33+
 * the SSID is also set through the hidden setWifiSsid, because getWifiSsid() is what
 * newer apps read.
 */
object ScanResultFactory {
    /** A common WPA2 access point, used when a preset does not record capabilities. */
    const val DEFAULT_CAPABILITIES = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"

    fun create(network: WifiNetwork): ScanResult = ScanResult().apply {
        @Suppress("DEPRECATION")
        SSID = network.ssid
        BSSID = network.bssid
        capabilities = network.capabilities ?: DEFAULT_CAPABILITIES
        level = network.level
        frequency = network.frequency
        channelWidth = ScanResult.CHANNEL_WIDTH_20MHZ
        centerFreq0 = network.frequency
        // ScanResult.timestamp is microseconds since boot.
        timestamp = SystemClock.elapsedRealtime() * 1_000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setWifiSsid(this, network.ssid)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setWifiSsid(result: ScanResult, ssid: String) {
        setWifiSsidMethod.invoke(result, WifiSsid.fromBytes(ssid.toByteArray(Charsets.UTF_8)))
    }

    // Looked up by name so the class reference stays inside the API 33 path.
    private val setWifiSsidMethod by lazy {
        ScanResult::class.java.getDeclaredMethod("setWifiSsid", Class.forName("android.net.wifi.WifiSsid"))
            .apply { isAccessible = true }
    }
}
