package io.github.ianlyoo.latebris.core.cell

import android.telephony.CellInfo
import android.telephony.TelephonyManager
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ianlyoo.latebris.data.preset.CellTower
import io.github.ianlyoo.latebris.hook.HookStateReader
import java.util.concurrent.Executor

/**
 * Replaces the cell list an app reads through getAllCellInfo(), requestCellInfoUpdate()
 * and the legacy getCellLocation() with the preset's LTE towers; the first tower is the
 * registered (serving) cell.
 */
class CellInfoHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        install(lpparam, "getAllCellInfo") {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java,
                "getAllCellInfo",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        presetCells()?.let { param.result = it }
                    }
                },
            )
        }

        install(lpparam, "requestCellInfoUpdate") {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java,
                "requestCellInfoUpdate",
                Executor::class.java,
                TelephonyManager.CellInfoCallback::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val original = param.args[1] as? TelephonyManager.CellInfoCallback ?: return
                        param.args[1] = object : TelephonyManager.CellInfoCallback() {
                            override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                                original.onCellInfo(presetCells() ?: cellInfo)
                            }

                            override fun onError(errorCode: Int, detail: Throwable?) {
                                original.onError(errorCode, detail)
                            }
                        }
                    }
                },
            )
        }

        install(lpparam, "getCellLocation") {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java,
                "getCellLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val tower = HookStateReader.current()?.cellTowers?.firstOrNull() ?: return
                        param.result = GsmCellLocation().apply { setLacAndCid(tower.tac, tower.ci) }
                    }
                },
            )
        }
    }

    /** The preset's towers as CellInfo, or null to leave the real list untouched. */
    private fun presetCells(): MutableList<CellInfo>? {
        val towers = HookStateReader.current()?.cellTowers?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { towers.mapIndexed(::toCellInfo).toMutableList<CellInfo>() }
            .onFailure { Log.w(TAG, "Could not build preset cells; leaving the real list", it) }
            .getOrNull()
    }

    private fun toCellInfo(index: Int, tower: CellTower): CellInfo =
        CellInfoFactory.create(tower, registered = index == 0)

    private fun install(lpparam: XC_LoadPackage.LoadPackageParam, method: String, hook: () -> Unit) {
        runCatching(hook)
            .onSuccess { Log.i(TAG, "Installed $method hook for ${lpparam.packageName}") }
            .onFailure { Log.w(TAG, "Failed to install $method hook for ${lpparam.packageName}", it) }
    }

    private companion object {
        const val TAG = "LatebrisCellHook"
    }
}
