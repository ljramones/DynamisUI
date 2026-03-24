package org.dynamisengine.ui.debug.export;

/**
 * A named investigation window: a saved frame range with label and note.
 * Persisted in session metadata for reuse across player sessions.
 *
 * @param name       short descriptive label (e.g. "stress onset")
 * @param startFrame start frame index (0-based)
 * @param endFrame   end frame index (0-based, inclusive)
 * @param note       free-text note explaining why this window matters
 */
public record InvestigationWindow(String name, int startFrame, int endFrame, String note) {

    /** Backwards-compatible constructor without note. */
    public InvestigationWindow(String name, int startFrame, int endFrame) {
        this(name, startFrame, endFrame, "");
    }

    public int frameCount() { return endFrame - startFrame + 1; }

    public float durationSeconds(float fps) { return frameCount() / fps; }
}
