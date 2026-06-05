package org.flossware.civilization.model;

import java.util.List;

/**
 * Represents a trade connection between civilizations.
 */
public record TradeRoute(
    String from,
    String to,
    List<String> goods,
    double volume,
    double tariff
) {
    public TradeRoute {
        if (volume < 0) {
            throw new IllegalArgumentException("Trade volume cannot be negative");
        }
        if (tariff < 0 || tariff > 1) {
            throw new IllegalArgumentException("Tariff must be in [0, 1]");
        }
        goods = List.copyOf(goods);
    }
}
