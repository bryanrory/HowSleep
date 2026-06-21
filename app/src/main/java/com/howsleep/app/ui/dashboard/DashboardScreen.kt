package com.howsleep.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPreSleep: () -> Unit,
    onNavigateToPostSleep: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChallenge: () -> Unit,
    onNavigateToTrends: () -> Unit,
    onNavigateToChallengeHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Últimas 7 noites") },
                actions = {
                    IconButton(onClick = onNavigateToTrends) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Tendências")
                    }
                    IconButton(onClick = onNavigateToChallengeHistory) {
                        Icon(Icons.Default.History, contentDescription = "Histórico de desafios")
                    }
                    IconButton(onClick = onNavigateToChallenge) {
                        Icon(Icons.Default.Star, contentDescription = "Desafio")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToPreSleep,
                text = { Text("Pré-sono") },
                icon = {},
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.nights.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Nenhuma noite registrada ainda.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Preencha o formulário pré-sono antes de dormir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { }
                items(uiState.nights, key = { it.sleepEpochDay }) { night ->
                    NightCard(night = night, onPostSleepClick = onNavigateToPostSleep)
                }
                item { }
            }
        }
    }
}

@Composable
private fun NightCard(night: DashboardNightItem, onPostSleepClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${night.localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))} ${night.localDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (night.isLowConfidence) {
                    Text(
                        "Dados incertos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            night.durationHours?.let {
                Text("Duração: %.1fh".format(it), style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                night.perceivedQuality?.let { Text("Qualidade: $it/5", style = MaterialTheme.typography.bodySmall) }
                night.moodScore?.let { Text("Humor: $it/5", style = MaterialTheme.typography.bodySmall) }
                night.energyLevel?.let { Text("Energia: $it/5", style = MaterialTheme.typography.bodySmall) }
            }

            if (!night.hasPostLog) {
                OutlinedButton(
                    onClick = onPostSleepClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Registrar pós-sono")
                }
            }
        }
    }
}
