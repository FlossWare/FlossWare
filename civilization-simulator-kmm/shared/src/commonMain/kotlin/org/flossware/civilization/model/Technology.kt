package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class Technology(
    val id: String,
    val era: String,
    val prerequisites: List<String>,
    val researchCost: Double,
    val diffusionRate: Double
) {
    init {
        require(researchCost > 0) { "Research cost must be positive: $researchCost" }
        require(diffusionRate in 0.0..1.0) { "Diffusion rate must be in [0, 1]: $diffusionRate" }
    }
}
