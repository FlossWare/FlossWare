package org.flossware.civilization.engine

import kotlinx.coroutines.*
import org.flossware.civilization.model.Scenario

class MonteCarloRunner(private val scenario: Scenario) {
    private val numRuns = scenario.simulationRules.monteCarloRuns

    /**
     * Executes all Monte Carlo runs in parallel using coroutines.
     * On Android: Dispatchers.Default uses JVM thread pool.
     * On iOS: Dispatchers.Default uses Grand Central Dispatch.
     */
    suspend fun runAll(): List<SimulationResult> = coroutineScope {
        (0 until numRuns).map { runIndex ->
            async(Dispatchers.Default) {
                runSingle(runIndex)
            }
        }.awaitAll()
    }

    private fun runSingle(runIndex: Int): SimulationResult {
        val engine = SimulationEngine(
            scenario,
            scenario.simulationRules.baseRandomSeed
        )
        return engine.run(runIndex)
    }

    data class MonteCarloAnalysis(
        val totalRuns: Int,
        val avgPopulation: Double,
        val avgWealth: Double,
        val avgTechs: Double,
        val survivalRate: Double
    ) {
        override fun toString(): String {
            return """
                Monte Carlo Analysis ($totalRuns runs):
                  Average Population: ${"%.0f".format(avgPopulation)}
                  Average Wealth: ${"%.0f".format(avgWealth)}
                  Average Techs Unlocked: ${"%.1f".format(avgTechs)}
                  Survival Rate: ${"%.1f".format(survivalRate * 100)}%
            """.trimIndent()
        }
    }

    companion object {
        fun analyze(results: List<SimulationResult>): MonteCarloAnalysis {
            require(results.isNotEmpty()) { "No results to analyze" }

            val avgPopulation = results.map { it.finalState.population.population }.average()
            val avgWealth = results.map { it.finalState.economy.wealth }.average()
            val avgTechs = results.map { it.finalState.technology.unlockedTechs.size.toDouble() }.average()
            val survivedCount = results.count { it.finalState.population.population > 10_000 }
            val survivalRate = survivedCount.toDouble() / results.size

            return MonteCarloAnalysis(results.size, avgPopulation, avgWealth, avgTechs, survivalRate)
        }
    }
}
