package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class EconomyState(
    val wealth: Double,
    val production: Double,
    val consumption: Double,
    val workers: Long,
    val tradeSurplus: Double,
    val gdp: Double,
    val tradeRoutes: List<TradeRoute>
) {
    init {
        require(wealth >= 0) { "Wealth cannot be negative" }
        require(workers >= 0) { "Workers cannot be negative" }
    }

    fun withWealth(newWealth: Double) = copy(wealth = maxOf(0.0, newWealth))
    fun withProduction(newProduction: Double) = copy(production = newProduction)
    fun withGDP(newGdp: Double) = copy(gdp = newGdp)
    fun withTradeRoutes(newRoutes: List<TradeRoute>) = copy(tradeRoutes = newRoutes)
    fun withWorkers(newWorkers: Long) = copy(workers = maxOf(0L, newWorkers))
}
