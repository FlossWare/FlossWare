package org.flossware.civilization.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

@Serializable
data class ClimateState(
    val temperatureAnomaly: Double,
    val droughtIndex: Double,
    val stormFrequency: Double,
    val seaLevelRise_mm: Double
) {
    init {
        require(temperatureAnomaly in -10.0..10.0) { "Temperature anomaly out of range [-10, 10]: $temperatureAnomaly" }
        require(droughtIndex in 0.0..1.0) { "Drought index must be in [0, 1]" }
        require(stormFrequency >= 0) { "Storm frequency cannot be negative" }
    }

    fun withTemperatureAnomaly(newAnomaly: Double) = copy(temperatureAnomaly = newAnomaly)
    fun withDroughtIndex(newIndex: Double) = copy(droughtIndex = newIndex)
    fun withStormFrequency(newFrequency: Double) = copy(stormFrequency = newFrequency)
    fun withSeaLevelRise(newRise: Double) = copy(seaLevelRise_mm = newRise)

    fun getResourceAbundance(): Double {
        val droughtPenalty = abs(droughtIndex - 0.5) * 2
        val tempPenalty = abs(temperatureAnomaly) * 0.1
        return max(0.1, 1.0 - droughtPenalty * 0.3 - tempPenalty)
    }
}
