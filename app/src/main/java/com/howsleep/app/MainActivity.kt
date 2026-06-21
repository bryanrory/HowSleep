package com.howsleep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.howsleep.app.navigation.HowSleepNavGraph
import com.howsleep.app.notification.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderScheduler.scheduleNightlyEvaluation()
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                HowSleepNavGraph(navController = navController)
            }
        }
    }
}
