package io.github.ianlyoo.latebris.core.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import io.github.ianlyoo.latebris.data.preset.WifiNetwork
import io.github.ianlyoo.latebris.hook.HookStateReader
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
                        val wifiNetworks = HookStateReader.current()?.wifiNetworks ?: return
                        param.result = ArrayList(wifiNetworks.map(::toScanResult))
                    }
                },
            )
            Log.i(TAG, "Installed Wi-Fi scan hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install Wi-Fi scan hook for ${lpparam.packageName}", throwable)
        }
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

    private companion object {
        const val TAG = "LatebrisWifiHook"
    }
}
