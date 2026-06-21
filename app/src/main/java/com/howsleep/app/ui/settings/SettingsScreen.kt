package com.howsleep.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.howsleep.app.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.sessionSimulated) {
        if (uiState.sessionSimulated) {
            snackbarHostState.showSnackbar("Sessão de sono simulada com sucesso!")
            viewModel.onSimulationAcknowledged()
        }
    }

    if (uiState.showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            is24Hour = true,
        )
        TimePickerDialog(
            title = { Text("Horário do lembrete") },
            onDismissRequest = viewModel::onDismissTimePicker,
            confirmButton = {
                Button(onClick = {
                    viewModel.onReminderTimeConfirmed(timePickerState.hour, timePickerState.minute)
                }) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::onDismissTimePicker) { Text("Cancelar") }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configurações") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Lembretes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Lembrete pré-sono")
                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = viewModel::onReminderToggled,
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = viewModel::onShowTimePicker,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Horário do lembrete: %02d:%02d".format(uiState.reminderHour, uiState.reminderMinute))
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "DEBUG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sensor de sono simulado")
                    Switch(
                        checked = uiState.useMockSleep,
                        onCheckedChange = viewModel::onMockSleepToggled,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = viewModel::simulateSleepSession,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Simular sessão de sono agora")
                }
            }
        }
    }
}
