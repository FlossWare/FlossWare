package org.flossware.civilization.model;

/**
 * Multi-dimensional climate state.
 */
public record ClimateState(
    double temperatureAnomaly,
    double droughtIndex,
    double stormFrequency,
    double seaLevelRise_mm
) {
    public ClimateState {
        if (temperatureAnomaly < -10 || temperatureAnomaly > 10) {
            throw new IllegalArgumentException("Temperature anomaly out of range [-10, 10]: " + temperatureAnomaly);
        }
        if (droughtIndex < 0 || droughtIndex > 1) {
            throw new IllegalArgumentException("Drought index must be in [0, 1]");
        }
        if (stormFrequency < 0) {
            throw new IllegalArgumentException("Storm frequency cannot be negative");
        }
    }

    public ClimateState withTemperatureAnomaly(double newAnomaly) {
        return new ClimateState(newAnomaly, droughtIndex, stormFrequency, seaLevelRise_mm);
    }

    public ClimateState withDroughtIndex(double newIndex) {
        return new ClimateState(temperatureAnomaly, newIndex, stormFrequency, seaLevelRise_mm);
    }

    public ClimateState withStormFrequency(double newFrequency) {
        return new ClimateState(temperatureAnomaly, droughtIndex, newFrequency, seaLevelRise_mm);
    }

    public ClimateState withSeaLevelRise(double newRise) {
        return new ClimateState(temperatureAnomaly, droughtIndex, stormFrequency, newRise);
    }

    public double getResourceAbundance() {
        double droughtPenalty = Math.abs(droughtIndex - 0.5) * 2;
        double tempPenalty = Math.abs(temperatureAnomaly) * 0.1;
        return Math.max(0.1, 1.0 - droughtPenalty * 0.3 - tempPenalty);
    }
}
