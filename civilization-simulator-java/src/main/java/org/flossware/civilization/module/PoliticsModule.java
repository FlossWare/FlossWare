package org.flossware.civilization.module;

import org.flossware.civilization.model.PoliticsState;
import org.flossware.civilization.model.Event;
import org.flossware.civilization.model.Event.EventSeverity;
import org.flossware.civilization.model.Event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Politics module handling stability, rebellions, and succession crises.
 *
 * Pure function: (state, params, random) → (newState, events)
 */
public final class PoliticsModule {

    private static final double REBELLION_THRESHOLD = 0.2;
    private static final double REBELLION_PROBABILITY = 0.3;
    private static final int SUCCESSION_AGE_THRESHOLD = 70;
    private static final double SUCCESSION_PROBABILITY = 0.2;
    private static final int RULER_AGE_INCREMENT = 1;

    /**
     * Ticks politics forward by one time step.
     *
     * @param current Current politics state
     * @param economicHealth Economic health factor (typically 0.0 to 1.0)
     * @param religiousUnity Religious unity factor (typically 0.0 to 1.0)
     * @param atWar Whether civilization is currently at war
     * @param yearsPerTick Duration of this tick in years (for adaptive time steps)
     * @param random Module-specific random generator
     * @return Updated state and events
     */
    public static ModuleResult<PoliticsState> tick(
        PoliticsState current,
        double economicHealth,
        double religiousUnity,
        boolean atWar,
        double yearsPerTick,
        SplittableRandom random
    ) {
        List<Event> events = new ArrayList<>();
        int year = 0; // Will be set by engine

        // Calculate base stability from current state
        double baseStability = current.stability();

        // Accumulate war exhaustion if at war, decay if at peace
        // Rates are per-year, scaled by tick duration
        double warExhaustion = current.warExhaustion();
        if (atWar) {
            warExhaustion = Math.min(1.0, warExhaustion + 0.05 * yearsPerTick);
        } else {
            warExhaustion = Math.max(0.0, warExhaustion - 0.1 * yearsPerTick);
        }

        // Calculate volatility component (random factor)
        double volatility = current.inRebellion() ? 0.3 : 0.1;

        // Calculate new stability
        // stability = baseStability + economicHealth*0.4 + religiousUnity*0.3 - warExhaustion*0.3 - random*volatility
        double newStability = baseStability
            + (economicHealth * 0.4)
            + (religiousUnity * 0.3)
            - (warExhaustion * 0.3)
            - (random.nextDouble() * volatility);

        // Clamp stability to [0, 1]
        newStability = Math.max(0.0, Math.min(1.0, newStability));

        // Age the ruler by tick duration
        int newRulerAge = current.rulerAge() + (int)Math.ceil(yearsPerTick);

        // Check for rebellion
        boolean rebellion = current.inRebellion();
        if (newStability < REBELLION_THRESHOLD && random.nextDouble() < REBELLION_PROBABILITY) {
            if (!rebellion) {
                rebellion = true;
                events.add(new Event(year, "", EventType.REBELLION, EventSeverity.CRITICAL,
                    "Popular rebellion breaks out due to low stability!", newStability));
            }
        } else if (rebellion && newStability > 0.5) {
            // Rebellion ends if stability recovers
            rebellion = false;
        }

        // Check for succession crisis
        boolean successionCrisis = current.inSuccessionCrisis();
        if (newRulerAge > SUCCESSION_AGE_THRESHOLD && random.nextDouble() < SUCCESSION_PROBABILITY) {
            if (!successionCrisis) {
                successionCrisis = true;
                events.add(new Event(year, "", EventType.SUCCESSION_CRISIS, EventSeverity.MAJOR,
                    "Succession crisis emerges as the ruler ages!", newRulerAge));
            }
        }

        // Build new state
        PoliticsState newState = new PoliticsState(
            newStability,
            current.government(),
            newRulerAge,
            warExhaustion,
            rebellion,
            successionCrisis
        );

        return new ModuleResult<>(newState, events);
    }
}
