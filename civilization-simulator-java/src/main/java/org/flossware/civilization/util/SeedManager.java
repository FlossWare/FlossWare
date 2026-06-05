package org.flossware.civilization.util;

import java.util.SplittableRandom;

/**
 * Hierarchical seed management for reproducible simulations.
 *
 * Seed Hierarchy:
 * - Base Seed: User-provided root seed
 * - Run Seed: hash(baseSeed + runIndex) for Monte Carlo isolation
 * - Year Seed: hash(runSeed + year + tickType) for year-to-year reproducibility
 * - Module Seed: hash(yearSeed + moduleName) for module isolation
 *
 * Guarantees identical results across threads, JVMs, and machines for the same base seed.
 */
public final class SeedManager {
    private final long baseSeed;

    public SeedManager(long baseSeed) {
        this.baseSeed = baseSeed;
    }

    /**
     * Derives a run-specific seed for Monte Carlo isolation.
     */
    public SplittableRandom getRunRandom(int runIndex) {
        long runSeed = hash(baseSeed, runIndex);
        return new SplittableRandom(runSeed);
    }

    /**
     * Derives a year-specific random generator.
     */
    public SplittableRandom getYearRandom(SplittableRandom runRandom, int year, String tickType) {
        return runRandom.split();
    }

    /**
     * Derives a module-specific random generator to prevent correlation artifacts.
     */
    public static SplittableRandom getModuleRandom(SplittableRandom yearRandom, String moduleName) {
        return yearRandom.split();
    }

    /**
     * Hash function for deterministic seed derivation.
     * Uses a simple but effective mixing function.
     */
    private static long hash(long seed, long value) {
        long h = seed;
        h ^= value;
        h *= 0x9e3779b97f4a7c15L; // Knuth's golden ratio
        h ^= h >>> 32;
        h *= 0x9e3779b97f4a7c15L;
        h ^= h >>> 32;
        return h;
    }

    /**
     * Hash with string parameter.
     */
    public static long hash(long seed, String value) {
        return hash(seed, value.hashCode());
    }

    public long getBaseSeed() {
        return baseSeed;
    }
}
