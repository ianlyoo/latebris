package com.example.gpstick.core.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetManager
import com.example.gpstick.data.preset.WifiNetwork
import com.example.gpstick.service.SimulationStateStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class WifiScanHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) {
            return
        }

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
    }

    private fun resolveActivePreset(): LocationPreset? {
        val context = currentApplicationContext() ?: return null
        val sharedState = SimulationStateStore.readFromProvider(context)
        if (!sharedState.isRunning || !sharedState.activeFeaturesEnabled || !sharedState.activeWifiMockEnabled) {
            return null
        }

        PresetManager.initialize(context)
        val presetId = sharedState.activePresetId ?: return null
        return PresetManager.getPreset(presetId)
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
        const val TARGET_PACKAGE = "com.example.gpstick"
    }
}
