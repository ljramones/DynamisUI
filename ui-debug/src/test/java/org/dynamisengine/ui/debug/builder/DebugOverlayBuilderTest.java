package org.dynamisengine.ui.debug.builder;

import org.dynamisengine.ui.debug.model.DebugOverlayPanel;
import org.dynamisengine.ui.debug.model.PanelRegion;
import org.dynamisengine.ui.debug.model.PanelSeverity;
import org.dynamisengine.ui.debug.model.RowSeverity;
import org.dynamisengine.ui.debug.runtime.DebugOverlayOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugOverlayBuilderTest {

    @Test
    void nullSnapshotReturnsEmptyList() {
        var builder = new DebugOverlayBuilder();
        assertEquals(List.of(), builder.buildAll(null));
    }

    @Test
    void emptySnapshotProducesCanonicalPanelOrder() {
        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        // Summary + Alerts + 10 categories = 12
        assertEquals(12, panels.size());

        // Check order
        assertEquals("Engine Summary", panels.get(0).title());
        assertEquals("Alerts", panels.get(1).title());
        assertEquals("Physics", panels.get(2).title());
        assertEquals("ECS", panels.get(3).title());
        assertEquals("Rendering", panels.get(4).title());
        assertEquals("Audio", panels.get(5).title());
        assertEquals("AI", panels.get(6).title());
        assertEquals("Scripting", panels.get(7).title());
        assertEquals("Content", panels.get(8).title());
        assertEquals("Input", panels.get(9).title());
        assertEquals("UI", panels.get(10).title());
        assertEquals("Engine", panels.get(11).title());
    }

    @Test
    void panelOrderMatchesIndexOrder() {
        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        for (int i = 0; i < panels.size(); i++) {
            assertEquals(i, panels.get(i).order(), "Panel " + panels.get(i).title() + " order mismatch");
        }
    }

    @Test
    void summaryPanelIsInTopRegion() {
        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        assertEquals(PanelRegion.TOP, panels.get(0).region());
        assertEquals(PanelRegion.TOP, panels.get(1).region());
    }

    @Test
    void categoryPanelsAreInGridRegion() {
        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        for (int i = 2; i < panels.size(); i++) {
            assertEquals(PanelRegion.GRID, panels.get(i).region(),
                "Panel " + panels.get(i).title() + " should be GRID");
        }
    }

    @Test
    void timelinePanelAppearsWhenEnabled() {
        var options = new DebugOverlayOptions(true, true, true, 60, 16, 8, false);
        var builder = new DebugOverlayBuilder(options);
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        assertEquals(13, panels.size());
        var timeline = panels.getLast();
        assertEquals("Timeline", timeline.title());
        assertEquals(PanelRegion.BOTTOM, timeline.region());
    }

    @Test
    void alertsDisabledSkipsAlertPanel() {
        var options = new DebugOverlayOptions(false, true, false, 60, 16, 8, false);
        var builder = new DebugOverlayBuilder(options);
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        assertEquals(11, panels.size()); // no alerts panel
        assertEquals("Engine Summary", panels.get(0).title());
        assertEquals("Physics", panels.get(1).title()); // alert skipped
    }

    @Test
    void categoryDataPopulatesRows() {
        var physicsMetrics = Map.of("bodies", "200", "contacts", "45");
        var category = new DebugViewSnapshot.DebugCategoryView("Physics",
            Map.of("physics", physicsMetrics), Map.of());
        var snapshot = new DebugViewSnapshot(
            Map.of("physics", category),
            List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY,
            100
        );

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);

        var physicsPanel = panels.stream()
            .filter(p -> "Physics".equals(p.title()))
            .findFirst().orElseThrow();

        assertTrue(physicsPanel.rows().size() >= 2);
    }

    @Test
    void alertsPopulateAlertPanel() {
        var alert = new DebugViewSnapshot.DebugAlertView(
            "physics.stepHigh", "WARNING", "6.2ms > 5.0ms", "6.2", "5.0");
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(alert),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);

        var alertPanel = panels.stream()
            .filter(p -> "Alerts".equals(p.title()))
            .findFirst().orElseThrow();

        assertTrue(alertPanel.highlighted());
        assertEquals(PanelSeverity.WARNING, alertPanel.severity());
        assertFalse(alertPanel.rows().isEmpty());
    }

    @Test
    void maxPanelsBudgetEnforced() {
        var options = new DebugOverlayOptions(true, true, true, 60, 3, 8, false);
        var builder = new DebugOverlayBuilder(options);
        var panels = builder.buildAll(DebugViewSnapshot.EMPTY);

        assertEquals(3, panels.size());
    }

    @Test
    void flagsPopulateWhenEnabled() {
        var category = new DebugViewSnapshot.DebugCategoryView("Physics",
            Map.of("physics", Map.of("bodies", "200")),
            Map.of("hasContacts", "ACTIVE", "sleeping", "OK"));
        var snapshot = new DebugViewSnapshot(
            Map.of("physics", category), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);

        var physicsPanel = panels.stream()
            .filter(p -> "Physics".equals(p.title()))
            .findFirst().orElseThrow();

        assertEquals(2, physicsPanel.flags().size());
    }

    @Test
    void summaryPanelShowsMetrics() {
        var summary = new DebugViewSnapshot.DebugSummaryView(1000, 14.2f, 85f, 19, 300, 5);
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(), summary, 1000);

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);

        var summaryPanel = panels.get(0);
        assertEquals("Engine Summary", summaryPanel.title());
        assertTrue(summaryPanel.rows().size() >= 4);
    }
}
