package com.howsleep.app.ui.presleep

data class PreSleepUiState(
    val stressLevel: Int = 3,
    val exerciseDone: Boolean = false,
    val caffeineMg: Int = 0,
    val caffeineHour: Int? = null,
    val screenTimeMinutes: Int? = null,
    val lastMealType: String? = null,
    val lastMealHour: Int? = null,
    val exerciseIntensity: String? = null,
    val exerciseMinutes: Int? = null,
    val alcoholUnits: Float? = null,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)
