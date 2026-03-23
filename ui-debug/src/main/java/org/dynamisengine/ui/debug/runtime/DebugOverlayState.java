package org.dynamisengine.ui.debug.runtime;

import org.dynamisengine.ui.debug.model.DebugOverlayPanel;

import java.util.List;

/**
 * Mutable state for the debug overlay runtime: focus mode, panel selection.
 *
 * <p>Keyboard-driven, read-only inspection. Not an editor or interactive UI system.
 */
public final class DebugOverlayState {

    private boolean focusMode;
    private int focusedPanelIndex;

    public boolean isFocusMode() { return focusMode; }

    public int focusedPanelIndex() { return focusedPanelIndex; }

    /** Toggle focus mode on/off. */
    public void toggleFocus() {
        focusMode = !focusMode;
    }

    /** Exit focus mode. */
    public void exitFocus() {
        focusMode = false;
    }

    /** Cycle to next panel. Wraps around. */
    public void nextPanel(int panelCount) {
        if (panelCount <= 0) return;
        focusedPanelIndex = (focusedPanelIndex + 1) % panelCount;
    }

    /** Cycle to previous panel. Wraps around. */
    public void previousPanel(int panelCount) {
        if (panelCount <= 0) return;
        focusedPanelIndex = (focusedPanelIndex - 1 + panelCount) % panelCount;
    }

    /** Get the currently focused panel from a list, or null if index is out of range. */
    public DebugOverlayPanel focusedPanel(List<DebugOverlayPanel> panels) {
        if (panels.isEmpty() || focusedPanelIndex >= panels.size()) return null;
        return panels.get(focusedPanelIndex);
    }
}
