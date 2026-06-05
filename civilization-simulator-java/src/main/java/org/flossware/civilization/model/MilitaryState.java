package org.flossware.civilization.model;

/**
 * Immutable military state snapshot.
 */
public record MilitaryState(
    long armySize,
    long navySize,
    double techAdvantage,
    double logisticsScore,
    boolean atWar,
    String warOpponent
) {
    public MilitaryState {
        if (armySize < 0) {
            throw new IllegalArgumentException("Army size cannot be negative");
        }
        if (navySize < 0) {
            throw new IllegalArgumentException("Navy size cannot be negative");
        }
        if (logisticsScore < 0 || logisticsScore > 1) {
            throw new IllegalArgumentException("Logistics score must be in [0, 1]");
        }
    }

    public MilitaryState withArmySize(long newSize) {
        return new MilitaryState(newSize, navySize, techAdvantage, logisticsScore, atWar, warOpponent);
    }

    public MilitaryState withNavySize(long newSize) {
        return new MilitaryState(armySize, newSize, techAdvantage, logisticsScore, atWar, warOpponent);
    }

    public MilitaryState withWar(boolean isAtWar, String opponent) {
        return new MilitaryState(armySize, navySize, techAdvantage, logisticsScore, isAtWar, opponent);
    }

    public MilitaryState withTechAdvantage(double advantage) {
        return new MilitaryState(armySize, navySize, advantage, logisticsScore, atWar, warOpponent);
    }
}
