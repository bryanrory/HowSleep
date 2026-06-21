package com.howsleep.app.ui.presleep

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
import androidx.compose.material3.FilterChip
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
fun PreSleepScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: PreSleepViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateToDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Como foi seu dia?") })
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
            // Nível de estresse
            Text("Nível de estresse hoje", style = MaterialTheme.typography.titleSmall)
            ScoreSlider(
                value = uiState.stressLevel,
                onValueChange = viewModel::onStressChanged,
            )

            // Exercitou hoje?
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Fez exercício hoje?", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.exerciseDone,
                    onCheckedChange = viewModel::onExerciseDoneChanged,
                )
            }

            if (uiState.exerciseDone) {
                Text("Intensidade do exercício", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW" to "Leve", "MODERATE" to "Moderado", "HIGH" to "Intenso").forEach { (value, label) ->
                        FilterChip(
                            selected = uiState.exerciseIntensity == value,
                            onClick = { viewModel.onExerciseIntensityChanged(value) },
                            label = { Text(label) },
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.exerciseMinutes?.toString() ?: "",
                    onValueChange = { viewModel.onExerciseMinutesChanged(it.toIntOrNull()) },
                    label = { Text("Minutos antes de dormir") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Cafeína
            Text("Cafeína (mg)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Nenhuma", 50 to "50mg", 100 to "100mg", 200 to "200mg+").forEach { (value, label) ->
                    FilterChip(
                        selected = uiState.caffeineMg == value,
                        onClick = { viewModel.onCaffeineMgChanged(value) },
                        label = { Text(label) },
                    )
                }
            }

            // Tempo de tela 2h antes
            OutlinedTextField(
                value = uiState.screenTimeMinutes?.toString() ?: "",
                onValueChange = { viewModel.onScreenTimeChanged(it.toIntOrNull()) },
                label = { Text("Tela nas últimas 2h (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // Última refeição
            Text("Tipo da última refeição", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("LIGHT" to "Leve", "SNACK" to "Lanche", "HEAVY" to "Pesada").forEach { (value, label) ->
                    FilterChip(
                        selected = uiState.lastMealType == value,
                        onClick = { viewModel.onLastMealTypeChanged(value.takeIf { uiState.lastMealType != value }) },
                        label = { Text(label) },
                    )
                }
            }

            // Álcool
            OutlinedTextField(
                value = uiState.alcoholUnits?.toString() ?: "",
                onValueChange = { viewModel.onAlcoholUnitsChanged(it.toFloatOrNull()) },
                label = { Text("Doses de álcool (0 se nenhuma)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            // Notas
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
