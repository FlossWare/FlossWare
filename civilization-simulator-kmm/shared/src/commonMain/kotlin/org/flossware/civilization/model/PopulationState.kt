package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class PopulationState(
    val population: Long,
    val birthRate: Double,
    val deathRate: Double,
    val carryingCapacity: Double,
    val plagueActive: Boolean
) {
    init {
        require(population >= 0) { "Population cannot be negative" }
        require(carryingCapacity >= 0) { "Carrying capacity cannot be negative" }
    }

    fun withDelta(delta: Double): PopulationState {
        val newPopulation = maxOf(0L, population + delta.toLong())
        return copy(population = newPopulation)
    }

    fun withPopulation(newPopulation: Long) = copy(population = newPopulation)
    fun withCarryingCapacity(newCapacity: Double) = copy(carryingCapacity = newCapacity)
    fun withPlague(isActive: Boolean) = copy(plagueActive = isActive)
}
