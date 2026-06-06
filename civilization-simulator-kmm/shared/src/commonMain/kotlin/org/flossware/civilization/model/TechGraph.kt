package org.flossware.civilization.model

class TechGraph(technologies: List<Technology>) {
    private val nodes: Map<String, Technology> = technologies.associateBy { it.id }
    private val adjacencyList: Map<String, List<String>> = technologies.associate { it.id to it.prerequisites }

    init {
        validateNoCycles()
        validateAllPrerequisitesExist()
    }

    fun getPrerequisites(techId: String): List<String> = adjacencyList[techId] ?: emptyList()
    fun getTechnology(techId: String): Technology? = nodes[techId]
    fun getAllTechnologyIds(): Set<String> = nodes.keys
    fun exists(techId: String): Boolean = nodes.containsKey(techId)

    private fun validateAllPrerequisitesExist() {
        for (tech in nodes.values) {
            for (prereq in tech.prerequisites) {
                require(nodes.containsKey(prereq)) {
                    "Technology '${tech.id}' references non-existent prerequisite: $prereq"
                }
            }
        }
    }

    private enum class Color { WHITE, GRAY, BLACK }

    private fun validateNoCycles() {
        val colors = nodes.keys.associateWith { Color.WHITE }.toMutableMap()
        for (techId in nodes.keys) {
            if (colors[techId] == Color.WHITE) {
                if (hasCycleDFS(techId, colors, ArrayDeque())) {
                    throw IllegalArgumentException("Cycle detected in technology tree")
                }
            }
        }
    }

    private fun hasCycleDFS(current: String, colors: MutableMap<String, Color>, path: ArrayDeque<String>): Boolean {
        colors[current] = Color.GRAY
        path.addLast(current)

        for (prereq in adjacencyList[current] ?: emptyList()) {
            if (colors[prereq] == Color.GRAY) {
                path.addLast(prereq)
                throw IllegalArgumentException("Cycle detected in tech tree: ${path.joinToString(" -> ")}")
            }
            if (colors[prereq] == Color.WHITE) {
                if (hasCycleDFS(prereq, colors, path)) return true
            }
        }

        path.removeLast()
        colors[current] = Color.BLACK
        return false
    }
}
