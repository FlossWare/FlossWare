package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class SimulationRules(
    val timeStep: String,
    val deterministicReproducible: Boolean,
    val baseRandomSeed: Long,
    val monteCarloRuns: Int,
    val parallelThreads: Int
) {
    init {
        require(monteCarloRuns >= 1) { "Monte Carlo runs must be at least 1" }
        require(parallelThreads >= 1) { "Parallel threads must be at least 1" }
    }
}
