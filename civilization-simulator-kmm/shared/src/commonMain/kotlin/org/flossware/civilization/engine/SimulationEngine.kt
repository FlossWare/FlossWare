package org.flossware.civilization.engine

import org.flossware.civilization.model.*
import org.flossware.civilization.module.*
import org.flossware.civilization.util.SeedManager
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SimulationEngine(
    private val scenario: Scenario,
    baseSeed: Long
) {
    private val seedManager = SeedManager(baseSeed)
    private val techGraph = TechGraph(scenario.techTree)

    fun run(runIndex: Int): SimulationResult {
        val runRandom = seedManager.getRunRandom(runIndex)
        var state = scenario.initialState
        val allEvents = mutableListOf<Event>()

        var currentYear = scenario.startYear

        while (currentYear <= scenario.endYear) {
            val tickType = TickType.determineTickType(
                scenario.worldConstraints.climateVolatility,
                state.politics.stability
            )

            val yearRandom = seedManager.getYearRandom(runRandom, currentYear, tickType.name)

            val tickResult = executeTick(state, currentYear, tickType, yearRandom)
            state = tickResult.state

            val eventYear = currentYear
            val civId = state.id
            val yearEvents = tickResult.events.map { e ->
                Event(eventYear, civId, e.type, e.severity, e.description, e.data)
            }
            allEvents.addAll(yearEvents)

            currentYear += ceil(tickType.years).toInt()
        }

        return SimulationResult(state, allEvents)
    }

    private data class TickResult(val state: CivilizationState, val events: List<Event>)

    private fun executeTick(
        initialState: CivilizationState,
        year: Int,
        tickType: TickType,
        yearRandom: Random
    ): TickResult {
        var state = initialState
        val events = mutableListOf<Event>()

        // 1. Climate Module
        val climateRandom = SeedManager.getModuleRandom(yearRandom, "climate")
        val climateResult = ClimateModule.tick(
            state.climate,
            scenario.worldConstraints.climateVolatility,
            climateRandom
        )
        state = state.withClimate(climateResult.state)
        events.addAll(climateResult.events)

        // 2. Migration Module (placeholder)

        // 3. Population Module
        val popRandom = SeedManager.getModuleRandom(yearRandom, "population")
        val popResult = PopulationModule.tick(
            state.population,
            state.climate.getResourceAbundance(),
            scenario.worldConstraints.plagueProbability,
            popRandom
        )
        state = state.withPopulation(popResult.state)
        events.addAll(popResult.events)

        // 4. Economy Module
        val econRandom = SeedManager.getModuleRandom(yearRandom, "economy")
        val econResult = EconomyModule.tick(
            state.economy,
            state.climate.getResourceAbundance(),
            state.technology.unlockedTechs,
            state.population.population,
            econRandom
        )
        state = state.withEconomy(econResult.state)
        events.addAll(econResult.events)

        // 5. Technology Module
        val techRandom = SeedManager.getModuleRandom(yearRandom, "technology")
        val techResult = TechnologyModule.tick(
            state.technology,
            techGraph,
            calculateTradeConnectivity(state),
            state.population.population,
            techRandom
        )
        state = state.withTechnology(techResult.state)
        events.addAll(techResult.events)

        // 6. Religion Module
        val relRandom = SeedManager.getModuleRandom(yearRandom, "religion")
        val relResult = ReligionModule.tick(
            state.religion,
            calculateTradeConnectivity(state),
            relRandom
        )
        state = state.withReligion(relResult.state)
        events.addAll(relResult.events)

        // 7. Politics Module
        val polRandom = SeedManager.getModuleRandom(yearRandom, "politics")
        val polResult = PoliticsModule.tick(
            state.politics,
            calculateEconomicHealth(state),
            state.religion.religiousUnity,
            state.military.atWar,
            tickType.years,
            polRandom
        )
        state = state.withPolitics(polResult.state)
        events.addAll(polResult.events)

        // 8. Military Module
        val milRandom = SeedManager.getModuleRandom(yearRandom, "military")
        val milResult = MilitaryModule.tick(
            state.military,
            state.economy.wealth,
            state.technology.unlockedTechs,
            milRandom
        )
        state = state.withMilitary(milResult.state)
        events.addAll(milResult.events)

        // Update year
        state = state.withYear(year)

        return TickResult(state, events)
    }

    private fun calculateTradeConnectivity(state: CivilizationState): Double {
        return min(1.0, state.economy.tradeRoutes.size * 0.2)
    }

    private fun calculateEconomicHealth(state: CivilizationState): Double {
        val wealthPerCapita = state.economy.wealth / max(1L, state.population.population)
        return min(1.0, wealthPerCapita / 1000.0)
    }
}
