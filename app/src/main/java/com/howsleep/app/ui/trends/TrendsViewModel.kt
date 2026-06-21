package com.howsleep.app.ui.trends

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
class TrendsViewModel @Inject constructor(
    private val habitLogRepository: HabitLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    init {
        loadData(TrendsPeriod.WEEK)
    }

    fun onPeriodChanged(period: TrendsPeriod) {
        if (_uiState.value.period == period) return
        _uiState.update { it.copy(period = period, isLoading = true) }
        loadData(period)
    }

    private fun loadData(period: TrendsPeriod) {
        viewModelScope.launch {
            habitLogRepository.getLastNNights(period.days)
                .onSuccess { nights ->
                    val items = nights.map { night ->
                        TrendNightItem(
                            epochDay = night.sleepEpochDay,
                            date = night.date,
                            durationHours = night.totalDurationMinutes?.let { it / 60f },
                            perceivedQuality = night.perceivedQuality,
                            moodScore = night.moodScore,
                            energyLevel = night.energyLevel,
                        )
                    }
                    val durations = items.mapNotNull { it.durationHours }
                    val qualities = items.mapNotNull { it.perceivedQuality?.toFloat() }
                    val moods = items.mapNotNull { it.moodScore?.toFloat() }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nights = items,
                            avgDurationHours = if (durations.isEmpty()) null else durations.average().toFloat(),
                            avgQuality = if (qualities.isEmpty()) null else qualities.average().toFloat(),
                            avgMood = if (moods.isEmpty()) null else moods.average().toFloat(),
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}
