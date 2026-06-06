package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class ConflictResult(
    val victory: Boolean,
    val defeat: Boolean,
    val attackerCasualties: Long,
    val defenderCasualties: Long,
    val strengthRatio: Double
)

object MilitaryModule {
    private const val WEALTH_PER_SOLDIER = 1000.0
    private const val WAR_PROBABILITY = 0.05
    private const val BASE_TECH_ADVANTAGE = 1.0
    private const val TECH_ADVANTAGE_PER_MILITARY_TECH = 0.1

    private val MILITARY_TECHS = setOf(
        "bronze_working", "iron_working", "horseback_riding", "archery",
        "siege_weapons", "gunpowder", "steel", "military_tactics",
        "naval_warfare", "fortification"
    )

    fun tick(
        current: MilitaryState,
        wealth: Double,
        unlockedTechs: Set<String>,
        random: Random
    ): ModuleResult<MilitaryState> {
        val events = mutableListOf<Event>()

        val techAdvantage = calculateTechAdvantage(unlockedTechs)
        val maxSoldiers = (wealth / WEALTH_PER_SOLDIER).toLong()
        var newArmySize = min(current.armySize, maxSoldiers)
        var newNavySize = min(current.navySize, maxSoldiers / 2)

        if (newArmySize < maxSoldiers) {
            val potentialGrowth = (maxSoldiers * 0.05).toLong()
            newArmySize = min(maxSoldiers, newArmySize + potentialGrowth)
        }

        var newState = current
            .withArmySize(newArmySize)
            .withNavySize(newNavySize)
            .withTechAdvantage(techAdvantage)

        if (current.atWar) {
            val result = resolveWar(newArmySize, newArmySize / 2, techAdvantage, 1.0, random)
            if (result.victory) {
                newState = newState.withWar(false, null)
                events.add(Event(0, "", EventType.WAR_ENDED, EventSeverity.MAJOR,
                    "Victory over ${current.warOpponent}!", null))
            } else if (result.defeat) {
                newState = newState.withWar(false, null)
                    .withArmySize((newState.armySize * 0.7).toLong())
                    .withNavySize((newState.navySize * 0.7).toLong())
                events.add(Event(0, "", EventType.WAR_ENDED, EventSeverity.CRITICAL,
                    "Defeated by ${current.warOpponent}!", null))
            }
        } else {
            if (random.nextDouble() < WAR_PROBABILITY) {
                val opponent = "Rival Civilization ${random.nextInt(10) + 1}"
                newState = newState.withWar(true, opponent)
                events.add(Event(0, "", EventType.WAR_DECLARED, EventSeverity.MAJOR,
                    "War declared against $opponent!", opponent))
            }
        }

        return ModuleResult(newState, events)
    }

    private fun calculateTechAdvantage(unlockedTechs: Set<String>): Double {
        val militaryTechCount = unlockedTechs.count { it in MILITARY_TECHS }
        return BASE_TECH_ADVANTAGE + (militaryTechCount * TECH_ADVANTAGE_PER_MILITARY_TECH)
    }

    private fun resolveWar(
        attackerStrength: Long,
        defenderStrength: Long,
        techAdvantage: Double,
        terrainAdvantage: Double,
        random: Random
    ): ConflictResult {
        var attackerEffective = attackerStrength * techAdvantage
        val defenderEffective = defenderStrength * terrainAdvantage
        val randomFactor = 0.8 + (random.nextDouble() * 0.4)
        attackerEffective *= randomFactor
        val attackerCasualties = (defenderEffective * 0.1).toLong()
        val defenderCasualties = (attackerEffective * 0.1).toLong()
        val ratio = attackerEffective / max(1.0, defenderEffective)
        return ConflictResult(ratio > 1.5, ratio < 0.67, attackerCasualties, defenderCasualties, ratio)
    }
}
