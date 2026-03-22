package org.dynamisengine.ui.debug.model;

/**
 * Describes a debug category for overlay display.
 * Allows plugins/modules to register custom categories beyond the built-in set.
 *
 * @param id          stable identifier (e.g. "physics", "ai")
 * @param displayName human-readable name (e.g. "Physics", "AI")
 * @param order       display order within the grid (lower = earlier)
 */
public record DebugCategoryDescriptor(
    String id,
    String displayName,
    int order
) {
    public DebugCategoryDescriptor {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (displayName == null || displayName.isBlank()) displayName = id;
    }
}
