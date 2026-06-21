package com.howsleep.app.ui.postsleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howsleep.app.data.repository.HabitLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostSleepViewModel @Inject constructor(
    private val repository: HabitLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostSleepUiState())
    val uiState: StateFlow<PostSleepUiState> = _uiState.asStateFlow()

    fun onMoodScoreChanged(value: Int) = _uiState.update { it.copy(moodScore = value) }
    fun onEnergyLevelChanged(value: Int) = _uiState.update { it.copy(energyLevel = value) }
    fun onPerceivedQualityChanged(value: Int) = _uiState.update { it.copy(perceivedQuality = value) }
    fun onGrogginessChanged(value: Int?) = _uiState.update { it.copy(grogginessMinutes = value) }
    fun onDreamRecallChanged(value: Boolean) = _uiState.update { it.copy(dreamRecall = value) }
    fun onHeadacheChanged(value: Boolean) = _uiState.update { it.copy(headache = value) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }
    fun onErrorDismissed() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.savePostSleepLog(
                moodScore = s.moodScore,
                energyLevel = s.energyLevel,
                perceivedQuality = s.perceivedQuality,
                morningGrogginessMinutes = s.grogginessMinutes,
                dreamRecall = s.dreamRecall,
                headache = s.headache,
                notes = s.notes.ifBlank { null },
            ).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isSaved = true) } },
                onFailure = { _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao salvar. Tente novamente.") } },
            )
        }
    }
}
