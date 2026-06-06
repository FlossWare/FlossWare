package org.flossware.civilization

import org.flossware.civilization.engine.SimulationEngine
import org.flossware.civilization.scenarios.RomeEnduresScenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReproducibilityTest {

    @Test
    fun testSameSeedProducesSameResults() {
        val scenario = RomeEnduresScenario.create()
        val seed = 42L

        val engine1 = SimulationEngine(scenario, seed)
        val result1 = engine1.run(0)

        val engine2 = SimulationEngine(scenario, seed)
        val result2 = engine2.run(0)

        assertEquals(result1.finalState.population.population, result2.finalState.population.population,
            "Population must be identical")
        assertEquals(result1.finalState.economy.wealth, result2.finalState.economy.wealth,
            "Wealth must be identical")
        assertEquals(result1.finalState.technology.unlockedTechs, result2.finalState.technology.unlockedTechs,
            "Unlocked techs must be identical")
        assertEquals(result1.events.size, result2.events.size, "Event count must be identical")
    }

    @Test
    fun testDifferentSeedsProduceDifferentResults() {
        val scenario = RomeEnduresScenario.create()

        val engine1 = SimulationEngine(scenario, 42L)
        val result1 = engine1.run(0)

        val engine2 = SimulationEngine(scenario, 999L)
        val result2 = engine2.run(0)

        val isDifferent = result1.finalState.population.population != result2.finalState.population.population ||
            kotlin.math.abs(result1.finalState.economy.wealth - result2.finalState.economy.wealth) > 0.01

        assertTrue(isDifferent, "Different seeds should produce different outcomes")
    }

    @Test
    fun testMonteCarloRunsAreIsolated() {
        val scenario = RomeEnduresScenario.create()
        val seed = 12345L

        val engine = SimulationEngine(scenario, seed)
        val run0First = engine.run(0)
        val run1First = engine.run(1)
        val run0Second = engine.run(0)
        val run1Second = engine.run(1)

        assertEquals(run0First.finalState.population.population, run0Second.finalState.population.population,
            "Run 0 must be reproducible")
        assertEquals(run1First.finalState.population.population, run1Second.finalState.population.population,
            "Run 1 must be reproducible")
        assertNotEquals(run0First.finalState.population.population, run1First.finalState.population.population,
            "Different Monte Carlo runs should have different outcomes")
    }
}
