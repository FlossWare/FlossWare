package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object EconomyModule {
    private const val BASE_PRODUCTIVITY = 1.0
    private const val PER_CAPITA_CONSUMPTION = 0.15
    private const val AVERAGE_PRICE_LEVEL = 1.0
    private const val TRADE_SURPLUS_MODIFIER = 0.2
    private const val BOOM_THRESHOLD = 0.5
    private const val COLLAPSE_THRESHOLD = -0.3
    private const val WORKFORCE_RATIO = 0.15
    private const val WEALTH_PRODUCTIVITY_BONUS = 0.1

    fun tick(
        current: EconomyState,
        resourceAbundance: Double,
        unlockedTechs: Set<String>,
        population: Long,
        random: Random
    ): ModuleResult<EconomyState> {
        require(population >= 0) { "Population cannot be negative: $population" }
        val events = mutableListOf<Event>()

        val workers = max(1L, (population * WORKFORCE_RATIO).toLong())
        val techMultiplier = calculateTechMultiplier(unlockedTechs)
        var productivity = BASE_PRODUCTIVITY * techMultiplier
        val wealthBonus = min(1.0, (current.wealth / 10_000_000.0) * WEALTH_PRODUCTIVITY_BONUS)
        productivity *= (1.0 + wealthBonus)

        val production = workers * productivity * resourceAbundance
        val consumption = population * PER_CAPITA_CONSUMPTION
        val tradeSurplus = calculateTradeSurplus(current)
        val wealthDelta = (production - consumption) * (1.0 + tradeSurplus * TRADE_SURPLUS_MODIFIER)
        val newWealth = current.wealth + wealthDelta
        val gdp = production * AVERAGE_PRICE_LEVEL

        if (current.wealth > 0) {
            val wealthChangeRatio = wealthDelta / current.wealth
            if (wealthChangeRatio > BOOM_THRESHOLD) {
                events.add(Event(0, "", EventType.ECONOMIC_BOOM, EventSeverity.MAJOR,
                    "Economic boom! Wealth increased by ${"%.1f".format(wealthChangeRatio * 100)}%", wealthDelta.toString()))
            } else if (wealthChangeRatio < COLLAPSE_THRESHOLD) {
                events.add(Event(0, "", EventType.ECONOMIC_COLLAPSE, EventSeverity.CRITICAL,
                    "Economic collapse! Wealth decreased by ${"%.1f".format(abs(wealthChangeRatio) * 100)}%", wealthDelta.toString()))
            }
        }

        val newState = EconomyState(max(0.0, newWealth), production, consumption, workers, tradeSurplus, gdp, current.tradeRoutes)
        return ModuleResult(newState, events)
    }

    private fun calculateTechMultiplier(unlockedTechs: Set<String>): Double {
        var multiplier = 1.0
        if ("agriculture" in unlockedTechs) multiplier *= 1.2
        if ("irrigation" in unlockedTechs) multiplier *= 1.15
        if ("bronze_working" in unlockedTechs) multiplier *= 1.1
        if ("iron_working" in unlockedTechs) multiplier *= 1.2
        if ("currency" in unlockedTechs) multiplier *= 1.25
        if ("engineering" in unlockedTechs) multiplier *= 1.15
        if ("banking" in unlockedTechs) multiplier *= 1.3
        if ("machinery" in unlockedTechs) multiplier *= 1.2
        if ("steam_power" in unlockedTechs) multiplier *= 1.4
        if ("industrialization" in unlockedTechs) multiplier *= 1.5
        return multiplier
    }

    private fun calculateTradeSurplus(state: EconomyState): Double {
        if (state.tradeRoutes.isEmpty()) return 0.0
        val totalValue = state.tradeRoutes.sumOf { it.volume * (1.0 - it.tariff) }
        return totalValue / state.tradeRoutes.size
    }
}
