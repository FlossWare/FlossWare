package org.flossware.civilization.engine;

/**
 * Variable time step modes for adaptive simulation.
 */
public enum TickType {
    MONTHLY(1.0 / 12.0, "Crisis mode - monthly steps"),
    YEARLY(1.0, "Default - yearly steps"),
    DECADE(10.0, "Low volatility - decade steps");

    private final double years;
    private final String description;

    TickType(double years, String description) {
        this.years = years;
        this.description = description;
    }

    public double getYears() {
        return years;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines tick type based on current volatility and stability.
     */
    public static TickType determineTickType(double volatility, double stability) {
        if (stability < 0.3) {
            return MONTHLY;
        }
        if (volatility < 0.1) {
            return DECADE;
        }
        return YEARLY;
    }
}
