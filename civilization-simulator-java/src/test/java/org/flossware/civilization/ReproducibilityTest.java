package org.flossware.civilization;

import org.flossware.civilization.engine.SimulationEngine;
import org.flossware.civilization.engine.SimulationResult;
import org.flossware.civilization.model.Scenario;
import org.flossware.civilization.scenarios.RomeEnuresScenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that simulations are reproducible with the same seed.
 * This is a critical guarantee of the system.
 */
class ReproducibilityTest {

    @Test
    void testSameSeedProducesSameResults() {
        Scenario scenario = RomeEnuresScenario.create();
        long seed = 42L;

        SimulationEngine engine1 = new SimulationEngine(scenario, seed);
        SimulationResult result1 = engine1.run(0);

        SimulationEngine engine2 = new SimulationEngine(scenario, seed);
        SimulationResult result2 = engine2.run(0);

        // Final states must be identical
        assertEquals(result1.finalState().population().population(),
                    result2.finalState().population().population(),
                    "Population must be identical");

        assertEquals(result1.finalState().economy().wealth(),
                    result2.finalState().economy().wealth(),
                    0.01,
                    "Wealth must be identical");

        assertEquals(result1.finalState().technology().unlockedTechs(),
                    result2.finalState().technology().unlockedTechs(),
                    "Unlocked techs must be identical");

        assertEquals(result1.events().size(),
                    result2.events().size(),
                    "Event count must be identical");
    }

    @Test
    void testDifferentSeedsProduceDifferentResults() {
        Scenario scenario = RomeEnuresScenario.create();

        SimulationEngine engine1 = new SimulationEngine(scenario, 42L);
        SimulationResult result1 = engine1.run(0);

        SimulationEngine engine2 = new SimulationEngine(scenario, 999L);
        SimulationResult result2 = engine2.run(0);

        // Results should differ (stochastic elements)
        // Note: There's a tiny chance they could be identical by coincidence,
        // but extremely unlikely
        boolean isDifferent = result1.finalState().population().population() !=
                             result2.finalState().population().population() ||
                             Math.abs(result1.finalState().economy().wealth() -
                                     result2.finalState().economy().wealth()) > 0.01;

        assertTrue(isDifferent,
                  "Different seeds should produce different outcomes (stochastic variation)");
    }

    @Test
    void testMonteCarloRunsAreIsolated() {
        Scenario scenario = RomeEnuresScenario.create();
        long seed = 12345L;

        SimulationEngine engine = new SimulationEngine(scenario, seed);

        SimulationResult run0_first = engine.run(0);
        SimulationResult run1_first = engine.run(1);

        // Re-run the same indices
        SimulationResult run0_second = engine.run(0);
        SimulationResult run1_second = engine.run(1);

        // Run 0 should be identical both times
        assertEquals(run0_first.finalState().population().population(),
                    run0_second.finalState().population().population(),
                    "Run 0 must be reproducible");

        // Run 1 should be identical both times
        assertEquals(run1_first.finalState().population().population(),
                    run1_second.finalState().population().population(),
                    "Run 1 must be reproducible");

        // But run 0 and run 1 should differ (different run seeds)
        assertNotEquals(run0_first.finalState().population().population(),
                       run1_first.finalState().population().population(),
                       "Different Monte Carlo runs should have different outcomes");
    }
}
