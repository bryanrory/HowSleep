package com.howsleep.app.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import kotlin.math.abs

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChallengeHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de desafios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
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
        } else if (uiState.challenges.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Nenhum desafio concluído ainda.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Complete seu primeiro desafio para ver o histórico aqui.",
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
                items(uiState.challenges, key = { it.id }) { challenge ->
                    ChallengeHistoryCard(challenge)
                }
                item { }
            }
        }
    }
}

@Composable
private fun ChallengeHistoryCard(item: ChallengeHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(status = item.status)
            }

            Text(
                text = "${item.validFromDate.format(dateFormatter)} – ${item.validUntilDate.format(dateFormatter)} · ${item.durationDays} dias",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(habitLabel(item.habitToChange), style = MaterialTheme.typography.labelSmall) },
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(sourceLabel(item.source), style = MaterialTheme.typography.labelSmall) },
                )
            }

            if (item.outcomeDeltaPercent != null && item.outcomeAverage != null) {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                OutcomeDeltaRow(item)
            }
        }
    }
}

@Composable
private fun OutcomeDeltaRow(item: ChallengeHistoryItem) {
    val delta = item.outcomeDeltaPercent!!
    val deltaSign = if (delta >= 0) "+" else ""
    val deltaColor = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val metricLabel = when (item.successMetricType) {
        "SLEEP_DURATION" -> "Duração (h)"
        "MOOD_SCORE" -> "Humor"
        "ENERGY_LEVEL" -> "Energia"
        "PERCEIVED_QUALITY" -> "Qualidade"
        else -> item.successMetricType
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = metricLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Base %.1f  →  Resultado %.1f".format(item.baselineValue, item.outcomeAverage),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "$deltaSign${"%.1f".format(abs(delta))}%",
            style = MaterialTheme.typography.titleSmall,
            color = deltaColor,
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "COMPLETED" -> "Concluído" to MaterialTheme.colorScheme.primary
        "ABANDONED" -> "Abandonado" to MaterialTheme.colorScheme.error
        "EXPIRED" -> "Expirado" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> status to MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

private fun habitLabel(habit: String): String = when (habit) {
    "CAFFEINE" -> "Cafeína"
    "SCREEN_TIME" -> "Tela"
    "STRESS" -> "Estresse"
    "MEAL_TIMING" -> "Alimentação"
    "EXERCISE" -> "Exercício"
    "ALCOHOL" -> "Álcool"
    "SLEEP_SCHEDULE" -> "Horário"
    else -> habit
}

private fun sourceLabel(source: String): String = when (source) {
    "AI_API" -> "IA"
    "LOCAL_ENGINE" -> "Local"
    "STATIC_DEFAULT" -> "Padrão"
    else -> source
}
