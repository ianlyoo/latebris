package io.github.ianlyoo.latebris.core.cell

import android.os.SystemClock
import android.telephony.CellIdentityLte
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthLte
import android.telephony.ClosedSubscriberGroupInfo
import io.github.ianlyoo.latebris.data.preset.CellTower
import kotlin.random.Random

/**
 * Builds the [CellInfoLte] an app receives for a preset tower, through the hidden
 * constructors and setters of API 30+ (minSdk is 31), each looked up by its exact
 * parameter types. A missing signature throws instead of producing a half-filled cell.
 */
object CellInfoFactory {
    private val random = Random(System.nanoTime())

    fun create(tower: CellTower, registered: Boolean): CellInfoLte {
        val info = cellInfoConstructor.newInstance()
        setCellIdentity.invoke(info, identity(tower))
        setCellSignalStrength.invoke(info, signalStrength())
        setRegistered.invoke(info, registered)
        setTimeStamp.invoke(info, SystemClock.elapsedRealtimeNanos())
        setCellConnectionStatus.invoke(
            info,
            if (registered) CellInfo.CONNECTION_PRIMARY_SERVING else CellInfo.CONNECTION_NONE,
        )
        return info
    }

    /** MNCs are two digits in most countries; the preset stores the number, so pad to two. */
    fun mncString(mnc: Int): String = if (mnc < 100) "%02d".format(mnc) else mnc.toString()

    private fun identity(tower: CellTower): CellIdentityLte = identityConstructor.newInstance(
        tower.ci,
        tower.pci,
        tower.tac,
        tower.earfcn,
        intArrayOf(),
        Int.MAX_VALUE, // bandwidth unavailable
        tower.mcc.toString(),
        mncString(tower.mnc),
        null,
        null,
        emptyList<String>(),
        null,
    )

    private fun signalStrength(): CellSignalStrengthLte = signalConstructor.newInstance(
        -70, // rssi
        random.nextInt(-95, -84), // rsrp
        random.nextInt(-12, -7), // rsrq
        30, // rssnr
        1, // cqiTableIndex
        10, // cqi
        Int.MAX_VALUE, // timingAdvance unavailable
    )

    private val int = Int::class.javaPrimitiveType!!

    private val identityConstructor by lazy {
        CellIdentityLte::class.java.getDeclaredConstructor(
            int, int, int, int, IntArray::class.java, int,
            String::class.java, String::class.java, String::class.java, String::class.java,
            Collection::class.java, ClosedSubscriberGroupInfo::class.java,
        ).apply { isAccessible = true }
    }

    private val signalConstructor by lazy {
        CellSignalStrengthLte::class.java.getDeclaredConstructor(int, int, int, int, int, int, int)
            .apply { isAccessible = true }
    }

    private val cellInfoConstructor by lazy {
        CellInfoLte::class.java.getDeclaredConstructor().apply { isAccessible = true }
    }

    private val setCellIdentity by lazy { method(CellInfoLte::class.java, "setCellIdentity", CellIdentityLte::class.java) }
    private val setCellSignalStrength by lazy {
        method(CellInfoLte::class.java, "setCellSignalStrength", CellSignalStrengthLte::class.java)
    }
    private val setRegistered by lazy { method(CellInfo::class.java, "setRegistered", Boolean::class.javaPrimitiveType!!) }
    private val setTimeStamp by lazy { method(CellInfo::class.java, "setTimeStamp", Long::class.javaPrimitiveType!!) }
    private val setCellConnectionStatus by lazy { method(CellInfo::class.java, "setCellConnectionStatus", int) }

    private fun method(owner: Class<*>, name: String, vararg types: Class<*>) =
        owner.getDeclaredMethod(name, *types).apply { isAccessible = true }
}
