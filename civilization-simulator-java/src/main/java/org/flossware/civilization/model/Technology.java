package org.flossware.civilization.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a technology node in the research tree.
 * Immutable record for thread safety and reproducibility.
 */
public record Technology(
    String id,
    String era,
    List<String> prerequisites,
    double researchCost,
    double diffusionRate
) {
    public Technology {
        Objects.requireNonNull(id, "Technology id cannot be null");
        Objects.requireNonNull(era, "Technology era cannot be null");
        Objects.requireNonNull(prerequisites, "Prerequisites cannot be null");

        if (researchCost <= 0) {
            throw new IllegalArgumentException("Research cost must be positive: " + researchCost);
        }
        if (diffusionRate < 0 || diffusionRate > 1) {
            throw new IllegalArgumentException("Diffusion rate must be in [0, 1]: " + diffusionRate);
        }

        prerequisites = List.copyOf(prerequisites);
    }
}
