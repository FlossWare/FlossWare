package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class Scenario(
    val scenarioId: String,
    val name: String,
    val description: String,
    val startYear: Int,
    val endYear: Int,
    val initialState: CivilizationState,
    val techTree: List<Technology>,
    val worldConstraints: WorldConstraints,
    val simulationRules: SimulationRules
) {
    init {
        require(startYear < endYear) { "Start year must be before end year" }
    }
}
