package org.flossware.civilization.util

import org.flossware.civilization.model.*

class ScenarioBuilder {
    private var scenarioId: String = "scenario-${kotlin.random.Random.nextLong()}"
    private var name: String = "Unnamed Scenario"
    private var description: String = ""
    private var startYear: Int = -3000
    private var endYear: Int = 2026
    private var initialState: CivilizationState? = null
    private val techTree: MutableList<Technology> = mutableListOf()
    private var worldConstraints: WorldConstraints? = null
    private var simulationRules: SimulationRules? = null

    fun withId(id: String) = apply { scenarioId = id }
    fun withName(name: String) = apply { this.name = name }
    fun withDescription(description: String) = apply { this.description = description }
    fun withTimeRange(startYear: Int, endYear: Int) = apply { this.startYear = startYear; this.endYear = endYear }
    fun withInitialState(state: CivilizationState) = apply { initialState = state }
    fun addTechnology(tech: Technology) = apply { techTree.add(tech) }
    fun withTechTree(techs: List<Technology>) = apply { techTree.clear(); techTree.addAll(techs) }
    fun withWorldConstraints(constraints: WorldConstraints) = apply { worldConstraints = constraints }
    fun withSimulationRules(rules: SimulationRules) = apply { simulationRules = rules }

    fun build(): Scenario {
        return Scenario(
            scenarioId = scenarioId,
            name = name,
            description = description,
            startYear = startYear,
            endYear = endYear,
            initialState = requireNotNull(initialState) { "Initial state must be set" },
            techTree = techTree.toList(),
            worldConstraints = requireNotNull(worldConstraints) { "World constraints must be set" },
            simulationRules = requireNotNull(simulationRules) { "Simulation rules must be set" }
        )
    }
}
