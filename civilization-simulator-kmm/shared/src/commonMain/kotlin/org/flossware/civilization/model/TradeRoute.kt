package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeRoute(
    val from: String,
    val to: String,
    val goods: List<String>,
    val volume: Double,
    val tariff: Double
) {
    init {
        require(volume >= 0) { "Trade volume cannot be negative" }
        require(tariff in 0.0..1.0) { "Tariff must be in [0, 1]" }
    }
}
