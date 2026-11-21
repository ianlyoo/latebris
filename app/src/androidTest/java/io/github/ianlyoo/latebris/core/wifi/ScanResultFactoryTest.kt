package io.github.ianlyoo.latebris.core.wifi

import android.net.wifi.ScanResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ianlyoo.latebris.data.preset.WifiNetwork
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Builds scan results with the factory the hook uses and reads them back like an app. */
@RunWith(AndroidJUnit4::class)
class ScanResultFactoryTest {

    @Test
    fun presetNetworkReadsBackThroughPublicApi() {
        val result = ScanResultFactory.create(WifiNetwork("00:11:22:33:44:55", "qa-lab-a", -48, 2412))

        assertEquals("00:11:22:33:44:55", result.BSSID)
        @Suppress("DEPRECATION")
        assertEquals("qa-lab-a", result.SSID)
        assertEquals("\"qa-lab-a\"", result.wifiSsid.toString())
        assertEquals(ScanResultFactory.DEFAULT_CAPABILITIES, result.capabilities)
        assertEquals(-48, result.level)
        assertEquals(2412, result.frequency)
        assertEquals(ScanResult.CHANNEL_WIDTH_20MHZ, result.channelWidth)
    }

    @Test
    fun recordedCapabilitiesAreKept() {
        val result = ScanResultFactory.create(WifiNetwork("10:20:30:40:50:60", "open-net", -60, 5180, "[ESS]"))
        assertEquals("[ESS]", result.capabilities)
    }
}
