package org.flossware.civilization.model;

/**
 * Immutable population state snapshot.
 */
public record PopulationState(
    long population,
    double birthRate,
    double deathRate,
    double carryingCapacity,
    boolean plagueActive
) {
    public PopulationState {
        if (population < 0) {
            throw new IllegalArgumentException("Population cannot be negative");
        }
        if (carryingCapacity < 0) {
            throw new IllegalArgumentException("Carrying capacity cannot be negative");
        }
    }

    public PopulationState withDelta(double delta) {
        long newPopulation = Math.max(0, population + (long)delta);
        return new PopulationState(
            newPopulation,
            birthRate,
            deathRate,
            carryingCapacity,
            plagueActive
        );
    }

    public PopulationState withPopulation(long newPopulation) {
        return new PopulationState(newPopulation, birthRate, deathRate, carryingCapacity, plagueActive);
    }

    public PopulationState withCarryingCapacity(double newCapacity) {
        return new PopulationState(population, birthRate, deathRate, newCapacity, plagueActive);
    }

    public PopulationState withPlague(boolean isActive) {
        return new PopulationState(population, birthRate, deathRate, carryingCapacity, isActive);
    }
}
