package com.howsleep.app.ui.presleep

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
class PreSleepViewModel @Inject constructor(
    private val repository: HabitLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreSleepUiState())
    val uiState: StateFlow<PreSleepUiState> = _uiState.asStateFlow()

    fun onStressChanged(value: Int) = _uiState.update { it.copy(stressLevel = value) }
    fun onExerciseDoneChanged(value: Boolean) = _uiState.update {
        it.copy(exerciseDone = value, exerciseIntensity = null, exerciseMinutes = null)
    }
    fun onCaffeineMgChanged(value: Int) = _uiState.update { it.copy(caffeineMg = value) }
    fun onCaffeineHourChanged(value: Int?) = _uiState.update { it.copy(caffeineHour = value) }
    fun onScreenTimeChanged(value: Int?) = _uiState.update { it.copy(screenTimeMinutes = value) }
    fun onLastMealTypeChanged(value: String?) = _uiState.update { it.copy(lastMealType = value) }
    fun onLastMealHourChanged(value: Int?) = _uiState.update { it.copy(lastMealHour = value) }
    fun onExerciseIntensityChanged(value: String?) = _uiState.update { it.copy(exerciseIntensity = value) }
    fun onExerciseMinutesChanged(value: Int?) = _uiState.update { it.copy(exerciseMinutes = value) }
    fun onAlcoholUnitsChanged(value: Float?) = _uiState.update { it.copy(alcoholUnits = value) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }
    fun onErrorDismissed() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.savePreSleepLog(
                stressLevel = s.stressLevel,
                exerciseDone = s.exerciseDone,
                caffeineMg = s.caffeineMg.takeIf { it > 0 },
                caffeineLastIntakeLocalHour = s.caffeineHour,
                screenTimeMinutes = s.screenTimeMinutes,
                lastMealType = s.lastMealType,
                lastMealLocalHour = s.lastMealHour,
                exerciseIntensity = s.exerciseIntensity,
                exerciseMinutesBeforeBed = s.exerciseMinutes,
                alcoholUnits = s.alcoholUnits,
                notes = s.notes.ifBlank { null },
            ).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isSaved = true) } },
                onFailure = { _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao salvar. Tente novamente.") } },
            )
        }
    }
}
