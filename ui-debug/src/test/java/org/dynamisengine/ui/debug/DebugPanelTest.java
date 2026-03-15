package org.dynamisengine.ui.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugPanelTest {

    @Test
    void builderCreatesPanelWithCorrectTitle() {
        DebugPanel panel = DebugPanel.builder("Perf").build();
        assertEquals("Perf", panel.title());
    }

    @Test
    void rowsAddedInOrderAndRetrievable() {
        DebugPanel panel = DebugPanel.builder("P")
            .row("A", "1")
            .row("B", "2")
            .build();

        assertEquals(2, panel.rows().size());
        assertEquals("A", panel.rows().get(0).label());
        assertEquals("1", panel.rows().get(0).value());
        assertEquals("B", panel.rows().get(1).label());
        assertEquals("2", panel.rows().get(1).value());
    }

    @Test
    void rowFloatUnitFormatsToTwoDecimals() {
        DebugPanel panel = DebugPanel.builder("P")
            .row("Frame", 16.6667f, "ms")
            .build();

        assertEquals("16.67 ms", panel.rows().getFirst().value());
    }

    @Test
    void panelVisibleByDefault() {
        DebugPanel panel = DebugPanel.builder("P").build();
        assertTrue(panel.visible());
    }

    @Test
    void visibleFalseProducesHiddenPanel() {
        DebugPanel panel = DebugPanel.builder("P").visible(false).build();
        assertFalse(panel.visible());
    }
}
