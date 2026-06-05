package org.flossware.civilization.model;

import java.util.List;

/**
 * Immutable economy state snapshot.
 */
public record EconomyState(
    double wealth,
    double production,
    double consumption,
    long workers,
    double tradeSurplus,
    double gdp,
    List<TradeRoute> tradeRoutes
) {
    public EconomyState {
        if (wealth < 0) {
            throw new IllegalArgumentException("Wealth cannot be negative");
        }
        if (workers < 0) {
            throw new IllegalArgumentException("Workers cannot be negative");
        }
        tradeRoutes = List.copyOf(tradeRoutes);
    }

    public EconomyState withWealth(double newWealth) {
        return new EconomyState(
            Math.max(0, newWealth),
            production,
            consumption,
            workers,
            tradeSurplus,
            gdp,
            tradeRoutes
        );
    }

    public EconomyState withProduction(double newProduction) {
        return new EconomyState(wealth, newProduction, consumption, workers, tradeSurplus, gdp, tradeRoutes);
    }

    public EconomyState withGDP(double newGdp) {
        return new EconomyState(wealth, production, consumption, workers, tradeSurplus, newGdp, tradeRoutes);
    }

    public EconomyState withTradeRoutes(List<TradeRoute> newRoutes) {
        return new EconomyState(wealth, production, consumption, workers, tradeSurplus, gdp, newRoutes);
    }

    public EconomyState withWorkers(long newWorkers) {
        return new EconomyState(wealth, production, consumption, Math.max(0, newWorkers), tradeSurplus, gdp, tradeRoutes);
    }
}
