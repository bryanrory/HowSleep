package com.howsleep.app.ui.settings

data class SettingsUiState(
    val useMockSleep: Boolean = false,
    val sessionSimulated: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 22,
    val reminderMinute: Int = 30,
    val showTimePicker: Boolean = false,
)
