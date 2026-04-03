package com.example.gpstick.core.cell

import android.os.Build
import android.telephony.CellInfoLte
import android.telephony.CellLocation
import android.telephony.TelephonyManager
import android.telephony.gsm.GsmCellLocation
import com.example.gpstick.data.preset.CellTower
import com.example.gpstick.data.preset.LocationPreset
import com.example.gpstick.data.preset.PresetManager
import com.example.gpstick.service.SimulationStateStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.random.Random

class CellInfoHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) {
            return
        }

        XposedHelpers.findAndHookMethod(
            TelephonyManager::class.java,
            "getAllCellInfo",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val towers = resolveActivePreset()?.cellTowers.orEmpty()
                    if (towers.isEmpty()) {
                        return
                    }

                    val mockedInfos = towers.mapNotNull(::buildCellInfoLte)
                    if (mockedInfos.isNotEmpty()) {
                        param.result = ArrayList(mockedInfos)
                    }
                }
            },
        )

        XposedHelpers.findAndHookMethod(
            TelephonyManager::class.java,
            "getCellLocation",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tower = resolveActivePreset()?.cellTowers?.firstOrNull() ?: return
                    param.result = buildCellLocation(tower) ?: param.result
                }
            },
        )
    }

    private fun resolveActivePreset(): LocationPreset? {
        val context = currentApplicationContext() ?: return null
        val sharedState = SimulationStateStore.readFromProvider(context)
        if (!sharedState.isRunning) {
            return null
        }

        PresetManager.initialize(context)
        val presetId = sharedState.activePresetId ?: return null
        return PresetManager.getPreset(presetId)
    }

    private fun buildCellInfoLte(tower: CellTower): CellInfoLte? = runCatching {
        val cellInfo = CellInfoLte::class.java.getDeclaredConstructor().newInstance()
        val identity = buildCellIdentityLte(tower)
        val signalStrength = buildCellSignalStrengthLte()

        invokeMethod(cellInfo, "setCellIdentity", identity)
        invokeMethod(cellInfo, "setCellSignalStrength", signalStrength)
        setBooleanFieldIfPresent(cellInfo, "mRegistered", true)
        cellInfo
    }.getOrNull()

    private fun buildCellIdentityLte(tower: CellTower): Any {
        val clazz = Class.forName("android.telephony.CellIdentityLte")
        val constructor = clazz.declaredConstructors
            .sortedByDescending { it.parameterTypes.size }
            .firstOrNull() ?: error("CellIdentityLte constructor not found")

        constructor.isAccessible = true
        val args = constructor.parameterTypes.mapIndexed { index, parameterType ->
            valueForCellIdentityParameter(index, parameterType, tower)
        }.toTypedArray()
        return constructor.newInstance(*args)
    }

    private fun buildCellSignalStrengthLte(): Any {
        val clazz = Class.forName("android.telephony.CellSignalStrengthLte")
        val constructor = clazz.declaredConstructors
            .sortedByDescending { it.parameterTypes.size }
            .firstOrNull() ?: error("CellSignalStrengthLte constructor not found")

        constructor.isAccessible = true
        val rsrp = RANDOM.nextInt(from = -95, until = -84)
        val rsrq = RANDOM.nextInt(from = -12, until = -7)
        val args = constructor.parameterTypes.mapIndexed { index, parameterType ->
            valueForSignalStrengthParameter(index, parameterType, rsrp, rsrq)
        }.toTypedArray()
        return constructor.newInstance(*args)
    }

    private fun buildCellLocation(tower: CellTower): CellLocation? = runCatching {
        GsmCellLocation().apply {
            setLacAndCid(tower.tac, tower.ci)
        }
    }.getOrNull()

    private fun valueForCellIdentityParameter(index: Int, type: Class<*>, tower: CellTower): Any? {
        if (type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType) {
            return when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N -> when (index) {
                    0 -> tower.mcc
                    1 -> tower.mnc
                    2 -> tower.ci
                    3 -> tower.pci
                    else -> tower.tac
                }

                Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> when (index) {
                    0 -> tower.mcc
                    1 -> tower.mnc
                    2 -> tower.ci
                    3 -> tower.pci
                    4 -> tower.tac
                    else -> tower.earfcn
                }

                else -> when (index) {
                    2 -> tower.ci
                    3 -> tower.pci
                    4 -> tower.tac
                    5 -> tower.earfcn
                    6 -> Int.MAX_VALUE
                    else -> 0
                }
            }
        }

        if (type == String::class.java) {
            return when (index) {
                0 -> tower.mcc.toString()
                1 -> tower.mnc.toString()
                else -> ""
            }
        }

        if (type == IntArray::class.java) {
            return intArrayOf()
        }

        if (java.util.Collection::class.java.isAssignableFrom(type)) {
            return emptyList<String>()
        }

        return null
    }

    private fun valueForSignalStrengthParameter(index: Int, type: Class<*>, rsrp: Int, rsrq: Int): Any? {
        if (type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType) {
            return when (index) {
                0 -> -70
                1 -> rsrp
                2 -> rsrq
                3 -> 30
                4 -> 10
                5 -> 1
                6 -> 0
                else -> 0
            }
        }

        return null
    }

    private fun invokeMethod(target: Any, methodName: String, argument: Any) {
        val method = target.javaClass.methods.firstOrNull {
            it.name == methodName && it.parameterTypes.size == 1
        } ?: target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == 1
        }
        method.isAccessible = true
        method.invoke(target, argument)
    }

    private fun setBooleanFieldIfPresent(target: Any, fieldName: String, value: Boolean) {
        runCatching {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setBoolean(target, value)
        }
    }

    private fun currentApplicationContext(): android.content.Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val method = activityThread.getDeclaredMethod("currentApplication")
        method.isAccessible = true
        method.invoke(null) as? android.content.Context
    }.getOrNull()

    private companion object {
        const val TARGET_PACKAGE = "com.example.gpstick"
        val RANDOM = Random(System.currentTimeMillis())
    }
}
