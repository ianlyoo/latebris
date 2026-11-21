package io.github.ianlyoo.latebris.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LatebrisAppRestoreSafetyTest {

    @Test
    fun sanitizeLatebrisRouteName_keepsKnownRoute() {
        assertEquals("PresetEditor", sanitizeLatebrisRouteName("PresetEditor"))
    }

    @Test
    fun sanitizeLatebrisRouteName_fallsBackForUnknownRoute() {
        assertEquals("Dashboard", sanitizeLatebrisRouteName("UnknownRoute"))
        assertEquals("Dashboard", sanitizeLatebrisRouteName(null))
    }

    @Test
    fun sanitizeDashboardTabName_keepsKnownTab() {
        assertEquals(DashboardTab.Options.name, sanitizeDashboardTabName(DashboardTab.Options.name))
    }

    @Test
    fun sanitizeDashboardTabName_fallsBackForUnknownTab() {
        assertEquals(DashboardTab.Status.name, sanitizeDashboardTabName("LegacyTab"))
        assertEquals(DashboardTab.Status.name, sanitizeDashboardTabName(null))
    }
}
