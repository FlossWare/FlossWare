package org.flossware.civilization.module;

import org.flossware.civilization.model.EconomyState;
import org.flossware.civilization.model.Event;
import org.flossware.civilization.model.Event.EventSeverity;
import org.flossware.civilization.model.Event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Economy module: production, consumption, trade, and wealth dynamics.
 *
 * Pure function: (state, params, random) → (newState, events)
 */
public final class EconomyModule {

    private static final double BASE_PRODUCTIVITY = 1.0;
    private static final double PER_CAPITA_CONSUMPTION = 0.8;
    private static final double AVERAGE_PRICE_LEVEL = 1.0;
    private static final double TRADE_SURPLUS_MODIFIER = 0.2;
    private static final double BOOM_THRESHOLD = 0.5;  // 50% increase
    private static final double COLLAPSE_THRESHOLD = -0.3;  // 30% decrease
    private static final double WORKFORCE_RATIO = 0.15;  // 15% of population as working-age adults

    /**
     * Ticks economy forward by one time step.
     *
     * @param current Current economy state
     * @param resourceAbundance From climate/environment (0.0 to 1.5+)
     * @param unlockedTechs Set of unlocked technology IDs
     * @param population Current population for workforce calculation
     * @param random Module-specific random generator
     * @return Updated state and events
     */
    public static ModuleResult<EconomyState> tick(
        EconomyState current,
        double resourceAbundance,
        Set<String> unlockedTechs,
        long population,
        SplittableRandom random
    ) {
        if (population < 0) {
            throw new IllegalArgumentException("Population cannot be negative: " + population);
        }

        List<Event> events = new ArrayList<>();
        int year = 0; // Will be set by engine

        // Calculate workforce as percentage of population
        // Minimum of 1 to ensure production even with tiny populations
        long workers = Math.max(1, (long)(population * WORKFORCE_RATIO));

        // Calculate productivity based on technology
        double techMultiplier = calculateTechMultiplier(unlockedTechs);
        double productivity = BASE_PRODUCTIVITY * techMultiplier;

        // Production: workers * productivity * resource abundance
        double production = workers * productivity * resourceAbundance;

        // Consumption: population * per capita consumption
        double consumption = population * PER_CAPITA_CONSUMPTION;

        // Trade surplus from trade routes
        double tradeSurplus = calculateTradeSurplus(current);

        // Wealth delta: (production - consumption) * (1 + tradeSurplus modifier)
        double wealthDelta = (production - consumption) * (1.0 + tradeSurplus * TRADE_SURPLUS_MODIFIER);

        // Calculate new wealth
        double newWealth = current.wealth() + wealthDelta;

        // Calculate GDP: production * average price level
        double gdp = production * AVERAGE_PRICE_LEVEL;

        // Detect economic events
        if (current.wealth() > 0) {
            double wealthChangeRatio = wealthDelta / current.wealth();

            if (wealthChangeRatio > BOOM_THRESHOLD) {
                events.add(new Event(year, "", EventType.ECONOMIC_BOOM, EventSeverity.MAJOR,
                    "Economic boom! Wealth increased by " + String.format("%.1f%%", wealthChangeRatio * 100),
                    wealthDelta));
            } else if (wealthChangeRatio < COLLAPSE_THRESHOLD) {
                events.add(new Event(year, "", EventType.ECONOMIC_COLLAPSE, EventSeverity.CRITICAL,
                    "Economic collapse! Wealth decreased by " + String.format("%.1f%%", Math.abs(wealthChangeRatio) * 100),
                    wealthDelta));
            }
        }

        // Create new state with updated values
        EconomyState newState = new EconomyState(
            Math.max(0, newWealth),
            production,
            consumption,
            workers,
            tradeSurplus,
            gdp,
            current.tradeRoutes()
        );

        return new ModuleResult<>(newState, events);
    }

    /**
     * Calculate technology multiplier based on unlocked technologies.
     * Key economic technologies provide bonuses.
     */
    private static double calculateTechMultiplier(Set<String> unlockedTechs) {
        double multiplier = 1.0;

        // Economic technology bonuses
        if (unlockedTechs.contains("agriculture")) {
            multiplier *= 1.2;
        }
        if (unlockedTechs.contains("irrigation")) {
            multiplier *= 1.15;
        }
        if (unlockedTechs.contains("bronze_working")) {
            multiplier *= 1.1;
        }
        if (unlockedTechs.contains("iron_working")) {
            multiplier *= 1.2;
        }
        if (unlockedTechs.contains("currency")) {
            multiplier *= 1.25;
        }
        if (unlockedTechs.contains("engineering")) {
            multiplier *= 1.15;
        }
        if (unlockedTechs.contains("banking")) {
            multiplier *= 1.3;
        }
        if (unlockedTechs.contains("machinery")) {
            multiplier *= 1.2;
        }
        if (unlockedTechs.contains("steam_power")) {
            multiplier *= 1.4;
        }
        if (unlockedTechs.contains("industrialization")) {
            multiplier *= 1.5;
        }

        return multiplier;
    }

    /**
     * Calculate trade surplus from active trade routes.
     * Simplified for now - returns average volume of trade routes adjusted for tariffs.
     */
    private static double calculateTradeSurplus(EconomyState state) {
        if (state.tradeRoutes().isEmpty()) {
            return 0.0;
        }

        double totalValue = state.tradeRoutes().stream()
            .mapToDouble(route -> route.volume() * (1.0 - route.tariff()))
            .sum();

        return totalValue / state.tradeRoutes().size();
    }
}
