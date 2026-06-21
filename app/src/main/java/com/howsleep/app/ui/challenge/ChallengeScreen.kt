package com.howsleep.app.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAbandonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Desafio Ativo") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!uiState.hasActiveChallenge) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nenhum desafio ativo",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Complete o formulário pós-sono para receber um desafio personalizado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Badge de origem
            SourceBadge(source = uiState.source)

            // Título e descrição
            Text(text = uiState.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = uiState.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Instrução do hábito
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("O que fazer", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.habitInstruction,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Progresso do desafio
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Progresso", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${uiState.daysElapsed} / ${uiState.durationDays} dias",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.daysElapsed.toFloat() / uiState.durationDays.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Métrica de sucesso
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Métrica de Sucesso", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    val metricLabel = metricLabel(uiState.successMetricType)
                    val directionLabel = if (uiState.successMetricDirection == "ABOVE") "≥" else "≤"
                    Text(
                        text = "$metricLabel $directionLabel ${uiState.successMetricTarget}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Linha de base: ${uiState.baselineValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Botão de abandonar
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showAbandonDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Abandonar desafio")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Abandonar desafio?") },
            text = { Text("Esta ação marcará o desafio como abandonado. Um novo desafio será gerado após a próxima noite completa.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.abandonChallenge()
                        showAbandonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Abandonar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun SourceBadge(source: String) {
    val label = when (source) {
        "AI_API" -> "Personalizado por IA"
        "LOCAL_ENGINE" -> "Análise Local"
        "STATIC_DEFAULT" -> "Dica Geral"
        else -> source
    }
    val color = when (source) {
        "AI_API" -> MaterialTheme.colorScheme.tertiary
        "LOCAL_ENGINE" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

private fun metricLabel(metricType: String): String = when (metricType) {
    "SLEEP_DURATION" -> "Duração do sono (horas)"
    "MOOD_SCORE" -> "Humor ao acordar (1–5)"
    "ENERGY_LEVEL" -> "Nível de energia (1–5)"
    "PERCEIVED_QUALITY" -> "Qualidade percebida (1–5)"
    else -> metricType
}
