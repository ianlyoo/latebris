package io.github.ianlyoo.latebris.hook

import android.util.Log
import io.github.ianlyoo.latebris.core.cell.CellInfoHook
import io.github.ianlyoo.latebris.core.gps.LocationMockHook
import io.github.ianlyoo.latebris.core.wifi.WifiScanHook
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

        if (lpparam.packageName == "io.github.ianlyoo.latebris") {
            Log.i(TAG, "Loading hooks for ${lpparam.packageName}")
        }
    }

    private companion object {
        const val TAG = "GpStickModule"
    }
}
