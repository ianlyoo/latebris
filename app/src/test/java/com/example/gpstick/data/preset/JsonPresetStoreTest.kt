package com.example.gpstick.data.preset

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonPresetStoreTest {
    @Test
    fun roundTripsExpandedPresetSchema() {
        val store = JsonPresetStore()
        val presets = listOf(
            LocationPreset(
                id = "sample",
                name = "Sample",
                summary = "Summary",
                gps = GpsPreset(37.0, 127.0, 30.0, 5f),
                wifiNetworks = listOf(
                    WifiNetwork(
                        bssid = "00:11:22:33:44:55",
                        ssid = "qa-lab",
                        level = -45,
                        frequency = 2412,
                    )
                ),
                cellTowers = listOf(
                    CellTower(
                        mcc = 450,
                        mnc = 5,
                        ci = 22011,
                        pci = 101,
                        tac = 101,
                        earfcn = 1800,
                    )
                ),
            )
        )

        val encoded = store.encode(presets)
        val decoded = store.decode(encoded)

        assertEquals(presets, decoded)
    }
}
