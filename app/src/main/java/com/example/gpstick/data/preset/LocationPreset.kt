package com.example.gpstick.data.preset

data class LocationPreset(
    val id: String,
    val name: String,
    val summary: String,
    val gps: GpsPreset,
    val wifiNetworks: List<WifiNetwork> = emptyList(),
    val cellTowers: List<CellTower> = emptyList(),
)

data class GpsPreset(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracyMeters: Float,
)

data class WifiNetwork(
    val bssid: String,
    val ssid: String,
    val level: Int,
    val frequency: Int,
)

data class CellTower(
    val mcc: Int,
    val mnc: Int,
    val ci: Int,
    val pci: Int,
    val tac: Int,
    val earfcn: Int,
)
