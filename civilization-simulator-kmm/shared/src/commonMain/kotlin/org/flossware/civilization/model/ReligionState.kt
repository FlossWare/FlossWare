package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class ReligionState(
    val religionShares: Map<String, Double>,
    val religiousUnity: Double,
    val stabilityBonus: Double,
    val spreadRate: Double
) {
    init {
        require(religiousUnity in 0.0..1.0) { "Religious unity must be in [0, 1]" }
        require(stabilityBonus in 0.0..1.0) { "Stability bonus must be in [0, 1]" }
    }

    fun withReligionShares(newShares: Map<String, Double>): ReligionState {
        val newUnity = newShares.values.maxOrNull() ?: 0.0
        val newBonus = newUnity * 0.2
        return copy(religionShares = newShares, religiousUnity = newUnity, stabilityBonus = newBonus)
    }

    fun withSpreadRate(newRate: Double) = copy(spreadRate = newRate)
}
