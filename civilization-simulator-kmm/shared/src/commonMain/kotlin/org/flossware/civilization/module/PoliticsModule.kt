package org.flossware.civilization.module

import org.flossware.civilization.model.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object PoliticsModule {
    private const val REBELLION_THRESHOLD = 0.2
    private const val REBELLION_PROBABILITY = 0.3
    private const val SUCCESSION_AGE_THRESHOLD = 70
    private const val SUCCESSION_PROBABILITY = 0.2

    fun tick(
        current: PoliticsState,
        economicHealth: Double,
        religiousUnity: Double,
        atWar: Boolean,
        yearsPerTick: Double,
        random: Random
    ): ModuleResult<PoliticsState> {
        val events = mutableListOf<Event>()

        var warExhaustion = current.warExhaustion
        if (atWar) {
            warExhaustion = min(1.0, warExhaustion + 0.05 * yearsPerTick)
        } else {
            warExhaustion = max(0.0, warExhaustion - 0.1 * yearsPerTick)
        }

        val volatility = if (current.inRebellion) 0.3 else 0.1
        var newStability = current.stability +
            (economicHealth * 0.4) +
            (religiousUnity * 0.3) -
            (warExhaustion * 0.3) -
            (random.nextDouble() * volatility)
        newStability = newStability.coerceIn(0.0, 1.0)

        val newRulerAge = current.rulerAge + ceil(yearsPerTick).toInt()

        var rebellion = current.inRebellion
        if (newStability < REBELLION_THRESHOLD && random.nextDouble() < REBELLION_PROBABILITY) {
            if (!rebellion) {
                rebellion = true
                events.add(Event(0, "", EventType.REBELLION, EventSeverity.CRITICAL,
                    "Popular rebellion breaks out due to low stability!", newStability.toString()))
            }
        } else if (rebellion && newStability > 0.5) {
            rebellion = false
        }

        var successionCrisis = current.inSuccessionCrisis
        if (newRulerAge > SUCCESSION_AGE_THRESHOLD && random.nextDouble() < SUCCESSION_PROBABILITY) {
            if (!successionCrisis) {
                successionCrisis = true
                events.add(Event(0, "", EventType.SUCCESSION_CRISIS, EventSeverity.MAJOR,
                    "Succession crisis emerges as the ruler ages!", newRulerAge.toString()))
            }
        }

        val newState = PoliticsState(newStability, current.government, newRulerAge, warExhaustion, rebellion, successionCrisis)
        return ModuleResult(newState, events)
    }
}
