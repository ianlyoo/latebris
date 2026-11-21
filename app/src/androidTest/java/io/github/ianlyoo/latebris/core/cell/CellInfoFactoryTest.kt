package io.github.ianlyoo.latebris.core.cell

import android.telephony.CellInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ianlyoo.latebris.data.preset.CellTower
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds cells with the same factory the hook uses and reads them back through the
 * public getters an app calls. Needs hidden-API access for the test app:
 * `adb shell settings put global hidden_api_policy 1` (LSPosed grants it to hooked apps).
 */
@RunWith(AndroidJUnit4::class)
class CellInfoFactoryTest {
    private val tower = CellTower(mcc = 450, mnc = 5, ci = 22011, pci = 101, tac = 101, earfcn = 1800)

    @Test
    fun servingCellReadsBackThroughPublicGetters() {
        val info = CellInfoFactory.create(tower, registered = true)
        val identity = info.cellIdentity

        assertEquals("450", identity.mccString)
        assertEquals("05", identity.mncString)
        assertEquals(22011, identity.ci)
        assertEquals(101, identity.pci)
        assertEquals(101, identity.tac)
        assertEquals(1800, identity.earfcn)
        assertTrue(info.isRegistered)
        assertEquals(CellInfo.CONNECTION_PRIMARY_SERVING, info.cellConnectionStatus)
        assertTrue(info.cellSignalStrength.rsrp in -95..-85)
    }

    @Test
    fun neighbourCellIsNotRegistered() {
        val info = CellInfoFactory.create(tower, registered = false)
        assertFalse(info.isRegistered)
        assertEquals(CellInfo.CONNECTION_NONE, info.cellConnectionStatus)
    }

    @Test
    fun mncIsPaddedToTwoDigitsOnly() {
        assertEquals("05", CellInfoFactory.mncString(5))
        assertEquals("45", CellInfoFactory.mncString(45))
        assertEquals("120", CellInfoFactory.mncString(120))
    }
}
