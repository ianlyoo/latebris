package com.example.gpstick.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GpStickAppRestoreSafetyTest {

    @Test
    fun sanitizeGpStickRouteName_keepsKnownRoute() {
        assertEquals("PresetEditor", sanitizeGpStickRouteName("PresetEditor"))
    }

    @Test
    fun sanitizeGpStickRouteName_fallsBackForUnknownRoute() {
        assertEquals("Dashboard", sanitizeGpStickRouteName("UnknownRoute"))
        assertEquals("Dashboard", sanitizeGpStickRouteName(null))
    }

    @Test
    fun sanitizeDashboardTabName_keepsKnownTab() {
        assertEquals(DashboardTab.Options.name, sanitizeDashboardTabName(DashboardTab.Options.name))
    }

    @Test
    fun sanitizeDashboardTabName_fallsBackForUnknownTab() {
        assertEquals(DashboardTab.Presets.name, sanitizeDashboardTabName("LegacyTab"))
        assertEquals(DashboardTab.Presets.name, sanitizeDashboardTabName(null))
    }
}
