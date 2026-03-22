package org.dynamisengine.ui.debug.model;

/**
 * A debug flag displayed in an overlay panel.
 * Flags represent boolean or state conditions rather than numeric metrics.
 *
 * @param name  the flag name (e.g. "hasContacts", "perceptStale")
 * @param state the current flag state
 */
public record DebugFlagView(
    String name,
    FlagState state
) {
    public DebugFlagView {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (state == null) state = FlagState.OK;
    }
}
