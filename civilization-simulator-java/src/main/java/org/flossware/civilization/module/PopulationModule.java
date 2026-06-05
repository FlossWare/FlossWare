package org.flossware.civilization.module;

import org.flossware.civilization.model.PopulationState;
import org.flossware.civilization.model.Event;
import org.flossware.civilization.model.Event.EventSeverity;
import org.flossware.civilization.model.Event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Population dynamics module with logistic growth and carrying capacity.
 *
 * Pure function: (state, params, random) → (newState, events)
 */
public final class PopulationModule {

    private static final double BASE_BIRTH_RATE = 0.03;
    private static final double BASE_DEATH_RATE = 0.02;
    private static final double PLAGUE_DEATH_MULTIPLIER = 3.0;

    /**
     * Ticks population forward by one time step.
     *
     * @param current Current population state
     * @param resourceAbundance From climate/environment (0.0 to 1.5+)
     * @param plagueProbability Probability of plague outbreak this tick
     * @param random Module-specific random generator
     * @return Updated state and events
     */
    public static ModuleResult<PopulationState> tick(
        PopulationState current,
        double resourceAbundance,
        double plagueProbability,
        SplittableRandom random
    ) {
        List<Event> events = new ArrayList<>();
        int year = 0; // Will be set by engine

        // Check for plague
        boolean plagueActive = current.plagueActive() || random.nextDouble() < plagueProbability;
        if (plagueActive && !current.plagueActive()) {
            events.add(new Event(year, "", EventType.PLAGUE, EventSeverity.CRITICAL,
                "Plague outbreak!", null));
        }

        // Plague ends after some time (20% chance per year)
        if (plagueActive && random.nextDouble() < 0.2) {
            plagueActive = false;
        }

        // Calculate carrying capacity
        double carryingCapacity = calculateCarryingCapacity(resourceAbundance);

        // Logistic growth
        double growthRate = logisticGrowth(current.population(), carryingCapacity);
        double births = current.population() * BASE_BIRTH_RATE * growthRate;

        // Deaths
        double mortalityRate = plagueActive ? BASE_DEATH_RATE * PLAGUE_DEATH_MULTIPLIER : BASE_DEATH_RATE;
        double deaths = current.population() * mortalityRate;

        // Apply delta
        long newPopulation = Math.max(1000, current.population() + (long)(births - deaths));

        // Population milestones
        if (newPopulation >= 10_000_000 && current.population() < 10_000_000) {
            events.add(new Event(year, "", EventType.POPULATION_MILESTONE, EventSeverity.MAJOR,
                "Population reaches 10 million!", newPopulation));
        }

        PopulationState newState = new PopulationState(
            newPopulation,
            BASE_BIRTH_RATE,
            mortalityRate,
            carryingCapacity,
            plagueActive
        );

        return new ModuleResult<>(newState, events);
    }

    /**
     * Logistic growth formula: r * (1 - P/K)
     * Returns growth rate multiplier between 0 and 1.
     */
    private static double logisticGrowth(long population, double carryingCapacity) {
        if (carryingCapacity <= 0) {
            return 0.0;
        }
        double ratio = population / carryingCapacity;
        return Math.max(0, 1.0 - ratio);
    }

    /**
     * Carrying capacity based on resource abundance.
     * Base capacity ~50M, scaled by resources.
     */
    private static double calculateCarryingCapacity(double resourceAbundance) {
        double baseCapacity = 50_000_000;
        return baseCapacity * Math.max(0.1, resourceAbundance);
    }
}
