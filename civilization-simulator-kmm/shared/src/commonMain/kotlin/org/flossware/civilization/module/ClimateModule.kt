package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.math.max
import kotlin.random.Random

object ClimateModule {
    private const val STORM_FREQUENCY_THRESHOLD = 2.8
    private const val DROUGHT_INDEX_THRESHOLD = 0.97

    fun tick(current: ClimateState, volatility: Double, random: Random): ModuleResult<ClimateState> {
        var newTemperatureAnomaly = current.temperatureAnomaly + (random.nextDouble() - 0.5) * 2 * volatility
        var newDroughtIndex = current.droughtIndex + (random.nextDouble() - 0.5) * volatility
        var newStormFrequency = current.stormFrequency + (random.nextDouble() - 0.5) * volatility * 0.5

        newTemperatureAnomaly = newTemperatureAnomaly.coerceIn(-10.0, 10.0)
        newDroughtIndex = newDroughtIndex.coerceIn(0.0, 1.0)
        newStormFrequency = newStormFrequency.coerceIn(0.0, 5.0)

        var newSeaLevelRise = current.seaLevelRise_mm + max(0.0, newTemperatureAnomaly * 0.5)
        newSeaLevelRise = max(0.0, newSeaLevelRise)

        val newState = ClimateState(newTemperatureAnomaly, newDroughtIndex, newStormFrequency, newSeaLevelRise)
        val events = mutableListOf<Event>()

        if (newStormFrequency > STORM_FREQUENCY_THRESHOLD || newDroughtIndex > DROUGHT_INDEX_THRESHOLD) {
            val description = buildDisasterDescription(newStormFrequency, newDroughtIndex)
            val severity = determineSeverity(newStormFrequency, newDroughtIndex)
            events.add(Event(0, "GLOBAL", EventType.CLIMATE_DISASTER, severity, description, null))
        }

        return ModuleResult(newState, events)
    }

    private fun buildDisasterDescription(stormFrequency: Double, droughtIndex: Double): String {
        return when {
            stormFrequency > STORM_FREQUENCY_THRESHOLD && droughtIndex > DROUGHT_INDEX_THRESHOLD ->
                "Extreme climate conditions: severe storms (${"%.2f".format(stormFrequency)}) and severe drought (${"%.2f".format(droughtIndex)})"
            stormFrequency > STORM_FREQUENCY_THRESHOLD ->
                "Severe storm activity detected (frequency: ${"%.2f".format(stormFrequency)})"
            else ->
                "Severe drought conditions (index: ${"%.2f".format(droughtIndex)})"
        }
    }

    private fun determineSeverity(stormFrequency: Double, droughtIndex: Double): EventSeverity {
        val extremeStorms = stormFrequency > 3.5
        val extremeDrought = droughtIndex > 0.985
        return when {
            extremeStorms && extremeDrought -> EventSeverity.CRITICAL
            stormFrequency > STORM_FREQUENCY_THRESHOLD && droughtIndex > DROUGHT_INDEX_THRESHOLD -> EventSeverity.CRITICAL
            stormFrequency > 3.2 || droughtIndex > 0.98 -> EventSeverity.MAJOR
            else -> EventSeverity.MAJOR
        }
    }
}
