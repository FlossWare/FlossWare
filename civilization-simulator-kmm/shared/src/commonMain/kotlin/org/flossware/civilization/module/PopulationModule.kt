package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.math.max
import kotlin.random.Random

object PopulationModule {
    private const val BASE_BIRTH_RATE = 0.03
    private const val BASE_DEATH_RATE = 0.02
    private const val PLAGUE_DEATH_MULTIPLIER = 3.0

    fun tick(
        current: PopulationState,
        resourceAbundance: Double,
        plagueProbability: Double,
        random: Random
    ): ModuleResult<PopulationState> {
        val events = mutableListOf<Event>()

        var plagueActive = current.plagueActive || random.nextDouble() < plagueProbability
        if (plagueActive && !current.plagueActive) {
            events.add(Event(0, "", EventType.PLAGUE, EventSeverity.CRITICAL, "Plague outbreak!", null))
        }
        if (plagueActive && random.nextDouble() < 0.2) {
            plagueActive = false
        }

        val carryingCapacity = calculateCarryingCapacity(resourceAbundance)
        val growthRate = logisticGrowth(current.population, carryingCapacity)
        val births = current.population * BASE_BIRTH_RATE * growthRate
        val mortalityRate = if (plagueActive) BASE_DEATH_RATE * PLAGUE_DEATH_MULTIPLIER else BASE_DEATH_RATE
        val deaths = current.population * mortalityRate
        val newPopulation = max(1000L, current.population + (births - deaths).toLong())

        if (newPopulation >= 10_000_000 && current.population < 10_000_000) {
            events.add(Event(0, "", EventType.POPULATION_MILESTONE, EventSeverity.MAJOR,
                "Population reaches 10 million!", newPopulation.toString()))
        }

        val newState = PopulationState(newPopulation, BASE_BIRTH_RATE, mortalityRate, carryingCapacity, plagueActive)
        return ModuleResult(newState, events)
    }

    private fun logisticGrowth(population: Long, carryingCapacity: Double): Double {
        if (carryingCapacity <= 0) return 0.0
        val ratio = population / carryingCapacity
        return max(0.0, 1.0 - ratio)
    }

    private fun calculateCarryingCapacity(resourceAbundance: Double): Double {
        val baseCapacity = 50_000_000.0
        return baseCapacity * max(0.1, resourceAbundance)
    }
}
