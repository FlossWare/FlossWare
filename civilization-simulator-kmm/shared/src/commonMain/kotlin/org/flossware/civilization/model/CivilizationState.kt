package org.flossware.civilization.model

import kotlinx.serialization.Serializable

@Serializable
data class CivilizationState(
    val id: String,
    val name: String,
    val coreRegions: List<String>,
    val capital: String,
    val year: Int,
    val population: PopulationState,
    val economy: EconomyState,
    val technology: TechnologyState,
    val politics: PoliticsState,
    val military: MilitaryState,
    val climate: ClimateState,
    val religion: ReligionState
) {
    fun withYear(newYear: Int) = copy(year = newYear)
    fun withPopulation(newPopulation: PopulationState) = copy(population = newPopulation)
    fun withEconomy(newEconomy: EconomyState) = copy(economy = newEconomy)
    fun withTechnology(newTechnology: TechnologyState) = copy(technology = newTechnology)
    fun withPolitics(newPolitics: PoliticsState) = copy(politics = newPolitics)
    fun withMilitary(newMilitary: MilitaryState) = copy(military = newMilitary)
    fun withClimate(newClimate: ClimateState) = copy(climate = newClimate)
    fun withReligion(newReligion: ReligionState) = copy(religion = newReligion)
}
