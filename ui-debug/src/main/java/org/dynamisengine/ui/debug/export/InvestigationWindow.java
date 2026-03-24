package org.dynamisengine.ui.debug.export;

/**
 * A named investigation window: a saved frame range with a label.
 * Persisted in session metadata for reuse across player sessions.
 *
 * @param name       descriptive label (e.g. "stress onset", "worst regression")
 * @param startFrame start frame index (0-based)
 * @param endFrame   end frame index (0-based, inclusive)
 */
public record InvestigationWindow(String name, int startFrame, int endFrame) {

    public int frameCount() { return endFrame - startFrame + 1; }

    public float durationSeconds(float fps) { return frameCount() / fps; }
}
