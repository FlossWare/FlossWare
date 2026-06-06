package org.flossware.civilization.scenarios

import org.flossware.civilization.model.*

object RomeEnduresScenario {
    fun create(): Scenario {
        return Scenario(
            scenarioId = "rome-endures-2026",
            name = "Rome Survives to Modern Era",
            description = "What if the Western Roman Empire endured through history?",
            startYear = -27,
            endYear = 2026,
            initialState = createInitialRomanState(),
            techTree = createTechTree(),
            worldConstraints = createWorldConstraints(),
            simulationRules = createSimulationRules()
        )
    }

    private fun createInitialRomanState(): CivilizationState {
        val population = PopulationState(5_000_000, 0.03, 0.02, 10_000_000.0, false)
        val tradeRoutes = listOf(
            TradeRoute("Rome", "Egypt", listOf("grain", "papyrus"), 1000.0, 0.1),
            TradeRoute("Rome", "Gaul", listOf("wine", "pottery"), 500.0, 0.15)
        )
        val economy = EconomyState(50_000_000.0, 100_000.0, 80_000.0, 500_000, 0.1, 100_000.0, tradeRoutes)
        val initialTechs = setOf("agriculture", "mining", "ironWorking", "metallurgy_advanced")
        val technology = TechnologyState(initialTechs, emptyMap(), 0.15, 3)
        val politics = PoliticsState(0.6, "Empire", 35, 0.0, false, false)
        val military = MilitaryState(250_000, 50_000, 1.0, 0.7, false, null)
        val climate = ClimateState(0.0, 0.5, 0.3, 0.0)
        val religionShares = mapOf("Roman Polytheism" to 0.85, "Christianity" to 0.05, "Judaism" to 0.10)
        val religion = ReligionState(religionShares, 0.85, 0.17, 0.05)

        return CivilizationState("rome", "Roman Empire",
            listOf("Italy", "Gaul", "Hispania", "Britannia", "Greece", "Egypt"),
            "Rome", -27, population, economy, technology, politics, military, climate, religion)
    }

    private fun createTechTree(): List<Technology> = listOf(
        Technology("agriculture", "neolithic", emptyList(), 50.0, 0.1),
        Technology("mining", "neolithic", emptyList(), 50.0, 0.1),
        Technology("ironWorking", "classical", listOf("mining"), 100.0, 0.08),
        Technology("metallurgy_advanced", "classical", listOf("ironWorking"), 150.0, 0.06),
        Technology("magnetism", "classical", emptyList(), 60.0, 0.1),
        Technology("copperSmelting", "classical", listOf("mining"), 80.0, 0.09),
        Technology("coalMining", "medieval", listOf("mining"), 120.0, 0.07),
        Technology("chemistry_basic", "medieval", emptyList(), 80.0, 0.09),
        Technology("steamEngine", "industrial", listOf("metallurgy_advanced", "coalMining"), 300.0, 0.05),
        Technology("combustion", "industrial", listOf("steamEngine", "chemistry_basic"), 250.0, 0.05),
        Technology("electricity", "industrial", listOf("magnetism", "copperSmelting"), 350.0, 0.04),
        Technology("materials_science", "modern", listOf("metallurgy_advanced"), 400.0, 0.03),
        Technology("semiconductor", "modern", listOf("electricity", "materials_science"), 500.0, 0.02),
        Technology("writing", "neolithic", emptyList(), 40.0, 0.12),
        Technology("mathematics", "classical", listOf("writing"), 90.0, 0.08),
        Technology("optics", "medieval", listOf("mathematics"), 110.0, 0.07),
        Technology("telescope", "industrial", listOf("optics"), 200.0, 0.06),
        Technology("radio", "industrial", listOf("electricity"), 280.0, 0.05),
        Technology("computing", "modern", listOf("electricity", "mathematics"), 450.0, 0.03),
        Technology("internet", "modern", listOf("computing", "radio"), 550.0, 0.02)
    )

    private fun createWorldConstraints() = WorldConstraints(0.6, 0.45, 0.25, 0.015, 0.65)

    private fun createSimulationRules() = SimulationRules("adaptive", true, 12345L, 50, 8)
}
