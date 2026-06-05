package org.flossware.civilization.util;

import org.flossware.civilization.model.*;

import java.util.*;

/**
 * Builder for constructing scenarios programmatically.
 */
public final class ScenarioBuilder {
    private String scenarioId = "scenario-" + UUID.randomUUID();
    private String name = "Unnamed Scenario";
    private String description = "";
    private int startYear = -3000;
    private int endYear = 2026;

    private CivilizationState initialState;
    private final List<Technology> techTree = new ArrayList<>();
    private WorldConstraints worldConstraints;
    private SimulationRules simulationRules;

    public ScenarioBuilder withId(String id) {
        this.scenarioId = id;
        return this;
    }

    public ScenarioBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ScenarioBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ScenarioBuilder withTimeRange(int startYear, int endYear) {
        this.startYear = startYear;
        this.endYear = endYear;
        return this;
    }

    public ScenarioBuilder withInitialState(CivilizationState state) {
        this.initialState = state;
        return this;
    }

    public ScenarioBuilder addTechnology(Technology tech) {
        this.techTree.add(tech);
        return this;
    }

    public ScenarioBuilder withTechTree(List<Technology> techs) {
        this.techTree.clear();
        this.techTree.addAll(techs);
        return this;
    }

    public ScenarioBuilder withWorldConstraints(WorldConstraints constraints) {
        this.worldConstraints = constraints;
        return this;
    }

    public ScenarioBuilder withSimulationRules(SimulationRules rules) {
        this.simulationRules = rules;
        return this;
    }

    public Scenario build() {
        Objects.requireNonNull(initialState, "Initial state must be set");
        Objects.requireNonNull(worldConstraints, "World constraints must be set");
        Objects.requireNonNull(simulationRules, "Simulation rules must be set");

        return new Scenario(
            scenarioId,
            name,
            description,
            startYear,
            endYear,
            initialState,
            techTree,
            worldConstraints,
            simulationRules
        );
    }
}
