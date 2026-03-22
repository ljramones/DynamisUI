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

    @Test
    void timelinePanelShowsEventsWhenPresent() {
        var event = new DebugViewSnapshot.DebugTimelineEvent(
            100, 1000, "WARNING", "physics", "physics.stepHigh", "6.2ms > 5.0ms");
        var options = new DebugOverlayOptions(true, true, true, 60, 16, 8, false);
        var builder = new DebugOverlayBuilder(options);
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100, List.of(event));

        var panels = builder.buildAll(snapshot);
        var timeline = panels.stream()
            .filter(p -> "Timeline".equals(p.title()))
            .findFirst().orElseThrow();

        // Should have event row(s) + total count row
        assertTrue(timeline.rows().size() >= 2);
        assertTrue(timeline.rows().stream().anyMatch(r -> r.value().contains("6.2ms")));
    }

    @Test
    void timelinePanelEmptyWhenNoEvents() {
        var options = new DebugOverlayOptions(true, true, true, 60, 16, 8, false);
        var builder = new DebugOverlayBuilder(options);
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100, List.of());

        var panels = builder.buildAll(snapshot);
        var timeline = panels.stream()
            .filter(p -> "Timeline".equals(p.title()))
            .findFirst().orElseThrow();

        assertTrue(timeline.rows().stream().anyMatch(r -> r.value().contains("no recent events")));
    }

    @Test
    void alertsGroupedByRuleName() {
        var alerts = List.of(
            new DebugViewSnapshot.DebugAlertView("engine.budgetHigh", "WARNING", "110%", "", ""),
            new DebugViewSnapshot.DebugAlertView("engine.budgetHigh", "WARNING", "120%", "", ""),
            new DebugViewSnapshot.DebugAlertView("engine.budgetHigh", "WARNING", "130%", "", ""),
            new DebugViewSnapshot.DebugAlertView("ai.budgetExceeded", "ERROR", "115%", "", "")
        );
        var snapshot = new DebugViewSnapshot(Map.of(), alerts,
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);
        var alertPanel = panels.stream()
            .filter(p -> "Alerts".equals(p.title()))
            .findFirst().orElseThrow();

        // Should have: summary row + 2 grouped alert rows (not 4 individual rows)
        // Summary: "C:0  E:1  W:3"
        // [E] ai.budgetExceeded (sorted first by severity)
        // [W] engine.budgetHigh x3
        assertTrue(alertPanel.rows().stream().anyMatch(r -> r.value().contains("E:1")));
        assertTrue(alertPanel.rows().stream().anyMatch(r -> r.label().contains("x3")));
    }

    @Test
    void alertsSortedBySeverityThenCount() {
        var alerts = List.of(
            new DebugViewSnapshot.DebugAlertView("warn1", "WARNING", "w1", "", ""),
            new DebugViewSnapshot.DebugAlertView("warn1", "WARNING", "w2", "", ""),
            new DebugViewSnapshot.DebugAlertView("error1", "ERROR", "e1", "", "")
        );
        var snapshot = new DebugViewSnapshot(Map.of(), alerts,
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);
        var alertPanel = panels.stream()
            .filter(p -> "Alerts".equals(p.title()))
            .findFirst().orElseThrow();

        // First row after summary should be error (higher severity), not warning
        var alertRows = alertPanel.rows().stream()
            .filter(r -> r.label().startsWith("[")).toList();
        assertTrue(alertRows.getFirst().label().startsWith("[E]"));
    }

    @Test
    void alertsCappedWithMoreIndicator() {
        var alerts = new java.util.ArrayList<DebugViewSnapshot.DebugAlertView>();
        for (int i = 0; i < 20; i++) {
            alerts.add(new DebugViewSnapshot.DebugAlertView(
                "rule" + i, "WARNING", "msg" + i, "", ""));
        }
        // maxRowsPerPanel = 4 means 3 alert rows + summary
        var options = new DebugOverlayOptions(false, true, true, 60, 16, 4, false);
        var builder = new DebugOverlayBuilder(options);
        var snapshot = new DebugViewSnapshot(Map.of(), alerts,
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);

        var panels = builder.buildAll(snapshot);
        var alertPanel = panels.stream()
            .filter(p -> "Alerts".equals(p.title()))
            .findFirst().orElseThrow();

        // Should show "+N more" row
        assertTrue(alertPanel.rows().stream().anyMatch(r -> r.value().contains("more")));
    }

    @Test
    void categoryPanelIncludesTrends() {
        var trend = new org.dynamisengine.ui.debug.model.DebugMiniTrend(
            "worldengine.frameTimeMs", 10.0, 20.0, List.of(10.0, 12.0, 15.0, 18.0, 20.0));
        var category = new DebugViewSnapshot.DebugCategoryView("Engine",
            Map.of("worldengine", Map.of("frameTimeMs", "15.0")),
            Map.of(), List.of(trend));
        var snapshot = new DebugViewSnapshot(
            Map.of("engine", category), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);

        var builder = new DebugOverlayBuilder();
        var panels = builder.buildAll(snapshot);

        var enginePanel = panels.stream()
            .filter(p -> "Engine".equals(p.title()))
            .findFirst().orElseThrow();

        assertEquals(1, enginePanel.trends().size());
        assertEquals("worldengine.frameTimeMs", enginePanel.trends().getFirst().metricName());
    }
}
