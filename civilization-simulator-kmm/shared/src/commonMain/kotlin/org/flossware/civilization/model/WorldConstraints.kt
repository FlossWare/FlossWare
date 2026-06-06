package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class WorldConstraints(
    val politicalStability: Double,
    val warFrequency: Double,
    val climateVolatility: Double,
    val plagueProbability: Double,
    val resourceAbundance: Double
) {
    init {
        require(politicalStability in 0.0..1.0) { "politicalStability must be in [0, 1]" }
        require(warFrequency in 0.0..1.0) { "warFrequency must be in [0, 1]" }
        require(climateVolatility in 0.0..1.0) { "climateVolatility must be in [0, 1]" }
        require(plagueProbability in 0.0..1.0) { "plagueProbability must be in [0, 1]" }
        require(resourceAbundance in 0.0..1.0) { "resourceAbundance must be in [0, 1]" }
    }
}
