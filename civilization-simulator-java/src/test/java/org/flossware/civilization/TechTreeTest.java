package org.flossware.civilization;

import org.flossware.civilization.model.TechGraph;
import org.flossware.civilization.model.Technology;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests technology tree validation and cycle detection.
 */
class TechTreeTest {

    @Test
    void testValidTechTree() {
        List<Technology> techs = Arrays.asList(
            new Technology("a", "era1", List.of(), 10, 0.1),
            new Technology("b", "era1", List.of("a"), 20, 0.1),
            new Technology("c", "era2", List.of("b"), 30, 0.1),
            new Technology("d", "era2", List.of("a", "b"), 40, 0.1)
        );

        TechGraph graph = new TechGraph(techs);

        assertTrue(graph.exists("a"));
        assertTrue(graph.exists("d"));
        assertEquals(List.of("a"), graph.getPrerequisites("b"));
        assertEquals(2, graph.getPrerequisites("d").size());
    }

    @Test
    void testCycleDetection() {
        List<Technology> techs = Arrays.asList(
            new Technology("a", "era1", List.of("b"), 10, 0.1),
            new Technology("b", "era1", List.of("a"), 20, 0.1)
        );

        assertThrows(IllegalArgumentException.class, () -> {
            new TechGraph(techs);
        }, "Should detect cycle a -> b -> a");
    }

    @Test
    void testSelfReference() {
        List<Technology> techs = Arrays.asList(
            new Technology("a", "era1", List.of("a"), 10, 0.1)
        );

        assertThrows(IllegalArgumentException.class, () -> {
            new TechGraph(techs);
        }, "Should detect self-reference cycle");
    }

    @Test
    void testMissingPrerequisite() {
        List<Technology> techs = Arrays.asList(
            new Technology("a", "era1", List.of("nonexistent"), 10, 0.1)
        );

        assertThrows(IllegalArgumentException.class, () -> {
            new TechGraph(techs);
        }, "Should detect missing prerequisite");
    }

    @Test
    void testComplexDAG() {
        // Diamond dependency:
        //     a
        //    / \
        //   b   c
        //    \ /
        //     d
        List<Technology> techs = Arrays.asList(
            new Technology("a", "era1", List.of(), 10, 0.1),
            new Technology("b", "era2", List.of("a"), 20, 0.1),
            new Technology("c", "era2", List.of("a"), 20, 0.1),
            new Technology("d", "era3", List.of("b", "c"), 30, 0.1)
        );

        TechGraph graph = new TechGraph(techs);

        assertEquals(List.of(), graph.getPrerequisites("a"));
        assertEquals(List.of("a"), graph.getPrerequisites("b"));
        assertEquals(List.of("a"), graph.getPrerequisites("c"));
        assertEquals(2, graph.getPrerequisites("d").size());
        assertTrue(graph.getPrerequisites("d").contains("b"));
        assertTrue(graph.getPrerequisites("d").contains("c"));
    }

    @Test
    void testLongChain() {
        List<Technology> techs = Arrays.asList(
            new Technology("t0", "era1", List.of(), 10, 0.1),
            new Technology("t1", "era1", List.of("t0"), 10, 0.1),
            new Technology("t2", "era1", List.of("t1"), 10, 0.1),
            new Technology("t3", "era1", List.of("t2"), 10, 0.1),
            new Technology("t4", "era1", List.of("t3"), 10, 0.1)
        );

        TechGraph graph = new TechGraph(techs);

        assertEquals(List.of(), graph.getPrerequisites("t0"));
        assertEquals(List.of("t0"), graph.getPrerequisites("t1"));
        assertEquals(List.of("t3"), graph.getPrerequisites("t4"));
    }

    @Test
    void testIndirectCycle() {
        // a -> b -> c -> a
        List<Technology> techs = Arrays.asList(
            new Technology("a", "era1", List.of("c"), 10, 0.1),
            new Technology("b", "era1", List.of("a"), 20, 0.1),
            new Technology("c", "era1", List.of("b"), 30, 0.1)
        );

        assertThrows(IllegalArgumentException.class, () -> {
            new TechGraph(techs);
        }, "Should detect indirect cycle");
    }
}
