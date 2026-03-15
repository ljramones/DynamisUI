package org.dynamisengine.ui.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugOverlayTest {

    @Test
    void disabledOverlayRendersNothing() {
        DebugOverlay overlay = new DebugOverlay(false);
        overlay.addPanel(DebugPanel.builder("P").row("A", "1").build());
        StubUIRenderer renderer = new StubUIRenderer();

        overlay.render(renderer);

        assertTrue(renderer.calls.isEmpty());
    }

    @Test
    void toggleFlipsEnabledState() {
        DebugOverlay overlay = new DebugOverlay(false);
        assertFalse(overlay.isEnabled());
        overlay.toggle();
        assertTrue(overlay.isEnabled());
    }

    @Test
    void beginFrameClearsPanelsFromPreviousFrame() {
        DebugOverlay overlay = new DebugOverlay(true);
        overlay.addPanel(DebugPanel.builder("P").row("A", "1").build());
        overlay.beginFrame();
        StubUIRenderer renderer = new StubUIRenderer();

        overlay.render(renderer);

        assertTrue(renderer.calls.isEmpty());
    }

    @Test
    void hiddenPanelIsNotRendered() {
        DebugOverlay overlay = new DebugOverlay(true);
        overlay.addPanel(DebugPanel.builder("Hidden").visible(false).build());
        StubUIRenderer renderer = new StubUIRenderer();

        overlay.render(renderer);

        assertTrue(renderer.calls.isEmpty());
    }

    @Test
    void enabledOverlayWithVisiblePanelDrawsRectAndText() {
        DebugOverlay overlay = new DebugOverlay(true);
        overlay.addPanel(DebugPanel.builder("Perf").row("FPS", "60").build());
        StubUIRenderer renderer = new StubUIRenderer();

        overlay.render(renderer);

        assertTrue(renderer.calls.contains("drawRect"));
        assertTrue(renderer.calls.contains("drawText"));
    }
}
