package org.flossware.civilization.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Directed Acyclic Graph (DAG) of technologies.
 * Validates no cycles and provides prerequisite lookup.
 */
public final class TechGraph {
    private final Map<String, Technology> nodes;
    private final Map<String, List<String>> adjacencyList;

    public TechGraph(List<Technology> technologies) {
        Objects.requireNonNull(technologies, "Technologies cannot be null");

        this.nodes = technologies.stream()
            .collect(Collectors.toUnmodifiableMap(Technology::id, t -> t));

        this.adjacencyList = technologies.stream()
            .collect(Collectors.toUnmodifiableMap(
                Technology::id,
                Technology::prerequisites
            ));

        validateNoCycles();
        validateAllPrerequisitesExist();
    }

    public List<String> getPrerequisites(String techId) {
        return adjacencyList.getOrDefault(techId, List.of());
    }

    public Technology getTechnology(String techId) {
        return nodes.get(techId);
    }

    public Set<String> getAllTechnologyIds() {
        return nodes.keySet();
    }

    public boolean exists(String techId) {
        return nodes.containsKey(techId);
    }

    /**
     * Validates that all prerequisites reference existing technologies.
     */
    private void validateAllPrerequisitesExist() {
        for (Technology tech : nodes.values()) {
            for (String prereq : tech.prerequisites()) {
                if (!nodes.containsKey(prereq)) {
                    throw new IllegalArgumentException(
                        "Technology '" + tech.id() + "' references non-existent prerequisite: " + prereq
                    );
                }
            }
        }
    }

    /**
     * Detects cycles in the tech tree using DFS with color marking.
     * WHITE = unvisited, GRAY = visiting, BLACK = visited
     */
    private void validateNoCycles() {
        Map<String, Color> colors = new HashMap<>();
        nodes.keySet().forEach(id -> colors.put(id, Color.WHITE));

        for (String techId : nodes.keySet()) {
            if (colors.get(techId) == Color.WHITE) {
                if (hasCycleDFS(techId, colors, new ArrayDeque<>())) {
                    throw new IllegalArgumentException("Cycle detected in technology tree");
                }
            }
        }
    }

    private boolean hasCycleDFS(String current, Map<String, Color> colors, Deque<String> path) {
        colors.put(current, Color.GRAY);
        path.addLast(current);

        for (String prereq : adjacencyList.get(current)) {
            if (colors.get(prereq) == Color.GRAY) {
                path.addLast(prereq);
                throw new IllegalArgumentException(
                    "Cycle detected in tech tree: " + String.join(" -> ", path)
                );
            }
            if (colors.get(prereq) == Color.WHITE) {
                if (hasCycleDFS(prereq, colors, path)) {
                    return true;
                }
            }
        }

        path.removeLast();
        colors.put(current, Color.BLACK);
        return false;
    }

    private enum Color { WHITE, GRAY, BLACK }
}
