package io.github.ianlyoo.latebris.core.wifi

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ianlyoo.latebris.hook.HookStateReader
import android.net.wifi.WifiManager

/** Replaces the Wi-Fi scan results an app reads with the preset's networks. */
class WifiScanHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                WifiManager::class.java,
                "getScanResults",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val wifiNetworks = HookStateReader.current()?.wifiNetworks ?: return
                        runCatching { ArrayList(wifiNetworks.map(ScanResultFactory::create)) }
                            .onSuccess { param.result = it }
                            .onFailure { Log.w(TAG, "Could not build preset scan results; leaving the real list", it) }
                    }
                },
            )
            Log.i(TAG, "Installed Wi-Fi scan hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install Wi-Fi scan hook for ${lpparam.packageName}", throwable)
        }
    }

    private companion object {
        const val TAG = "LatebrisWifiHook"
    }
}
