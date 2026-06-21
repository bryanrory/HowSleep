package com.howsleep.app.ui.postsleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.howsleep.app.ui.components.ScoreSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostSleepScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: PostSleepViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateToDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Como foi sua noite?") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Humor ao acordar", style = MaterialTheme.typography.titleSmall)
            ScoreSlider(
                value = uiState.moodScore,
                onValueChange = viewModel::onMoodScoreChanged,
            )

            Text("Nível de energia", style = MaterialTheme.typography.titleSmall)
            ScoreSlider(
                value = uiState.energyLevel,
                onValueChange = viewModel::onEnergyLevelChanged,
            )

            Text("Qualidade percebida do sono", style = MaterialTheme.typography.titleSmall)
            ScoreSlider(
                value = uiState.perceivedQuality,
                onValueChange = viewModel::onPerceivedQualityChanged,
            )

            OutlinedTextField(
                value = uiState.grogginessMinutes?.toString() ?: "",
                onValueChange = { viewModel.onGrogginessChanged(it.toIntOrNull()) },
                label = { Text("Sonolência ao acordar (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Lembrou de sonhos?", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.dreamRecall,
                    onCheckedChange = viewModel::onDreamRecallChanged,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Acordou com dor de cabeça?", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.headache,
                    onCheckedChange = viewModel::onHeadacheChanged,
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Observações (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text("Salvar")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
