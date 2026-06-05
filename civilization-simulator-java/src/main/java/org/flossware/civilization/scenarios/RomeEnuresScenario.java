package org.flossware.civilization.scenarios;

import org.flossware.civilization.model.*;
import org.flossware.civilization.util.ScenarioBuilder;

import java.util.*;

/**
 * "Rome Endures to Modern Era" - What if the Western Roman Empire survived?
 *
 * This scenario injects modern technology into a classical Roman starting point
 * and simulates the path from -27 BCE to 2026 CE.
 */
public final class RomeEnuresScenario {

    /**
     * Creates the complete Rome Endures scenario.
     */
    public static Scenario create() {
        return new ScenarioBuilder()
            .withId("rome-endures-2026")
            .withName("Rome Survives to Modern Era")
            .withDescription("What if the Western Roman Empire endured through history?")
            .withTimeRange(-27, 2026)
            .withInitialState(createInitialRomanState())
            .withTechTree(createTechTree())
            .withWorldConstraints(createWorldConstraints())
            .withSimulationRules(createSimulationRules())
            .build();
    }

    private static CivilizationState createInitialRomanState() {
        PopulationState population = new PopulationState(
            5_000_000,  // Initial population
            0.03,       // Birth rate
            0.02,       // Death rate
            10_000_000, // Carrying capacity
            false       // No plague initially
        );

        List<TradeRoute> tradeRoutes = List.of(
            new TradeRoute("Rome", "Egypt", List.of("grain", "papyrus"), 1000.0, 0.1),
            new TradeRoute("Rome", "Gaul", List.of("wine", "pottery"), 500.0, 0.15)
        );

        EconomyState economy = new EconomyState(
            50_000_000, // Initial wealth
            100_000,    // Production
            80_000,     // Consumption
            500_000,    // Workers (NOTE: Overwritten by EconomyModule as 15% of population on first tick)
            0.1,        // Trade surplus
            100_000,    // GDP
            tradeRoutes
        );

        Set<String> initialTechs = new HashSet<>(Arrays.asList(
            "agriculture", "mining", "ironWorking", "metallurgy_advanced"
        ));

        TechnologyState technology = new TechnologyState(
            initialTechs,
            new HashMap<>(),
            0.15,       // 15% literacy rate
            3           // 3 universities (Alexandria, Athens, Rome)
        );

        PoliticsState politics = new PoliticsState(
            0.6,        // Stability
            "Empire",   // Government type
            35,         // Augustus age in -27 BCE
            0.0,        // No war exhaustion initially
            false,      // No rebellion
            false       // No succession crisis
        );

        MilitaryState military = new MilitaryState(
            250_000,    // Legion size
            50_000,     // Navy size
            1.0,        // Tech advantage
            0.7,        // Logistics score
            false,      // Not at war
            null        // No opponent
        );

        ClimateState climate = new ClimateState(
            0.0,        // No temperature anomaly initially
            0.5,        // Moderate drought index
            0.3,        // Low storm frequency
            0.0         // Sea level at baseline
        );

        Map<String, Double> religionShares = new HashMap<>();
        religionShares.put("Roman Polytheism", 0.85);
        religionShares.put("Christianity", 0.05);
        religionShares.put("Judaism", 0.10);

        ReligionState religion = new ReligionState(
            religionShares,
            0.85,       // High unity (Roman polytheism dominant)
            0.17,       // Stability bonus from unity
            0.05        // Spread rate
        );

        return new CivilizationState(
            "rome",
            "Roman Empire",
            Arrays.asList("Italy", "Gaul", "Hispania", "Britannia", "Greece", "Egypt"),
            "Rome",
            -27,
            population,
            economy,
            technology,
            politics,
            military,
            climate,
            religion
        );
    }

    /**
     * Creates the technology tree from spec - full DAG with prerequisites.
     */
    private static List<Technology> createTechTree() {
        return Arrays.asList(
            // Neolithic base
            new Technology("agriculture", "neolithic", List.of(), 50, 0.1),
            new Technology("mining", "neolithic", List.of(), 50, 0.1),

            // Classical era
            new Technology("ironWorking", "classical", List.of("mining"), 100, 0.08),
            new Technology("metallurgy_advanced", "classical", List.of("ironWorking"), 150, 0.06),
            new Technology("magnetism", "classical", List.of(), 60, 0.1),
            new Technology("copperSmelting", "classical", List.of("mining"), 80, 0.09),

            // Medieval era
            new Technology("coalMining", "medieval", List.of("mining"), 120, 0.07),
            new Technology("chemistry_basic", "medieval", List.of(), 80, 0.09),

            // Industrial era
            new Technology("steamEngine", "industrial", List.of("metallurgy_advanced", "coalMining"), 300, 0.05),
            new Technology("combustion", "industrial", List.of("steamEngine", "chemistry_basic"), 250, 0.05),
            new Technology("electricity", "industrial", List.of("magnetism", "copperSmelting"), 350, 0.04),

            // Modern era
            new Technology("materials_science", "modern", List.of("metallurgy_advanced"), 400, 0.03),
            new Technology("semiconductor", "modern", List.of("electricity", "materials_science"), 500, 0.02),

            // Additional technologies for depth
            new Technology("writing", "neolithic", List.of(), 40, 0.12),
            new Technology("mathematics", "classical", List.of("writing"), 90, 0.08),
            new Technology("optics", "medieval", List.of("mathematics"), 110, 0.07),
            new Technology("telescope", "industrial", List.of("optics"), 200, 0.06),
            new Technology("radio", "industrial", List.of("electricity"), 280, 0.05),
            new Technology("computing", "modern", List.of("electricity", "mathematics"), 450, 0.03),
            new Technology("internet", "modern", List.of("computing", "radio"), 550, 0.02)
        );
    }

    private static WorldConstraints createWorldConstraints() {
        return new WorldConstraints(
            0.6,   // Political stability
            0.45,  // War frequency
            0.25,  // Climate volatility
            0.22,  // Plague probability
            0.65   // Resource abundance
        );
    }

    private static SimulationRules createSimulationRules() {
        return new SimulationRules(
            "adaptive",     // Time step mode
            true,           // Deterministic reproducible
            12345L,         // Base random seed
            50,             // Monte Carlo runs
            8               // Parallel threads
        );
    }
}
