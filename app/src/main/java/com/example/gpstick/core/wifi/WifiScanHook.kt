package com.example.gpstick.core.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.WifiNetwork
import com.example.gpstick.service.SimulationStateStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class WifiScanHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                WifiManager::class.java,
                "getScanResults",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val preset = resolveActivePreset() ?: return
                        val wifiNetworks = preset.wifiNetworks
                        if (wifiNetworks.isEmpty()) {
                            return
                        }

                        param.result = ArrayList(wifiNetworks.map(::toScanResult))
                    }
                },
            )
            Log.i(TAG, "Installed Wi-Fi scan hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install Wi-Fi scan hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun resolveActivePreset(): LocationPreset? {
        val context = currentApplicationContext() ?: return null
        val snapshot = SimulationStateStore.readSnapshotFromProvider(context)
        val sharedState = snapshot.controlState
        if (!sharedState.isRunning || !sharedState.activeFeaturesEnabled || !sharedState.activeWifiMockEnabled) {
            return null
        }

        return snapshot.activePreset
    }

    private fun toScanResult(network: WifiNetwork): ScanResult {
        val scanResult = ScanResult::class.java.getDeclaredConstructor().newInstance()
        setField(scanResult, "BSSID", network.bssid)
        setField(scanResult, "SSID", network.ssid)
        setField(scanResult, "level", network.level)
        setField(scanResult, "frequency", network.frequency)
        setField(scanResult, "timestamp", SystemClock.elapsedRealtimeNanos())
        return scanResult
    }

    private fun setField(target: ScanResult, fieldName: String, value: Any) {
        val field = ScanResult::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun currentApplicationContext(): android.content.Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val method = activityThread.getDeclaredMethod("currentApplication")
        method.isAccessible = true
        method.invoke(null) as? android.content.Context
    }.getOrNull()

    private companion object {
        const val TAG = "GpStickWifiHook"
    }
}
