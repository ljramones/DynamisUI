package org.dynamisengine.ui.debug.model;

/**
 * Stable identity for a debug overlay panel.
 * Enables panel diffing, incremental updates, and animation stability.
 *
 * @param category the debug category this panel represents
 * @param stableKey a unique key within the category (e.g. "physics", "ai.cognition")
 */
public record DebugOverlayPanelId(
    String category,
    String stableKey
) {
    public DebugOverlayPanelId {
        if (category == null || category.isBlank()) throw new IllegalArgumentException("category required");
        if (stableKey == null || stableKey.isBlank()) throw new IllegalArgumentException("stableKey required");
    }
}
