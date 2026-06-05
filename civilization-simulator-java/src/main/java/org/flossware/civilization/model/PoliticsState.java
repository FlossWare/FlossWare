package org.flossware.civilization.model;

/**
 * Immutable political state snapshot.
 */
public record PoliticsState(
    double stability,
    String government,
    int rulerAge,
    double warExhaustion,
    boolean inRebellion,
    boolean inSuccessionCrisis
) {
    public PoliticsState {
        if (stability < 0 || stability > 1) {
            throw new IllegalArgumentException("Stability must be in [0, 1]");
        }
        if (rulerAge < 0) {
            throw new IllegalArgumentException("Ruler age cannot be negative");
        }
        if (warExhaustion < 0 || warExhaustion > 1) {
            throw new IllegalArgumentException("War exhaustion must be in [0, 1]");
        }
    }

    public PoliticsState withStability(double newStability) {
        double clamped = Math.max(0, Math.min(1, newStability));
        return new PoliticsState(clamped, government, rulerAge, warExhaustion, inRebellion, inSuccessionCrisis);
    }

    public PoliticsState withRebellion(boolean isInRebellion) {
        return new PoliticsState(stability, government, rulerAge, warExhaustion, isInRebellion, inSuccessionCrisis);
    }

    public PoliticsState withSuccessionCrisis(boolean isCrisis) {
        return new PoliticsState(stability, government, rulerAge, warExhaustion, inRebellion, isCrisis);
    }

    public PoliticsState withWarExhaustion(double newExhaustion) {
        double clamped = Math.max(0, Math.min(1, newExhaustion));
        return new PoliticsState(stability, government, rulerAge, clamped, inRebellion, inSuccessionCrisis);
    }

    public PoliticsState withRulerAge(int newAge) {
        return new PoliticsState(stability, government, newAge, warExhaustion, inRebellion, inSuccessionCrisis);
    }
}
