package org.flossware.civilization.engine;

import org.flossware.civilization.model.*;
import org.flossware.civilization.module.*;
import org.flossware.civilization.util.SeedManager;

import java.util.*;

/**
 * Main simulation engine with variable tick loop.
 *
 * Pure function: (scenario, seed) → (finalState, eventLog)
 * Execution order (strictly sequential):
 * 1. EnvironmentModule (Climate, resources, disasters)
 * 2. MigrationModule (Population movement - placeholder for now)
 * 3. PopulationModule (Births, deaths, plague)
 * 4. EconomyModule (Production, consumption, trade)
 * 5. TechnologyModule (Research, diffusion, unlocks)
 * 6. ReligionModule (Spread, conversion, schisms)
 * 7. PoliticsModule (Stability, succession, rebellion)
 * 8. MilitaryModule (War resolution, territorial changes)
 */
public final class SimulationEngine {

    private final Scenario scenario;
    private final SeedManager seedManager;
    private final TechGraph techGraph;

    public SimulationEngine(Scenario scenario, long baseSeed) {
        this.scenario = Objects.requireNonNull(scenario);
        this.seedManager = new SeedManager(baseSeed);
        this.techGraph = new TechGraph(scenario.techTree());
    }

    /**
     * Runs the complete simulation from start year to end year.
     *
     * @param runIndex Monte Carlo run index for seed isolation
     * @return Final state and complete event log
     */
    public SimulationResult run(int runIndex) {
        SplittableRandom runRandom = seedManager.getRunRandom(runIndex);
        CivilizationState state = scenario.initialState();
        List<Event> allEvents = new ArrayList<>();

        int currentYear = scenario.startYear();

        while (currentYear <= scenario.endYear()) {
            TickType tickType = TickType.determineTickType(
                scenario.worldConstraints().climateVolatility(),
                state.politics().stability()
            );

            SplittableRandom yearRandom = seedManager.getYearRandom(runRandom, currentYear, tickType.name());

            // Execute all modules in sequence
            TickResult tickResult = executeTick(state, currentYear, tickType, yearRandom);
            state = tickResult.state();

            // Add year to all events
            final int eventYear = currentYear;
            final String civId = state.id();
            List<Event> yearEvents = tickResult.events().stream()
                .map(e -> new Event(eventYear, civId, e.type(), e.severity(), e.description(), e.data()))
                .toList();
            allEvents.addAll(yearEvents);

            currentYear += (int) Math.ceil(tickType.getYears());
        }

        return new SimulationResult(state, allEvents);
    }

    /**
     * Executes one simulation tick with all modules.
     */
    private TickResult executeTick(
        CivilizationState state,
        int year,
        TickType tickType,
        SplittableRandom yearRandom
    ) {
        List<Event> events = new ArrayList<>();

        // 1. Climate Module
        var climateRandom = SeedManager.getModuleRandom(yearRandom, "climate");
        var climateResult = ClimateModule.tick(
            state.climate(),
            scenario.worldConstraints().climateVolatility(),
            climateRandom
        );
        state = state.withClimate(climateResult.state());
        events.addAll(climateResult.events());

        // 2. Migration Module (placeholder)
        // Would go here

        // 3. Population Module
        var popRandom = SeedManager.getModuleRandom(yearRandom, "population");
        var popResult = PopulationModule.tick(
            state.population(),
            state.climate().getResourceAbundance(),
            scenario.worldConstraints().plagueProbability(),
            popRandom
        );
        state = state.withPopulation(popResult.state());
        events.addAll(popResult.events());

        // 4. Economy Module
        var econRandom = SeedManager.getModuleRandom(yearRandom, "economy");
        var econResult = EconomyModule.tick(
            state.economy(),
            state.climate().getResourceAbundance(),
            state.technology().unlockedTechs(),
            state.population().population(),
            econRandom
        );
        state = state.withEconomy(econResult.state());
        events.addAll(econResult.events());

        // 5. Technology Module
        var techRandom = SeedManager.getModuleRandom(yearRandom, "technology");
        var techResult = TechnologyModule.tick(
            state.technology(),
            techGraph,
            calculateTradeConnectivity(state),
            state.population().population(),
            techRandom
        );
        state = state.withTechnology(techResult.state());
        events.addAll(techResult.events());

        // 6. Religion Module
        var relRandom = SeedManager.getModuleRandom(yearRandom, "religion");
        var relResult = ReligionModule.tick(
            state.religion(),
            calculateTradeConnectivity(state),
            relRandom
        );
        state = state.withReligion(relResult.state());
        events.addAll(relResult.events());

        // 7. Politics Module
        var polRandom = SeedManager.getModuleRandom(yearRandom, "politics");
        var polResult = PoliticsModule.tick(
            state.politics(),
            calculateEconomicHealth(state),
            state.religion().religiousUnity(),
            state.military().atWar(),
            tickType.getYears(),
            polRandom
        );
        state = state.withPolitics(polResult.state());
        events.addAll(polResult.events());

        // 8. Military Module
        var milRandom = SeedManager.getModuleRandom(yearRandom, "military");
        var milResult = MilitaryModule.tick(
            state.military(),
            state.economy().wealth(),
            state.technology().unlockedTechs(),
            milRandom
        );
        state = state.withMilitary(milResult.state());
        events.addAll(milResult.events());

        // Update year
        state = state.withYear(year);

        return new TickResult(state, events);
    }

    private double calculateTradeConnectivity(CivilizationState state) {
        // Simplified: based on number of trade routes
        return Math.min(1.0, state.economy().tradeRoutes().size() * 0.2);
    }

    private double calculateEconomicHealth(CivilizationState state) {
        // Simplified: based on GDP growth
        double wealthPerCapita = state.economy().wealth() / Math.max(1, state.population().population());
        return Math.min(1.0, wealthPerCapita / 1000.0);
    }

    private record TickResult(CivilizationState state, List<Event> events) {}
}
