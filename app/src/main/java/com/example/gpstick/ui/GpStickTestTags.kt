package com.example.gpstick.ui

object GpStickTestTags {
    const val DASHBOARD_DRAWER_OPEN = "dashboard_drawer_open"

    const val DASHBOARD_TAB_PRESETS = "dashboard_tab_presets"
    const val DASHBOARD_TAB_STATUS = "dashboard_tab_status"
    const val DASHBOARD_TAB_OPTIONS = "dashboard_tab_options"
    const val DASHBOARD_TAB_HELP = "dashboard_tab_help"

    const val DASHBOARD_PRESETS_PANEL = "dashboard_presets_panel"
    const val DASHBOARD_STATUS_PANEL = "dashboard_status_panel"
    const val DASHBOARD_OPTIONS_PANEL = "dashboard_options_panel"
    const val DASHBOARD_HELP_PANEL = "dashboard_help_panel"

    const val CAPTURE_CURRENT_STATE_CONTROL = "capture_current_state_control"

    const val PRESET_LIST = "preset_list"
    const val START_CONTROL = "start_service_control"
    const val STOP_CONTROL = "stop_service_control"
    const val SELECTED_PRESET = "selected_preset"
    const val SIMULATION_STATUS = "simulation_status"
    const val PERMISSION_STATUS = "permission_status"
    const val LOCATION_PERMISSION_STATUS = "location_permission_status"
    const val NOTIFICATION_PERMISSION_STATUS = "notification_permission_status"
    const val REQUEST_PERMISSIONS_CONTROL = "request_permissions_control"
    const val FEATURES_ENABLED_TOGGLE = "features_enabled_toggle"
    const val GPS_MOCK_ENABLED_TOGGLE = "gps_mock_enabled_toggle"
    const val WIFI_MOCK_ENABLED_TOGGLE = "wifi_mock_enabled_toggle"
    const val CELL_MOCK_ENABLED_TOGGLE = "cell_mock_enabled_toggle"
    const val MOVEMENT_SIMULATION_ENABLED_TOGGLE = "movement_simulation_enabled_toggle"
    const val ACTIVE_PRESET_COORDINATES = "active_preset_coordinates"
    const val NEW_PRESET_CONTROL = "new_preset_control"
    const val PRESET_EDITOR_SCREEN = "preset_editor_screen"
    const val PRESET_EDITOR_BACK = "preset_editor_back"
    const val PRESET_NAME_FIELD = "preset_name_field"
    const val PRESET_LATITUDE_FIELD = "preset_latitude_field"
    const val PRESET_LONGITUDE_FIELD = "preset_longitude_field"
    const val PRESET_ALTITUDE_FIELD = "preset_altitude_field"
    const val SAVE_PRESET_CONTROL = "save_preset_control"
    const val DELETE_PRESET_CONTROL = "delete_preset_control"
    const val AUTO_FILL_CONTROL = "auto_fill_control"
    const val ADD_WIFI_ROW_CONTROL = "add_wifi_row_control"
    const val ADD_CELL_ROW_CONTROL = "add_cell_row_control"

    fun editPresetControl(presetId: String): String = "edit_preset_$presetId"

    fun removeWifiRowControl(index: Int): String = "remove_wifi_row_$index"

    fun removeCellRowControl(index: Int): String = "remove_cell_row_$index"
}
