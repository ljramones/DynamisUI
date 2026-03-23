package org.dynamisengine.ui.debug.runtime;

import org.dynamisengine.ui.debug.model.DebugOverlayPanel;

import java.util.List;

/**
 * Mutable state for the debug overlay runtime: focus mode, panel selection,
 * and replay/time navigation.
 *
 * <p>Keyboard-driven, read-only inspection. Not an editor or interactive UI system.
 */
public final class DebugOverlayState {

    private boolean focusMode;
    private int focusedPanelIndex;
    private boolean replayMode;
    private long selectedFrameNumber;

    // --- Focus mode ---

    public boolean isFocusMode() { return focusMode; }
    public int focusedPanelIndex() { return focusedPanelIndex; }

    public void toggleFocus() { focusMode = !focusMode; }
    public void exitFocus() { focusMode = false; }

    public void nextPanel(int panelCount) {
        if (panelCount <= 0) return;
        focusedPanelIndex = (focusedPanelIndex + 1) % panelCount;
    }

    public void previousPanel(int panelCount) {
        if (panelCount <= 0) return;
        focusedPanelIndex = (focusedPanelIndex - 1 + panelCount) % panelCount;
    }

    public DebugOverlayPanel focusedPanel(List<DebugOverlayPanel> panels) {
        if (panels.isEmpty() || focusedPanelIndex >= panels.size()) return null;
        return panels.get(focusedPanelIndex);
    }

    // --- Replay mode ---

    public boolean isReplayMode() { return replayMode; }
    public long selectedFrameNumber() { return selectedFrameNumber; }

    /** Toggle between live and replay mode. Entering replay freezes at the given live frame. */
    public void toggleReplay(long currentLiveFrame) {
        replayMode = !replayMode;
        if (replayMode) {
            selectedFrameNumber = currentLiveFrame;
        }
    }

    /** Exit replay mode (return to live). */
    public void exitReplay() { replayMode = false; }

    /** Step backward N frames. Clamps to oldest available. */
    public void stepBackward(int steps, long oldestAvailable) {
        selectedFrameNumber = Math.max(oldestAvailable, selectedFrameNumber - steps);
    }

    /** Step forward N frames. Clamps to newest available. */
    public void stepForward(int steps, long newestAvailable) {
        selectedFrameNumber = Math.min(newestAvailable, selectedFrameNumber + steps);
    }

    /** Jump to oldest available frame. */
    public void jumpToOldest(long oldestAvailable) {
        selectedFrameNumber = oldestAvailable;
    }

    /** Jump to newest available frame. */
    public void jumpToNewest(long newestAvailable) {
        selectedFrameNumber = newestAvailable;
    }

    /** Returns a display label for the current mode. */
    public String modeLabel(long liveTick) {
        if (!replayMode) return "[ LIVE ]";
        long delta = liveTick - selectedFrameNumber;
        return String.format("[ REPLAY T=%d  -%d frames ]", selectedFrameNumber, delta);
    }
}
