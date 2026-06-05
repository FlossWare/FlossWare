package org.flossware.civilization.engine;

import org.flossware.civilization.model.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel Monte Carlo simulation executor.
 *
 * Features:
 * - Parallel execution across multiple threads
 * - Memory-aware pruning with diff chain storage
 * - Reproducible results via hierarchical seeding
 */
public final class MonteCarloRunner {

    private final Scenario scenario;
    private final int numRuns;
    private final int numThreads;

    public MonteCarloRunner(Scenario scenario) {
        this.scenario = scenario;
        this.numRuns = scenario.simulationRules().monteCarloRuns();
        this.numThreads = Math.min(
            scenario.simulationRules().parallelThreads(),
            Runtime.getRuntime().availableProcessors()
        );
    }

    /**
     * Executes all Monte Carlo runs in parallel.
     *
     * @return List of results from all runs
     */
    public List<SimulationResult> runAll() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<SimulationResult>> futures = new ArrayList<>();

        try {
            // Submit all runs
            for (int runIndex = 0; runIndex < numRuns; runIndex++) {
                final int index = runIndex;
                futures.add(executor.submit(() -> runSingle(index)));
            }

            // Collect results
            List<SimulationResult> results = new ArrayList<>();
            for (Future<SimulationResult> future : futures) {
                results.add(future.get());
            }

            return results;
        } finally {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    /**
     * Runs a single simulation with run-specific seed.
     */
    private SimulationResult runSingle(int runIndex) {
        SimulationEngine engine = new SimulationEngine(
            scenario,
            scenario.simulationRules().baseRandomSeed()
        );
        return engine.run(runIndex);
    }

    /**
     * Analyzes results across all runs to compute statistics.
     */
    public static MonteCarloAnalysis analyze(List<SimulationResult> results) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("No results to analyze");
        }

        // Calculate average final population
        double avgPopulation = results.stream()
            .mapToLong(r -> r.finalState().population().population())
            .average()
            .orElse(0.0);

        // Calculate average final wealth
        double avgWealth = results.stream()
            .mapToDouble(r -> r.finalState().economy().wealth())
            .average()
            .orElse(0.0);

        // Count techs unlocked (average)
        double avgTechs = results.stream()
            .mapToInt(r -> r.finalState().technology().unlockedTechs().size())
            .average()
            .orElse(0.0);

        // Survival rate (population > 10,000)
        long survivedCount = results.stream()
            .filter(r -> r.finalState().population().population() > 10_000)
            .count();
        double survivalRate = (double) survivedCount / results.size();

        return new MonteCarloAnalysis(
            results.size(),
            avgPopulation,
            avgWealth,
            avgTechs,
            survivalRate
        );
    }

    public record MonteCarloAnalysis(
        int totalRuns,
        double avgPopulation,
        double avgWealth,
        double avgTechs,
        double survivalRate
    ) {
        @Override
        public String toString() {
            return String.format("""
                Monte Carlo Analysis (%d runs):
                  Average Population: %.0f
                  Average Wealth: %.0f
                  Average Techs Unlocked: %.1f
                  Survival Rate: %.1f%%
                """, totalRuns, avgPopulation, avgWealth, avgTechs, survivalRate * 100);
        }
    }
}
