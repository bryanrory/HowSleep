package com.howsleep.app.ui.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val durationProducer = remember { CartesianChartModelProducer() }
    val qualityProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(uiState.nights) {
        val durations = uiState.nights.mapNotNull { it.durationHours }
        val qualities = uiState.nights.mapNotNull { it.perceivedQuality?.toFloat() }

        if (durations.isNotEmpty()) {
            durationProducer.runTransaction {
                lineSeries { series(y = durations) }
            }
        }
        if (qualities.isNotEmpty()) {
            qualityProducer.runTransaction {
                lineSeries { series(y = qualities) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tendências") },
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
        } else if (uiState.nights.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Nenhuma noite com dados completos ainda.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Continue registrando suas noites para ver tendências.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrendsPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = uiState.period == period,
                            onClick = { viewModel.onPeriodChanged(period) },
                            label = { Text(period.label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    uiState.avgDurationHours?.let {
                        StatCard(label = "Duração média", value = "%.1fh".format(it))
                    }
                    uiState.avgQuality?.let {
                        StatCard(label = "Qualidade média", value = "%.1f/5".format(it))
                    }
                    uiState.avgMood?.let {
                        StatCard(label = "Humor médio", value = "%.1f/5".format(it))
                    }
                }

                if (uiState.nights.mapNotNull { it.durationHours }.isNotEmpty()) {
                    Text("Duração do sono (horas)", style = MaterialTheme.typography.titleSmall)
                    CartesianChartHost(
                        chart = rememberCartesianChart(rememberLineCartesianLayer()),
                        modelProducer = durationProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))

                if (uiState.nights.mapNotNull { it.perceivedQuality }.isNotEmpty()) {
                    Text("Qualidade percebida (1–5)", style = MaterialTheme.typography.titleSmall)
                    CartesianChartHost(
                        chart = rememberCartesianChart(rememberLineCartesianLayer()),
                        modelProducer = qualityProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
