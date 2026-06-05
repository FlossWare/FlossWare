package org.flossware.civilization.module;

import org.flossware.civilization.model.ReligionState;
import org.flossware.civilization.model.Event;
import org.flossware.civilization.model.Event.EventSeverity;
import org.flossware.civilization.model.Event.EventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Religion and cultural dynamics module.
 *
 * Pure function: (state, tradeConnectivity, random) → (newState, events)
 */
public final class ReligionModule {

    private static final double SCHISM_THRESHOLD = 0.05;
    private static final double MINORITY_THRESHOLD = 0.2;
    private static final double UNITY_THRESHOLD = 0.6;

    /**
     * Ticks religion forward by one time step.
     *
     * @param current Current religion state
     * @param tradeConnectivity Trade connectivity factor (0.0 to 1.0+)
     * @param random Module-specific random generator
     * @return Updated state and events
     */
    public static ModuleResult<ReligionState> tick(
        ReligionState current,
        double tradeConnectivity,
        SplittableRandom random
    ) {
        List<Event> events = new ArrayList<>();
        int year = 0; // Will be set by engine

        // Spread religions based on spreadRate and trade
        Map<String, Double> newShares = spreadReligions(
            current.religionShares(),
            current.spreadRate(),
            tradeConnectivity,
            random
        );

        // Calculate unity (largest religion share)
        double unity = newShares.values().stream()
            .mapToDouble(d -> d)
            .max()
            .orElse(0.0);

        // Calculate stability bonus
        double stabilityBonus = unity * 0.2;

        // Check for schism
        boolean schismOccurred = checkForSchism(newShares, unity, random);
        if (schismOccurred) {
            events.add(new Event(
                year,
                "",
                EventType.RELIGIOUS_SCHISM,
                EventSeverity.MAJOR,
                "Religious schism splits the dominant faith!",
                null
            ));

            // Split the largest religion
            newShares = splitLargestReligion(newShares, random);

            // Recalculate unity after schism
            unity = newShares.values().stream()
                .mapToDouble(d -> d)
                .max()
                .orElse(0.0);

            // Recalculate stability bonus
            stabilityBonus = unity * 0.2;
        }

        ReligionState newState = new ReligionState(
            newShares,
            unity,
            stabilityBonus,
            current.spreadRate()
        );

        return new ModuleResult<>(newState, events);
    }

    /**
     * Spread religions based on spread rate and trade connectivity.
     * Religions with higher shares grow slightly, influenced by trade.
     */
    private static Map<String, Double> spreadReligions(
        Map<String, Double> currentShares,
        double spreadRate,
        double tradeConnectivity,
        SplittableRandom random
    ) {
        Map<String, Double> newShares = new HashMap<>(currentShares);

        // Trade increases religious exchange
        double effectiveSpreadRate = spreadRate * (1.0 + tradeConnectivity * 0.5);

        // Apply small random fluctuations to each religion
        for (Map.Entry<String, Double> entry : currentShares.entrySet()) {
            String religion = entry.getKey();
            double currentShare = entry.getValue();

            // Random fluctuation: -spreadRate to +spreadRate
            double change = (random.nextDouble() * 2.0 - 1.0) * effectiveSpreadRate;
            double newShare = Math.max(0.0, Math.min(1.0, currentShare + change));

            newShares.put(religion, newShare);
        }

        // Normalize shares to sum to 1.0
        return normalizeShares(newShares);
    }

    /**
     * Check if a religious schism occurs.
     * Conditions: minority religion > 20%, unity < 60%, and 5% random chance.
     */
    private static boolean checkForSchism(
        Map<String, Double> shares,
        double unity,
        SplittableRandom random
    ) {
        // Check if any minority religion exceeds threshold
        boolean hasSignificantMinority = shares.values().stream()
            .anyMatch(share -> share > MINORITY_THRESHOLD && share < unity);

        // Schism occurs if conditions are met and random check passes
        return hasSignificantMinority
            && unity < UNITY_THRESHOLD
            && random.nextDouble() < SCHISM_THRESHOLD;
    }

    /**
     * Split the largest religion into two factions.
     */
    private static Map<String, Double> splitLargestReligion(
        Map<String, Double> shares,
        SplittableRandom random
    ) {
        Map<String, Double> newShares = new HashMap<>(shares);

        // Find the largest religion
        String largestReligion = shares.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        if (largestReligion != null) {
            double originalShare = shares.get(largestReligion);

            // Split roughly 60/40 with some randomness
            double splitRatio = 0.6 + (random.nextDouble() * 0.2 - 0.1); // 0.5 to 0.7
            double mainShare = originalShare * splitRatio;
            double schismShare = originalShare * (1.0 - splitRatio);

            newShares.put(largestReligion, mainShare);
            newShares.put(largestReligion + " (Reformed)", schismShare);
        }

        return normalizeShares(newShares);
    }

    /**
     * Normalize religion shares to sum to 1.0.
     */
    private static Map<String, Double> normalizeShares(Map<String, Double> shares) {
        double total = shares.values().stream().mapToDouble(d -> d).sum();

        if (total <= 0) {
            return shares;
        }

        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : shares.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / total);
        }

        return normalized;
    }
}
