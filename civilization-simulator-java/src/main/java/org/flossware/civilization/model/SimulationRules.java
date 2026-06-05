package org.flossware.civilization.model;

/**
 * Simulation execution rules.
 */
public record SimulationRules(
    String timeStep,
    boolean deterministicReproducible,
    long baseRandomSeed,
    int monteCarloRuns,
    int parallelThreads
) {
    public SimulationRules {
        if (monteCarloRuns < 1) {
            throw new IllegalArgumentException("Monte Carlo runs must be at least 1");
        }
        if (parallelThreads < 1) {
            throw new IllegalArgumentException("Parallel threads must be at least 1");
        }
    }
}
