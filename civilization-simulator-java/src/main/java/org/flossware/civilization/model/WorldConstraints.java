package org.flossware.civilization.model;

/**
 * Global world parameters affecting all civilizations.
 */
public record WorldConstraints(
    double politicalStability,
    double warFrequency,
    double climateVolatility,
    double plagueProbability,
    double resourceAbundance
) {
    public WorldConstraints {
        validateRange(politicalStability, "politicalStability");
        validateRange(warFrequency, "warFrequency");
        validateRange(climateVolatility, "climateVolatility");
        validateRange(plagueProbability, "plagueProbability");
        validateRange(resourceAbundance, "resourceAbundance");
    }

    private void validateRange(double value, String name) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be in [0, 1]: " + value);
        }
    }
}
