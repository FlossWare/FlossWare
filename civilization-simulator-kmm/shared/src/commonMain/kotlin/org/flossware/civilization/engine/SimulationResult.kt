package org.flossware.civilization.engine

import org.flossware.civilization.model.CivilizationState
import org.flossware.civilization.model.Event

data class SimulationResult(
    val finalState: CivilizationState,
    val events: List<Event>
)
