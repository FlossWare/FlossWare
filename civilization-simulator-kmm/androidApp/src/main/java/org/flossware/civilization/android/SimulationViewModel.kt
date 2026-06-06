package org.flossware.civilization.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.flossware.civilization.engine.MonteCarloRunner
import org.flossware.civilization.engine.SimulationEngine
import org.flossware.civilization.engine.SimulationResult
import org.flossware.civilization.scenarios.RomeEnduresScenario

data class SimulationUiState(
    val isRunning: Boolean = false,
    val mode: String = "single",
    val singleResult: SimulationResult? = null,
    val monteCarloResults: List<SimulationResult>? = null,
    val analysis: MonteCarloRunner.MonteCarloAnalysis? = null,
    val durationMs: Long = 0,
    val error: String? = null
)

class SimulationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState

    fun runSingle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, mode = "single", error = null)
            try {
                val scenario = RomeEnduresScenario.create()
                val startTime = System.currentTimeMillis()
                val engine = SimulationEngine(scenario, scenario.simulationRules.baseRandomSeed)
                val result = engine.run(0)
                val duration = System.currentTimeMillis() - startTime

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    singleResult = result,
                    durationMs = duration
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, error = e.message)
            }
        }
    }

    fun runMonteCarlo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, mode = "monte", error = null)
            try {
                val scenario = RomeEnduresScenario.create()
                val startTime = System.currentTimeMillis()
                val runner = MonteCarloRunner(scenario)
                val results = runner.runAll()
                val duration = System.currentTimeMillis() - startTime
                val analysis = MonteCarloRunner.analyze(results)

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    monteCarloResults = results,
                    analysis = analysis,
                    durationMs = duration
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, error = e.message)
            }
        }
    }
}
