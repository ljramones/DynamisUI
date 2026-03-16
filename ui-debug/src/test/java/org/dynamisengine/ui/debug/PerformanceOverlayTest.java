package org.dynamisengine.ui.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceOverlayTest {

    @Test
    void initialFpsIsZero() {
        PerformanceOverlay overlay = new PerformanceOverlay();
        assertEquals(0f, overlay.fps());
        assertEquals(0f, overlay.avgFrameMs());
    }

    @Test
    void afterUpdateFpsIsPositive() {
        PerformanceOverlay overlay = new PerformanceOverlay();
        overlay.update(16);
        assertTrue(overlay.fps() > 0f, "FPS should be positive after update");
        assertTrue(overlay.avgFrameMs() > 0f, "avgFrameMs should be positive after update");
    }

    @Test
    void buildReturnsPanelWithPerformanceTitle() {
        PerformanceOverlay overlay = new PerformanceOverlay();
        overlay.update(16);
        DebugPanel panel = overlay.build();

        assertEquals("Performance", panel.title());
        assertEquals(2, panel.rows().size());
        assertEquals("FPS", panel.rows().get(0).label());
        assertEquals("Frame", panel.rows().get(1).label());
    }

    @Test
    void multipleUpdatesProduceStableFps() {
        PerformanceOverlay overlay = new PerformanceOverlay();
        for (int i = 0; i < 10; i++) {
            overlay.update(16);
        }
        assertTrue(overlay.fps() > 0f);
        assertTrue(overlay.avgFrameMs() > 0f);
    }

    @Test
    void frameRowFormatsWithMsSuffix() {
        PerformanceOverlay overlay = new PerformanceOverlay();
        overlay.update(16);
        DebugPanel panel = overlay.build();

        String frameValue = panel.rows().get(1).value();
        assertTrue(frameValue.endsWith("ms"), "Frame row should end with 'ms': " + frameValue);
    }
}
