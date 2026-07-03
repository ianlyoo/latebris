package com.example.gpstick.hook

import android.util.Log
import com.example.gpstick.core.cell.CellInfoHook
import com.example.gpstick.core.gps.LocationMockHook
import com.example.gpstick.core.wifi.WifiScanHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ModuleEntryPoint : IXposedHookLoadPackage {
    private val wifiScanHook = WifiScanHook()
    private val cellInfoHook = CellInfoHook()
    private val locationMockHook = LocationMockHook()

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        locationMockHook.handleLoadPackage(lpparam)
        wifiScanHook.handleLoadPackage(lpparam)
        cellInfoHook.handleLoadPackage(lpparam)

        if (lpparam.packageName == "com.example.gpstick") {
            Log.i(TAG, "Loading hooks for ${lpparam.packageName}")
        }
    }

    private companion object {
        const val TAG = "GpStickModule"
    }
}
