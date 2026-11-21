package io.github.ianlyoo.latebris.hook

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ianlyoo.latebris.BuildConfig
import io.github.ianlyoo.latebris.core.cell.CellInfoHook
import io.github.ianlyoo.latebris.core.gps.LocationMockHook
import io.github.ianlyoo.latebris.core.wifi.WifiScanHook
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The module's only entry point (assets/xposed_init).
 *
 * - system_server ("android") is never a target.
 * - In Latebris itself nothing is faked; [ModuleStatus.isActive] is made to return true.
 * - In any other scoped app the GPS, Wi-Fi and cell hooks are installed once per process
 *   (they hook framework classes, which every package in the process shares).
 */
class ModuleEntryPoint : IXposedHookLoadPackage {
    private val installed = AtomicBoolean(false)

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            SYSTEM_SERVER -> return
            BuildConfig.APPLICATION_ID -> reportActive(lpparam)
            else -> if (installed.compareAndSet(false, true)) {
                Log.i(TAG, "Loading hooks for ${lpparam.packageName}")
                LocationMockHook().handleLoadPackage(lpparam)
                WifiScanHook().handleLoadPackage(lpparam)
                CellInfoHook().handleLoadPackage(lpparam)
            }
        }
    }

    private fun reportActive(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            // The app's own copy of the class, not the module class loader's.
            val status = lpparam.classLoader.loadClass(ModuleStatus::class.java.name)
            XposedHelpers.findAndHookMethod(
                status,
                "isActive",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                },
            )
        }.onFailure { Log.w(TAG, "Could not report the module as active", it) }
    }

    private companion object {
        const val TAG = "LatebrisModule"
        const val SYSTEM_SERVER = "android"
    }
}
