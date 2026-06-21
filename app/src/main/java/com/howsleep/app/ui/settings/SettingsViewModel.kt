package com.howsleep.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.howsleep.app.data.local.PreferencesKeys
import com.howsleep.app.notification.ReminderScheduler
import com.howsleep.app.sleep.MockSleepWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val workManager: WorkManager,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        useMockSleep = prefs[PreferencesKeys.USE_MOCK_SLEEP] ?: false,
                        reminderEnabled = prefs[PreferencesKeys.REMINDER_ENABLED] ?: false,
                        reminderHour = prefs[PreferencesKeys.REMINDER_HOUR] ?: 22,
                        reminderMinute = prefs[PreferencesKeys.REMINDER_MINUTE] ?: 30,
                    )
                }
            }
        }
    }

    fun onMockSleepToggled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.USE_MOCK_SLEEP] = enabled }
        }
    }

    fun simulateSleepSession() {
        workManager.enqueue(OneTimeWorkRequestBuilder<MockSleepWorker>().build())
        _uiState.update { it.copy(sessionSimulated = true) }
    }

    fun onSimulationAcknowledged() {
        _uiState.update { it.copy(sessionSimulated = false) }
    }

    fun onReminderToggled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.REMINDER_ENABLED] = enabled }
            if (enabled) {
                val state = _uiState.value
                reminderScheduler.schedulePreSleepReminder(state.reminderHour, state.reminderMinute)
            } else {
                reminderScheduler.cancelPreSleepReminder()
            }
        }
    }

    fun onShowTimePicker() {
        _uiState.update { it.copy(showTimePicker = true) }
    }

    fun onDismissTimePicker() {
        _uiState.update { it.copy(showTimePicker = false) }
    }

    fun onReminderTimeConfirmed(hour: Int, minute: Int) {
        viewModelScope.launch {
            dataStore.edit {
                it[PreferencesKeys.REMINDER_HOUR] = hour
                it[PreferencesKeys.REMINDER_MINUTE] = minute
            }
            if (_uiState.value.reminderEnabled) {
                reminderScheduler.schedulePreSleepReminder(hour, minute)
            }
        }
        _uiState.update { it.copy(showTimePicker = false) }
    }
}
