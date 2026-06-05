package org.flossware.civilization.model;

import java.util.List;

/**
 * Complete state of a civilization at a point in time.
 * Aggregates all module states into a single immutable snapshot.
 */
public record CivilizationState(
    String id,
    String name,
    List<String> coreRegions,
    String capital,
    int year,
    PopulationState population,
    EconomyState economy,
    TechnologyState technology,
    PoliticsState politics,
    MilitaryState military,
    ClimateState climate,
    ReligionState religion
) {
    public CivilizationState {
        coreRegions = List.copyOf(coreRegions);
    }

    public CivilizationState withYear(int newYear) {
        return new CivilizationState(id, name, coreRegions, capital, newYear,
            population, economy, technology, politics, military, climate, religion);
    }

    public CivilizationState withPopulation(PopulationState newPopulation) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            newPopulation, economy, technology, politics, military, climate, religion);
    }

    public CivilizationState withEconomy(EconomyState newEconomy) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            population, newEconomy, technology, politics, military, climate, religion);
    }

    public CivilizationState withTechnology(TechnologyState newTechnology) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            population, economy, newTechnology, politics, military, climate, religion);
    }

    public CivilizationState withPolitics(PoliticsState newPolitics) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            population, economy, technology, newPolitics, military, climate, religion);
    }

    public CivilizationState withMilitary(MilitaryState newMilitary) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            population, economy, technology, politics, newMilitary, climate, religion);
    }

    public CivilizationState withClimate(ClimateState newClimate) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            population, economy, technology, politics, military, newClimate, religion);
    }

    public CivilizationState withReligion(ReligionState newReligion) {
        return new CivilizationState(id, name, coreRegions, capital, year,
            population, economy, technology, politics, military, climate, newReligion);
    }
}
