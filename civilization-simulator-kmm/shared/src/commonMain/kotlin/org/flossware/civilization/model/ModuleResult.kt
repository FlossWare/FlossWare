package org.flossware.civilization.model

data class ModuleResult<T>(
    val state: T,
    val events: List<Event>
)
