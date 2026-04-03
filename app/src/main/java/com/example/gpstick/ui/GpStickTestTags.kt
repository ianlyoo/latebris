package com.example.gpstick.ui

object GpStickTestTags {
    const val PRESET_LIST = "preset_list"
    const val START_CONTROL = "start_service_control"
    const val STOP_CONTROL = "stop_service_control"
    const val SELECTED_PRESET = "selected_preset"
    const val SIMULATION_STATUS = "simulation_status"
    const val ACTIVE_PRESET_COORDINATES = "active_preset_coordinates"
    const val NEW_PRESET_CONTROL = "new_preset_control"
    const val PRESET_EDITOR_SCREEN = "preset_editor_screen"
    const val PRESET_EDITOR_BACK = "preset_editor_back"
    const val SAVE_PRESET_CONTROL = "save_preset_control"
    const val DELETE_PRESET_CONTROL = "delete_preset_control"
    const val AUTO_FILL_CONTROL = "auto_fill_control"
    const val ADD_WIFI_ROW_CONTROL = "add_wifi_row_control"
    const val ADD_CELL_ROW_CONTROL = "add_cell_row_control"

    fun editPresetControl(presetId: String): String = "edit_preset_$presetId"

    fun removeWifiRowControl(index: Int): String = "remove_wifi_row_$index"

    fun removeCellRowControl(index: Int): String = "remove_cell_row_$index"
}
