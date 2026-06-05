package org.flossware.civilization.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable religion and culture state.
 */
public record ReligionState(
    Map<String, Double> religionShares,
    double religiousUnity,
    double stabilityBonus,
    double spreadRate
) {
    public ReligionState {
        if (religiousUnity < 0 || religiousUnity > 1) {
            throw new IllegalArgumentException("Religious unity must be in [0, 1]");
        }
        if (stabilityBonus < 0 || stabilityBonus > 1) {
            throw new IllegalArgumentException("Stability bonus must be in [0, 1]");
        }
        religionShares = Map.copyOf(religionShares);
    }

    public ReligionState withReligionShares(Map<String, Double> newShares) {
        double totalShare = newShares.values().stream().mapToDouble(d -> d).sum();
        double newUnity = newShares.values().stream().mapToDouble(d -> d).max().orElse(0.0);
        double newBonus = newUnity * 0.2;
        return new ReligionState(newShares, newUnity, newBonus, spreadRate);
    }

    public ReligionState withSpreadRate(double newRate) {
        return new ReligionState(religionShares, religiousUnity, stabilityBonus, newRate);
    }
}
