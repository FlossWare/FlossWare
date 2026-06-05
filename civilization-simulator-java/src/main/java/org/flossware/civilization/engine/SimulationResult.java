package org.flossware.civilization.engine;

import org.flossware.civilization.model.CivilizationState;
import org.flossware.civilization.model.Event;

import java.util.List;

/**
 * Result of a complete simulation run.
 */
public record SimulationResult(
    CivilizationState finalState,
    List<Event> events
) {
    public SimulationResult {
        events = List.copyOf(events);
    }
}
