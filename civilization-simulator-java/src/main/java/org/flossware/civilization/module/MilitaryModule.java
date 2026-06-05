package org.flossware.civilization.module;

import org.flossware.civilization.model.MilitaryState;
import org.flossware.civilization.model.Event;
import org.flossware.civilization.model.Event.EventSeverity;
import org.flossware.civilization.model.Event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Military module for warfare, army/navy maintenance, and conflict resolution.
 *
 * Pure function: (state, params, random) → (newState, events)
 */
public final class MilitaryModule {

    private static final double WEALTH_PER_SOLDIER = 1000.0;
    private static final double WAR_PROBABILITY = 0.05; // 5% per year
    private static final double BASE_TECH_ADVANTAGE = 1.0;
    private static final double TECH_ADVANTAGE_PER_MILITARY_TECH = 0.1;

    // Military technologies that provide advantages
    private static final Set<String> MILITARY_TECHS = Set.of(
        "bronze_working",
        "iron_working",
        "horseback_riding",
        "archery",
        "siege_weapons",
        "gunpowder",
        "steel",
        "military_tactics",
        "naval_warfare",
        "fortification"
    );

    /**
     * Ticks military state forward by one time step.
     *
     * @param current Current military state
     * @param wealth Available wealth for military maintenance
     * @param unlockedTechs Set of unlocked technology IDs
     * @param random Module-specific random generator
     * @return Updated state and events
     */
    public static ModuleResult<MilitaryState> tick(
        MilitaryState current,
        double wealth,
        Set<String> unlockedTechs,
        SplittableRandom random
    ) {
        List<Event> events = new ArrayList<>();
        int year = 0; // Will be set by engine

        // Calculate tech advantage based on unlocked military technologies
        double techAdvantage = calculateTechAdvantage(unlockedTechs);

        // Calculate maximum sustainable forces based on wealth
        long maxSoldiers = (long) (wealth / WEALTH_PER_SOLDIER);
        long newArmySize = Math.min(current.armySize(), maxSoldiers);
        long newNavySize = Math.min(current.navySize(), maxSoldiers / 2); // Navy is more expensive

        // Apply gradual growth if we can afford more
        if (newArmySize < maxSoldiers) {
            long potentialGrowth = (long) (maxSoldiers * 0.05); // 5% growth per year
            newArmySize = Math.min(maxSoldiers, newArmySize + potentialGrowth);
        }

        // Update state with new forces and tech advantage
        MilitaryState newState = current
            .withArmySize(newArmySize)
            .withNavySize(newNavySize)
            .withTechAdvantage(techAdvantage);

        // War logic
        if (current.atWar()) {
            // Resolve war
            ConflictResult result = resolveWar(
                newArmySize,
                newArmySize / 2, // Simplified: enemy has half our strength
                techAdvantage,
                1.0, // terrain advantage (neutral)
                random
            );

            if (result.victory()) {
                // War ends in victory
                newState = newState.withWar(false, null);
                events.add(new Event(year, "", EventType.WAR_ENDED, EventSeverity.MAJOR,
                    "Victory over " + current.warOpponent() + "!", result));
            } else if (result.defeat()) {
                // War ends in defeat
                newState = newState.withWar(false, null);
                // Apply casualties
                newState = newState
                    .withArmySize((long) (newState.armySize() * 0.7))
                    .withNavySize((long) (newState.navySize() * 0.7));
                events.add(new Event(year, "", EventType.WAR_ENDED, EventSeverity.CRITICAL,
                    "Defeated by " + current.warOpponent() + "!", result));
            }
            // Otherwise, war continues
        } else {
            // Check for new war
            if (random.nextDouble() < WAR_PROBABILITY) {
                String opponent = "Rival Civilization " + (random.nextInt(10) + 1);
                newState = newState.withWar(true, opponent);
                events.add(new Event(year, "", EventType.WAR_DECLARED, EventSeverity.MAJOR,
                    "War declared against " + opponent + "!", opponent));
            }
        }

        return new ModuleResult<>(newState, events);
    }

    /**
     * Calculate technology advantage based on unlocked military technologies.
     *
     * @param unlockedTechs Set of unlocked technology IDs
     * @return Technology advantage multiplier (1.0 = no advantage)
     */
    private static double calculateTechAdvantage(Set<String> unlockedTechs) {
        long militaryTechCount = unlockedTechs.stream()
            .filter(MILITARY_TECHS::contains)
            .count();

        return BASE_TECH_ADVANTAGE + (militaryTechCount * TECH_ADVANTAGE_PER_MILITARY_TECH);
    }

    /**
     * Resolve war outcome based on military strength and advantages.
     *
     * @param attackerStrength Attacker's army size
     * @param defenderStrength Defender's army size
     * @param techAdvantage Technology advantage multiplier
     * @param terrainAdvantage Terrain advantage multiplier
     * @param random Random generator
     * @return Conflict result
     */
    private static ConflictResult resolveWar(
        long attackerStrength,
        long defenderStrength,
        double techAdvantage,
        double terrainAdvantage,
        SplittableRandom random
    ) {
        // Calculate effective strengths
        double attackerEffective = attackerStrength * techAdvantage;
        double defenderEffective = defenderStrength * terrainAdvantage;

        // Add randomness (±20%)
        double randomFactor = 0.8 + (random.nextDouble() * 0.4);
        attackerEffective *= randomFactor;

        // Calculate casualties
        double attackerCasualties = defenderEffective * 0.1;
        double defenderCasualties = attackerEffective * 0.1;

        // Determine outcome based on strength ratio
        double ratio = attackerEffective / Math.max(1, defenderEffective);

        boolean victory = ratio > 1.5; // Decisive victory requires 1.5x advantage
        boolean defeat = ratio < 0.67; // Defeat if less than 0.67x enemy strength

        return new ConflictResult(
            victory,
            defeat,
            (long) attackerCasualties,
            (long) defenderCasualties,
            ratio
        );
    }
}

/**
 * Result of a military conflict.
 *
 * @param victory True if attacker won decisively
 * @param defeat True if attacker was defeated
 * @param attackerCasualties Number of attacker casualties
 * @param defenderCasualties Number of defender casualties
 * @param strengthRatio Effective strength ratio (attacker/defender)
 */
record ConflictResult(
    boolean victory,
    boolean defeat,
    long attackerCasualties,
    long defenderCasualties,
    double strengthRatio
) {
}
