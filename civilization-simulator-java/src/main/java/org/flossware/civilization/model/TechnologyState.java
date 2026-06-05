package org.flossware.civilization.model;

import java.util.Map;
import java.util.Set;

/**
 * Immutable technology research state.
 */
public record TechnologyState(
    Set<String> unlockedTechs,
    Map<String, Double> researchProgress,
    double literacyRate,
    int universities
) {
    public TechnologyState {
        if (literacyRate < 0 || literacyRate > 1) {
            throw new IllegalArgumentException("Literacy rate must be in [0, 1]");
        }
        if (universities < 0) {
            throw new IllegalArgumentException("Universities cannot be negative");
        }
        unlockedTechs = Set.copyOf(unlockedTechs);
        researchProgress = Map.copyOf(researchProgress);
    }

    public TechnologyState withUnlockedTech(String techId) {
        var newUnlocked = new java.util.HashSet<>(unlockedTechs);
        newUnlocked.add(techId);
        return new TechnologyState(newUnlocked, researchProgress, literacyRate, universities);
    }

    public TechnologyState withResearchProgress(String techId, double progress) {
        var newProgress = new java.util.HashMap<>(researchProgress);
        newProgress.put(techId, progress);
        return new TechnologyState(unlockedTechs, newProgress, literacyRate, universities);
    }

    public TechnologyState withLiteracyRate(double newRate) {
        return new TechnologyState(unlockedTechs, researchProgress, newRate, universities);
    }
}
