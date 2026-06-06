package org.flossware.civilization

import org.flossware.civilization.model.TechGraph
import org.flossware.civilization.model.Technology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TechTreeTest {

    @Test
    fun testValidTechTree() {
        val techs = listOf(
            Technology("a", "era1", emptyList(), 10.0, 0.1),
            Technology("b", "era1", listOf("a"), 20.0, 0.1),
            Technology("c", "era2", listOf("b"), 30.0, 0.1),
            Technology("d", "era2", listOf("a", "b"), 40.0, 0.1)
        )
        val graph = TechGraph(techs)
        assertTrue(graph.exists("a"))
        assertTrue(graph.exists("d"))
        assertEquals(listOf("a"), graph.getPrerequisites("b"))
        assertEquals(2, graph.getPrerequisites("d").size)
    }

    @Test
    fun testCycleDetection() {
        val techs = listOf(
            Technology("a", "era1", listOf("b"), 10.0, 0.1),
            Technology("b", "era1", listOf("a"), 20.0, 0.1)
        )
        assertFailsWith<IllegalArgumentException> { TechGraph(techs) }
    }

    @Test
    fun testMissingPrerequisite() {
        val techs = listOf(
            Technology("a", "era1", listOf("nonexistent"), 10.0, 0.1)
        )
        assertFailsWith<IllegalArgumentException> { TechGraph(techs) }
    }
}
