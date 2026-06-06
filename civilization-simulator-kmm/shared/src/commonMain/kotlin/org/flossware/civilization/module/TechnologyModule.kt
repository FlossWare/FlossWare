package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.random.Random

object TechnologyModule {
    private const val BASE_RESEARCH_POINTS = 10.0
    private const val UNIVERSITY_BONUS = 0.5

    fun tick(
        current: TechnologyState,
        techGraph: TechGraph,
        tradeConnectivity: Double,
        population: Long,
        random: Random
    ): ModuleResult<TechnologyState> {
        val events = mutableListOf<Event>()
        var newState = current

        val researchPoints = calculateResearchPoints(current, population)
        val nextTech = findNextResearchableTech(current, techGraph)

        if (nextTech != null) {
            val currentProgress = current.researchProgress[nextTech.id] ?: 0.0
            val newProgress = currentProgress + researchPoints

            if (newProgress >= nextTech.researchCost) {
                newState = newState.withUnlockedTech(nextTech.id)
                events.add(Event(0, "", EventType.TECHNOLOGY_UNLOCKED, EventSeverity.MAJOR,
                    "Unlocked: ${nextTech.id}", nextTech.id))
                val updatedProgress = current.researchProgress.toMutableMap()
                updatedProgress.remove(nextTech.id)
                newState = TechnologyState(newState.unlockedTechs, updatedProgress, newState.literacyRate, newState.universities)
            } else {
                newState = newState.withResearchProgress(nextTech.id, newProgress)
            }
        }

        return ModuleResult(newState, events)
    }

    private fun calculateResearchPoints(state: TechnologyState, population: Long): Double {
        val literacyMultiplier = 1.0 + state.literacyRate
        val universityMultiplier = 1.0 + (state.universities * UNIVERSITY_BONUS)
        val populationFactor = minOf(1.0, population / 1_000_000.0)
        return BASE_RESEARCH_POINTS * literacyMultiplier * universityMultiplier * populationFactor
    }

    private fun findNextResearchableTech(state: TechnologyState, graph: TechGraph): Technology? {
        return graph.getAllTechnologyIds()
            .filter { it !in state.unlockedTechs }
            .filter { isUnlocked(it, state.unlockedTechs, graph) }
            .mapNotNull { graph.getTechnology(it) }
            .minByOrNull { it.researchCost }
    }

    private fun isUnlocked(techId: String, unlocked: Set<String>, graph: TechGraph): Boolean {
        return graph.getPrerequisites(techId).all { it in unlocked }
    }
}
