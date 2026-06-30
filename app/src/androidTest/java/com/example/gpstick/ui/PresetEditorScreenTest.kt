package com.example.gpstick.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
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

    @Test
    fun coordinateFieldsPreserveTypedText() {
        var latestState = PresetEditorUiState(
            name = "Demo",
            latitude = "",
            longitude = "",
            altitude = "10.0",
        )

        composeTestRule.setContent {
            var editorState by remember { mutableStateOf(latestState) }

            PresetEditorScreen(
                state = editorState,
                onNavigateBack = {},
                onNameChanged = { value ->
                    editorState = editorState.copy(name = value)
                    latestState = editorState
                },
                onLatitudeChanged = { value ->
                    editorState = editorState.copy(latitude = value)
                    latestState = editorState
                },
                onLongitudeChanged = { value ->
                    editorState = editorState.copy(longitude = value)
                    latestState = editorState
                },
                onAltitudeChanged = { value ->
                    editorState = editorState.copy(altitude = value)
                    latestState = editorState
                },
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

        composeTestRule.onNodeWithTag(GpStickTestTags.PRESET_LATITUDE_FIELD)
            .performTextInput("３７,５６６５")
        composeTestRule.onNodeWithTag(GpStickTestTags.PRESET_LONGITUDE_FIELD)
            .performTextInput("−１２６,９７８０")

        composeTestRule.runOnIdle {
            assertEquals("３７,５６６５", latestState.latitude)
            assertEquals("−１２６,９７８０", latestState.longitude)
        }
    }
}
