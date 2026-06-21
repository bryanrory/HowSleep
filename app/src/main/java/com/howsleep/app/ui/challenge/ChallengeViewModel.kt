package com.howsleep.app.ui.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howsleep.app.data.db.entity.AiChallengeEntity
import com.howsleep.app.data.repository.AiChallengeRepository
import com.howsleep.app.domain.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val aiChallengeRepository: AiChallengeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    init {
        observeActiveChallenge()
    }

    private fun observeActiveChallenge() {
        viewModelScope.launch {
            aiChallengeRepository.getActiveChallenge().collect { challenge ->
                if (challenge == null) {
                    _uiState.update { it.copy(isLoading = false, hasActiveChallenge = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, hasActiveChallenge = true) }
                    updateFromChallenge(challenge)
                }
            }
        }
    }

    private fun updateFromChallenge(challenge: AiChallengeEntity) {
        val zone = ZoneId.of(TimeUtils.currentTimezoneId())
        val todayEpochDay = LocalDate.now(zone).toEpochDay()
        val daysElapsed = (todayEpochDay - challenge.validFromEpochDay + 1)
            .coerceIn(0, challenge.durationDays.toLong()).toInt()

        _uiState.update { state ->
            state.copy(
                title = challenge.title,
                description = challenge.description,
                habitInstruction = challenge.habitChangeInstruction,
                source = challenge.source,
                durationDays = challenge.durationDays,
                daysElapsed = daysElapsed,
                status = challenge.status,
                successMetricType = challenge.successMetricType,
                successMetricTarget = challenge.successMetricTarget,
                successMetricDirection = challenge.successMetricDirection,
                baselineValue = challenge.baselineValue,
            )
        }
    }

    fun abandonChallenge() {
        viewModelScope.launch {
            val challenge = aiChallengeRepository.getActiveChallengeOnce() ?: return@launch
            aiChallengeRepository.updateChallenge(challenge.copy(status = "ABANDONED"))
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Erro ao abandonar o desafio.") }
                }
        }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(errorMessage = null) }
}
