package org.flossware.civilization.model;

import java.util.List;

/**
 * Complete scenario configuration.
 */
public record Scenario(
    String scenarioId,
    String name,
    String description,
    int startYear,
    int endYear,
    CivilizationState initialState,
    List<Technology> techTree,
    WorldConstraints worldConstraints,
    SimulationRules simulationRules
) {
    public Scenario {
        if (startYear >= endYear) {
            throw new IllegalArgumentException("Start year must be before end year");
        }
        techTree = List.copyOf(techTree);
    }
}
