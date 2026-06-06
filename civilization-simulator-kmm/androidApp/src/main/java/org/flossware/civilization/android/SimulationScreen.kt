package org.flossware.civilization.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(viewModel: SimulationViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Civilization Simulator") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Rome Survives to Modern Era", style = MaterialTheme.typography.headlineSmall)
            Text("What if the Western Roman Empire endured through history?",
                style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.runSingle() }, enabled = !uiState.isRunning) {
                    Text("Run Single")
                }
                Button(onClick = { viewModel.runMonteCarlo() }, enabled = !uiState.isRunning) {
                    Text("Monte Carlo (50 runs)")
                }
            }

            if (uiState.isRunning) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Running simulation...", modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            uiState.error?.let { error ->
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }

            uiState.singleResult?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Single Run Results", fontWeight = FontWeight.Bold)
                        Text("Duration: ${uiState.durationMs} ms")
                        Text("Final Year: ${result.finalState.year}")
                        Text("Population: ${result.finalState.population.population}")
                        Text("Wealth: ${"%.0f".format(result.finalState.economy.wealth)}")
                        Text("Techs: ${result.finalState.technology.unlockedTechs.size}")
                        Text("Army: ${result.finalState.military.armySize}")
                        Text("Stability: ${"%.2f".format(result.finalState.politics.stability)}")
                        Text("Events: ${result.events.size}")
                    }
                }
            }

            uiState.analysis?.let { analysis ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Monte Carlo Analysis", fontWeight = FontWeight.Bold)
                        Text("Duration: ${uiState.durationMs} ms")
                        Text("Total Runs: ${analysis.totalRuns}")
                        Text("Avg Population: ${"%.0f".format(analysis.avgPopulation)}")
                        Text("Avg Wealth: ${"%.0f".format(analysis.avgWealth)}")
                        Text("Avg Techs: ${"%.1f".format(analysis.avgTechs)}")
                        Text("Survival Rate: ${"%.1f".format(analysis.survivalRate * 100)}%")
                    }
                }
            }
        }
    }
}
