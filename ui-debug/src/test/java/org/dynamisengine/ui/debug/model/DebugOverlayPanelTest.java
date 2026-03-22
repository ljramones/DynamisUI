package org.dynamisengine.ui.debug.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugOverlayPanelTest {

    private static final DebugOverlayPanelId ID = new DebugOverlayPanelId("test", "panel");

    @Test
    void builderCreatesPanel() {
        var panel = DebugOverlayPanel.builder(ID, "Test Panel")
            .row("metric", "42")
            .build();

        assertEquals("Test Panel", panel.title());
        assertEquals(ID, panel.id());
        assertEquals(1, panel.rows().size());
        assertEquals("metric", panel.rows().getFirst().label());
        assertEquals("42", panel.rows().getFirst().value());
    }

    @Test
    void defaultsAreCorrect() {
        var panel = DebugOverlayPanel.builder(ID, "T").build();

        assertEquals(PanelRegion.GRID, panel.region());
        assertEquals(0, panel.order());
        assertEquals(PanelSeverity.NORMAL, panel.severity());
        assertFalse(panel.highlighted());
        assertTrue(panel.rows().isEmpty());
        assertTrue(panel.flags().isEmpty());
        assertTrue(panel.trends().isEmpty());
    }

    @Test
    void severityPropagatesFromRows() {
        var panel = DebugOverlayPanel.builder(ID, "T")
            .row("ok", "1", RowSeverity.NORMAL)
            .row("bad", "2", RowSeverity.ERROR)
            .row("warn", "3", RowSeverity.WARNING)
            .build();

        assertEquals(PanelSeverity.ERROR, panel.severity());
    }

    @Test
    void explicitSeverityOverridesPropagation() {
        var panel = DebugOverlayPanel.builder(ID, "T")
            .row("bad", "2", RowSeverity.ERROR)
            .severity(PanelSeverity.WARNING)
            .build();

        assertEquals(PanelSeverity.WARNING, panel.severity());
    }

    @Test
    void flagsArePreserved() {
        var panel = DebugOverlayPanel.builder(ID, "T")
            .flag("hasContacts", FlagState.ACTIVE)
            .flag("sleeping", FlagState.OK)
            .build();

        assertEquals(2, panel.flags().size());
        assertEquals("hasContacts", panel.flags().get(0).name());
        assertEquals(FlagState.ACTIVE, panel.flags().get(0).state());
    }

    @Test
    void regionAndOrderAreSet() {
        var panel = DebugOverlayPanel.builder(ID, "T")
            .region(PanelRegion.TOP)
            .order(5)
            .build();

        assertEquals(PanelRegion.TOP, panel.region());
        assertEquals(5, panel.order());
    }

    @Test
    void rowsAreImmutable() {
        var panel = DebugOverlayPanel.builder(ID, "T")
            .row("a", "1")
            .build();

        assertThrows(UnsupportedOperationException.class, () -> panel.rows().add(DebugOverlayRow.of("b", "2")));
    }

    @Test
    void panelIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> new DebugOverlayPanelId("", "key"));
        assertThrows(IllegalArgumentException.class, () -> new DebugOverlayPanelId("cat", ""));
    }
}
