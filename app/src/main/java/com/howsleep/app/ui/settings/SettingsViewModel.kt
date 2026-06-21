package com.howsleep.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.howsleep.app.data.local.PreferencesKeys
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _uiState.update { it.copy(useMockSleep = prefs[PreferencesKeys.USE_MOCK_SLEEP] ?: false) }
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
}
