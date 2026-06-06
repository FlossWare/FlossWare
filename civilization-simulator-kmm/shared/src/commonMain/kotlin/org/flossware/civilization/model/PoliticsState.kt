package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class PoliticsState(
    val stability: Double,
    val government: String,
    val rulerAge: Int,
    val warExhaustion: Double,
    val inRebellion: Boolean,
    val inSuccessionCrisis: Boolean
) {
    init {
        require(stability in 0.0..1.0) { "Stability must be in [0, 1]" }
        require(rulerAge >= 0) { "Ruler age cannot be negative" }
        require(warExhaustion in 0.0..1.0) { "War exhaustion must be in [0, 1]" }
    }

    fun withStability(newStability: Double) = copy(stability = newStability.coerceIn(0.0, 1.0))
    fun withRebellion(isInRebellion: Boolean) = copy(inRebellion = isInRebellion)
    fun withSuccessionCrisis(isCrisis: Boolean) = copy(inSuccessionCrisis = isCrisis)
    fun withWarExhaustion(newExhaustion: Double) = copy(warExhaustion = newExhaustion.coerceIn(0.0, 1.0))
    fun withRulerAge(newAge: Int) = copy(rulerAge = newAge)
}
