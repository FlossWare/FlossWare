package org.flossware.civilization.util

import kotlin.random.Random

/**
 * Hierarchical seed management for reproducible simulations.
 * Uses Kotlin multiplatform Random instead of JVM SplittableRandom.
 *
 * Seed Hierarchy:
 * - Base Seed: User-provided root seed
 * - Run Seed: hash(baseSeed + runIndex) for Monte Carlo isolation
 * - Year Seed: derived via split (sequential deterministic derivation)
 * - Module Seed: derived via split for module isolation
 */
class SeedManager(val baseSeed: Long) {

    /**
     * Derives a run-specific random for Monte Carlo isolation.
     */
    fun getRunRandom(runIndex: Int): Random {
        val runSeed = hash(baseSeed, runIndex.toLong())
        return Random(runSeed.toInt())
    }

    /**
     * Derives a year-specific random generator.
     * Replicates the SplittableRandom.split() behavior deterministically.
     */
    fun getYearRandom(runRandom: Random, year: Int, tickType: String): Random {
        val yearSeed = runRandom.nextLong()
        return Random(yearSeed.toInt())
    }

    companion object {
        /**
         * Derives a module-specific random generator.
         */
        fun getModuleRandom(yearRandom: Random, moduleName: String): Random {
            val moduleSeed = yearRandom.nextLong()
            return Random(moduleSeed.toInt())
        }

        /**
         * Hash function for deterministic seed derivation.
         */
        fun hash(seed: Long, value: Long): Long {
            var h = seed
            h = h xor value
            h *= -7046029254386353131L
            h = h xor (h ushr 32)
            h *= -7046029254386353131L
            h = h xor (h ushr 32)
            return h
        }

        fun hash(seed: Long, value: String): Long {
            return hash(seed, value.hashCode().toLong())
        }
    }
}
