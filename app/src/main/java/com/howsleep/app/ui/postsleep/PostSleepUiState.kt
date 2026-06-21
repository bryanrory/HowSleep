package com.howsleep.app.ui.postsleep

data class PostSleepUiState(
    val moodScore: Int = 3,
    val energyLevel: Int = 3,
    val perceivedQuality: Int = 3,
    val grogginessMinutes: Int? = null,
    val dreamRecall: Boolean = false,
    val headache: Boolean = false,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)
