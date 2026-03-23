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

    // --- Replay tests ---

    @Test
    void initiallyNotInReplayMode() {
        var state = new DebugOverlayState();
        assertFalse(state.isReplayMode());
    }

    @Test
    void toggleReplayFreezesAtCurrentFrame() {
        var state = new DebugOverlayState();
        state.toggleReplay(500);
        assertTrue(state.isReplayMode());
        assertEquals(500, state.selectedFrameNumber());
    }

    @Test
    void toggleReplayExitsCleanly() {
        var state = new DebugOverlayState();
        state.toggleReplay(500);
        state.toggleReplay(600); // exits replay
        assertFalse(state.isReplayMode());
    }

    @Test
    void stepBackwardClampsToOldest() {
        var state = new DebugOverlayState();
        state.toggleReplay(100);
        state.stepBackward(50, 80);
        assertEquals(80, state.selectedFrameNumber()); // clamped to oldest
    }

    @Test
    void stepForwardClampsToNewest() {
        var state = new DebugOverlayState();
        state.toggleReplay(100);
        state.stepForward(50, 120);
        assertEquals(120, state.selectedFrameNumber()); // clamped
    }

    @Test
    void stepBackwardNormal() {
        var state = new DebugOverlayState();
        state.toggleReplay(100);
        state.stepBackward(10, 0);
        assertEquals(90, state.selectedFrameNumber());
    }

    @Test
    void stepForwardNormal() {
        var state = new DebugOverlayState();
        state.toggleReplay(100);
        state.stepForward(5, 200);
        assertEquals(105, state.selectedFrameNumber());
    }

    @Test
    void jumpToOldestAndNewest() {
        var state = new DebugOverlayState();
        state.toggleReplay(100);
        state.jumpToOldest(10);
        assertEquals(10, state.selectedFrameNumber());
        state.jumpToNewest(200);
        assertEquals(200, state.selectedFrameNumber());
    }

    @Test
    void modeLabelShowsLiveOrReplay() {
        var state = new DebugOverlayState();
        assertEquals("[ LIVE ]", state.modeLabel(100));
        state.toggleReplay(80);
        assertTrue(state.modeLabel(100).contains("REPLAY"));
        assertTrue(state.modeLabel(100).contains("T=80"));
    }

    @Test
    void focusAndReplayCoexist() {
        var state = new DebugOverlayState();
        state.toggleFocus();
        state.toggleReplay(50);
        assertTrue(state.isFocusMode());
        assertTrue(state.isReplayMode());
        assertEquals(50, state.selectedFrameNumber());
    }
}
