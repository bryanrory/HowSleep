package com.howsleep.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howsleep.app.data.repository.HabitLogRepository
import com.howsleep.app.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    sleepRepository: SleepRepository,
    habitLogRepository: HabitLogRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        sleepRepository.getLast7Nights(),
        habitLogRepository.getLatestPostSleepLogs(7),
        habitLogRepository.getLatestPreSleepLogs(7),
    ) { sessions, postLogs, preLogs ->
        val postByEpochDay = postLogs.associateBy { it.sleepEpochDay }
        val preByEpochDay = preLogs.associateBy { it.sleepEpochDay }

        val items = sessions.map { session ->
            val post = postByEpochDay[session.sleepEpochDay]
            val pre = preByEpochDay[session.sleepEpochDay]
            DashboardNightItem(
                sleepEpochDay = session.sleepEpochDay,
                localDate = LocalDate.ofEpochDay(session.sleepEpochDay),
                durationHours = session.totalDurationMinutes / 60f,
                perceivedQuality = post?.perceivedQuality,
                moodScore = post?.moodScore,
                energyLevel = post?.energyLevel,
                hasPreLog = pre != null,
                hasPostLog = post != null,
                isLowConfidence = session.confidence < 70,
            )
        }
        DashboardUiState(nights = items, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DashboardUiState(isLoading = true),
    )
}
