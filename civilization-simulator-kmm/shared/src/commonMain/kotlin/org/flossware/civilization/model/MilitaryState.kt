package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class MilitaryState(
    val armySize: Long,
    val navySize: Long,
    val techAdvantage: Double,
    val logisticsScore: Double,
    val atWar: Boolean,
    val warOpponent: String?
) {
    init {
        require(armySize >= 0) { "Army size cannot be negative" }
        require(navySize >= 0) { "Navy size cannot be negative" }
        require(logisticsScore in 0.0..1.0) { "Logistics score must be in [0, 1]" }
    }

    fun withArmySize(newSize: Long) = copy(armySize = newSize)
    fun withNavySize(newSize: Long) = copy(navySize = newSize)
    fun withWar(isAtWar: Boolean, opponent: String?) = copy(atWar = isAtWar, warOpponent = opponent)
    fun withTechAdvantage(advantage: Double) = copy(techAdvantage = advantage)
}
