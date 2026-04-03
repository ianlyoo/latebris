package com.example.gpstick.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresetEditorScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pressingSystemBackTriggersNavigateBack() {
        var navigateBackCalls = 0

        composeTestRule.setContent {
            PresetEditorScreen(
                state = PresetEditorUiState(
                    name = "Demo",
                    latitude = "37.0",
                    longitude = "-122.0",
                    altitude = "10.0",
                ),
                onNavigateBack = { navigateBackCalls += 1 },
                onNameChanged = {},
                onLatitudeChanged = {},
                onLongitudeChanged = {},
                onAltitudeChanged = {},
                onAddWifiNetwork = {},
                onUpdateWifiNetwork = { _, _, _ -> },
                onRemoveWifiNetwork = {},
                onAddCellTower = {},
                onUpdateCellTower = { _, _, _ -> },
                onRemoveCellTower = {},
                onAutoFill = {},
                isAutoFillInProgress = false,
                onSave = {},
                onDelete = {},
            )
        }

        composeTestRule.onNodeWithTag(GpStickTestTags.PRESET_EDITOR_SCREEN).assertIsDisplayed()

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertEquals(1, navigateBackCalls)
    }
}
