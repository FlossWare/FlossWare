package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class TechnologyState(
    val unlockedTechs: Set<String>,
    val researchProgress: Map<String, Double>,
    val literacyRate: Double,
    val universities: Int
) {
    init {
        require(literacyRate in 0.0..1.0) { "Literacy rate must be in [0, 1]" }
        require(universities >= 0) { "Universities cannot be negative" }
    }

    fun withUnlockedTech(techId: String): TechnologyState {
        val newUnlocked = unlockedTechs + techId
        return copy(unlockedTechs = newUnlocked)
    }

    fun withResearchProgress(techId: String, progress: Double): TechnologyState {
        val newProgress = researchProgress + (techId to progress)
        return copy(researchProgress = newProgress)
    }

    fun withLiteracyRate(newRate: Double) = copy(literacyRate = newRate)
}
