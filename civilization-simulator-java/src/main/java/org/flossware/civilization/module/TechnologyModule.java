package org.flossware.civilization.module;

import org.flossware.civilization.model.*;
import org.flossware.civilization.model.Event.EventSeverity;
import org.flossware.civilization.model.Event.EventType;

import java.util.*;

/**
 * Technology research and diffusion module.
 * Pure function with explicit DAG-based unlocking.
 */
public final class TechnologyModule {

    private static final double BASE_RESEARCH_POINTS = 10.0;
    private static final double UNIVERSITY_BONUS = 0.5;

    public static ModuleResult<TechnologyState> tick(
        TechnologyState current,
        TechGraph techGraph,
        double tradeConnectivity,
        long population,
        SplittableRandom random
    ) {
        List<Event> events = new ArrayList<>();
        TechnologyState newState = current;

        // Calculate research points per year
        double researchPoints = calculateResearchPoints(current, population);

        // Find next researchable technology
        Optional<Technology> nextTech = findNextResearchableTech(current, techGraph);

        if (nextTech.isPresent()) {
            Technology tech = nextTech.get();
            double currentProgress = current.researchProgress().getOrDefault(tech.id(), 0.0);
            double newProgress = currentProgress + researchPoints;

            if (newProgress >= tech.researchCost()) {
                // Unlock technology
                newState = newState.withUnlockedTech(tech.id());
                events.add(new Event(0, "", EventType.TECHNOLOGY_UNLOCKED, EventSeverity.MAJOR,
                    "Unlocked: " + tech.id(), tech));

                // Clear progress
                Map<String, Double> updatedProgress = new HashMap<>(current.researchProgress());
                updatedProgress.remove(tech.id());
                newState = new TechnologyState(newState.unlockedTechs(), updatedProgress,
                    newState.literacyRate(), newState.universities());
            } else {
                // Update progress
                newState = newState.withResearchProgress(tech.id(), newProgress);
            }
        }

        // Technology diffusion from trade
        newState = applyTechDiffusion(newState, techGraph, tradeConnectivity, random, events);

        return new ModuleResult<>(newState, events);
    }

    /**
     * Research points = base * (1 + literacyRate) * (1 + universityBonus)
     */
    private static double calculateResearchPoints(TechnologyState state, long population) {
        double literacyMultiplier = 1.0 + state.literacyRate();
        double universityMultiplier = 1.0 + (state.universities() * UNIVERSITY_BONUS);
        double populationFactor = Math.min(1.0, population / 1_000_000.0);
        return BASE_RESEARCH_POINTS * literacyMultiplier * universityMultiplier * populationFactor;
    }

    /**
     * Find next technology that can be researched (all prerequisites met).
     */
    private static Optional<Technology> findNextResearchableTech(TechnologyState state, TechGraph graph) {
        return graph.getAllTechnologyIds().stream()
            .filter(id -> !state.unlockedTechs().contains(id))
            .filter(id -> isUnlocked(id, state.unlockedTechs(), graph))
            .map(graph::getTechnology)
            .min(Comparator.comparingDouble(Technology::researchCost));
    }

    /**
     * Check if all prerequisites are unlocked.
     */
    private static boolean isUnlocked(String techId, Set<String> unlocked, TechGraph graph) {
        return graph.getPrerequisites(techId).stream().allMatch(unlocked::contains);
    }

    /**
     * Apply technology diffusion based on trade connectivity.
     */
    private static TechnologyState applyTechDiffusion(
        TechnologyState state,
        TechGraph graph,
        double tradeConnectivity,
        SplittableRandom random,
        List<Event> events
    ) {
        // Simplified: Random chance to gain a tech from neighbors
        // In full implementation, would track neighbor civs
        return state;
    }
}
