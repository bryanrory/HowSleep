package com.howsleep.app.ui.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howsleep.app.data.repository.AiChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ChallengeHistoryViewModel @Inject constructor(
    aiChallengeRepository: AiChallengeRepository,
) : ViewModel() {

    val uiState: StateFlow<ChallengeHistoryUiState> = aiChallengeRepository
        .getChallengeHistory()
        .map { entities ->
            ChallengeHistoryUiState(
                isLoading = false,
                challenges = entities.map { entity ->
                    ChallengeHistoryItem(
                        id = entity.id,
                        title = entity.title,
                        status = entity.status,
                        source = entity.source,
                        durationDays = entity.durationDays,
                        validFromDate = LocalDate.ofEpochDay(entity.validFromEpochDay),
                        validUntilDate = LocalDate.ofEpochDay(entity.validUntilEpochDay),
                        habitToChange = entity.habitToChange,
                        baselineValue = entity.baselineValue,
                        outcomeAverage = entity.outcomeAverage,
                        outcomeDeltaPercent = entity.outcomeDeltaPercent,
                        successMetricType = entity.successMetricType,
                    )
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ChallengeHistoryUiState())
}
