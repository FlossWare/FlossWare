package org.flossware.civilization.module;

import org.flossware.civilization.model.Event;
import java.util.List;

/**
 * Result of a module tick: new state + events generated.
 *
 * @param <T> State type
 */
public record ModuleResult<T>(
    T state,
    List<Event> events
) {
    public ModuleResult {
        events = List.copyOf(events);
    }
}
