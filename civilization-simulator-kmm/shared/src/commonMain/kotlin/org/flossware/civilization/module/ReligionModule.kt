package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.random.Random

object ReligionModule {
    private const val SCHISM_THRESHOLD = 0.05
    private const val MINORITY_THRESHOLD = 0.2
    private const val UNITY_THRESHOLD = 0.6

    fun tick(current: ReligionState, tradeConnectivity: Double, random: Random): ModuleResult<ReligionState> {
        val events = mutableListOf<Event>()
        var newShares = spreadReligions(current.religionShares, current.spreadRate, tradeConnectivity, random)
        var unity = newShares.values.maxOrNull() ?: 0.0
        var stabilityBonus = unity * 0.2

        val schismOccurred = checkForSchism(newShares, unity, random)
        if (schismOccurred) {
            events.add(Event(0, "", EventType.RELIGIOUS_SCHISM, EventSeverity.MAJOR,
                "Religious schism splits the dominant faith!", null))
            newShares = splitLargestReligion(newShares, random)
            unity = newShares.values.maxOrNull() ?: 0.0
            stabilityBonus = unity * 0.2
        }

        val newState = ReligionState(newShares, unity, stabilityBonus, current.spreadRate)
        return ModuleResult(newState, events)
    }

    private fun spreadReligions(currentShares: Map<String, Double>, spreadRate: Double, tradeConnectivity: Double, random: Random): Map<String, Double> {
        val newShares = currentShares.toMutableMap()
        val effectiveSpreadRate = spreadRate * (1.0 + tradeConnectivity * 0.5)
        for ((religion, currentShare) in currentShares) {
            val change = (random.nextDouble() * 2.0 - 1.0) * effectiveSpreadRate
            newShares[religion] = (currentShare + change).coerceIn(0.0, 1.0)
        }
        return normalizeShares(newShares)
    }

    private fun checkForSchism(shares: Map<String, Double>, unity: Double, random: Random): Boolean {
        val hasSignificantMinority = shares.values.any { it > MINORITY_THRESHOLD && it < unity }
        return hasSignificantMinority && unity < UNITY_THRESHOLD && random.nextDouble() < SCHISM_THRESHOLD
    }

    private fun splitLargestReligion(shares: Map<String, Double>, random: Random): Map<String, Double> {
        val newShares = shares.toMutableMap()
        val largestReligion = shares.maxByOrNull { it.value }?.key ?: return shares
        val originalShare = shares[largestReligion] ?: return shares
        val splitRatio = 0.6 + (random.nextDouble() * 0.2 - 0.1)
        newShares[largestReligion] = originalShare * splitRatio
        newShares["$largestReligion (Reformed)"] = originalShare * (1.0 - splitRatio)
        return normalizeShares(newShares)
    }

    private fun normalizeShares(shares: Map<String, Double>): Map<String, Double> {
        val total = shares.values.sum()
        if (total <= 0) return shares
        return shares.mapValues { it.value / total }
    }
}
