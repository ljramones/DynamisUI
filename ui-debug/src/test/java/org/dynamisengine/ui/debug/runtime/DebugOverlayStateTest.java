package org.dynamisengine.ui.debug.runtime;

import org.dynamisengine.ui.debug.model.DebugOverlayPanel;
import org.dynamisengine.ui.debug.model.DebugOverlayPanelId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebugOverlayStateTest {

    private static DebugOverlayPanel panel(String name) {
        return DebugOverlayPanel.builder(
            new DebugOverlayPanelId("test", name), name).build();
    }

    @Test
    void initiallyNotInFocusMode() {
        var state = new DebugOverlayState();
        assertFalse(state.isFocusMode());
        assertEquals(0, state.focusedPanelIndex());
    }

    @Test
    void toggleFocusFlipsState() {
        var state = new DebugOverlayState();
        state.toggleFocus();
        assertTrue(state.isFocusMode());
        state.toggleFocus();
        assertFalse(state.isFocusMode());
    }

    @Test
    void nextPanelWrapsAround() {
        var state = new DebugOverlayState();
        state.nextPanel(3);
        assertEquals(1, state.focusedPanelIndex());
        state.nextPanel(3);
        assertEquals(2, state.focusedPanelIndex());
        state.nextPanel(3);
        assertEquals(0, state.focusedPanelIndex()); // wraps
    }

    @Test
    void previousPanelWrapsAround() {
        var state = new DebugOverlayState();
        state.previousPanel(3);
        assertEquals(2, state.focusedPanelIndex()); // wraps backward
        state.previousPanel(3);
        assertEquals(1, state.focusedPanelIndex());
    }

    @Test
    void focusedPanelReturnsCorrectPanel() {
        var panels = List.of(panel("A"), panel("B"), panel("C"));
        var state = new DebugOverlayState();
        assertEquals("A", state.focusedPanel(panels).title());
        state.nextPanel(3);
        assertEquals("B", state.focusedPanel(panels).title());
    }

    @Test
    void focusedPanelReturnsNullForEmptyList() {
        var state = new DebugOverlayState();
        assertNull(state.focusedPanel(List.of()));
    }

    @Test
    void nextPanelSafeWithZeroCount() {
        var state = new DebugOverlayState();
        state.nextPanel(0); // should not crash
        assertEquals(0, state.focusedPanelIndex());
    }

    @Test
    void exitFocusAlwaysDisables() {
        var state = new DebugOverlayState();
        state.toggleFocus();
        assertTrue(state.isFocusMode());
        state.exitFocus();
        assertFalse(state.isFocusMode());
    }
}
