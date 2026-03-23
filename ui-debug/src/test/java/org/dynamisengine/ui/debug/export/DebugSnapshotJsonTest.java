package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;
import org.dynamisengine.ui.debug.model.DebugMiniTrend;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugSnapshotJsonTest {

    @Test
    void serializeEmptySnapshot() {
        String json = DebugSnapshotJson.toJson(DebugViewSnapshot.EMPTY);
        assertNotNull(json);
        assertTrue(json.contains("\"tick\":0"));
        assertTrue(json.contains("\"alerts\":[]"));
        assertTrue(json.contains("\"timelineEvents\":[]"));
    }

    @Test
    void serializeWithSummary() {
        var summary = new DebugViewSnapshot.DebugSummaryView(100, 14.2f, 85f, 5, 300, 4);
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(), summary, 100);
        String json = DebugSnapshotJson.toJson(snapshot);

        assertTrue(json.contains("\"tick\":100"));
        assertTrue(json.contains("\"frameTimeMs\":14.2"));
        assertTrue(json.contains("\"sourceCount\":5"));
    }

    @Test
    void serializeWithAlerts() {
        var alert = new DebugViewSnapshot.DebugAlertView("test.rule", "WARNING", "value too high", "", "");
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(alert),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 50);
        String json = DebugSnapshotJson.toJson(snapshot);

        assertTrue(json.contains("\"ruleName\":\"test.rule\""));
        assertTrue(json.contains("\"severity\":\"WARNING\""));
        assertTrue(json.contains("\"message\":\"value too high\""));
    }

    @Test
    void serializeWithCategories() {
        var cat = new DebugViewSnapshot.DebugCategoryView("Physics",
            Map.of("physics", Map.of("stepTimeMs", "4.5")),
            Map.of("hasContacts", "ACTIVE"),
            List.of());
        var snapshot = new DebugViewSnapshot(Map.of("physics", cat), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 200);
        String json = DebugSnapshotJson.toJson(snapshot);

        assertTrue(json.contains("\"physics\""));
        assertTrue(json.contains("\"stepTimeMs\":\"4.5\""));
        assertTrue(json.contains("\"hasContacts\":\"ACTIVE\""));
    }

    @Test
    void serializeWithTrends() {
        var trend = new DebugMiniTrend("engine.frameTimeMs", 3.0, 15.0, List.of(3.0, 5.0, 10.0, 15.0));
        var cat = new DebugViewSnapshot.DebugCategoryView("Engine",
            Map.of("engine", Map.of("frameTimeMs", "10.0")),
            Map.of(), List.of(trend));
        var snapshot = new DebugViewSnapshot(Map.of("engine", cat), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 300);
        String json = DebugSnapshotJson.toJson(snapshot);

        assertTrue(json.contains("\"name\":\"engine.frameTimeMs\""));
        assertTrue(json.contains("\"min\":3.0"));
        assertTrue(json.contains("\"max\":15.0"));
        assertTrue(json.contains("\"values\":[3.0,5.0,10.0,15.0]"));
    }

    @Test
    void serializeWithTimelineEvents() {
        var event = new DebugViewSnapshot.DebugTimelineEvent(
            842, 1000, "WARNING", "physics", "physics.stepHigh", "step time high");
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 842, List.of(event));
        String json = DebugSnapshotJson.toJson(snapshot);

        assertTrue(json.contains("\"frame\":842"));
        assertTrue(json.contains("\"severity\":\"WARNING\""));
        assertTrue(json.contains("\"source\":\"physics\""));
    }

    @Test
    void escapesSpecialCharacters() {
        var alert = new DebugViewSnapshot.DebugAlertView("test", "WARNING", "value = \"high\"", "", "");
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(alert),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 1);
        String json = DebugSnapshotJson.toJson(snapshot);

        assertTrue(json.contains("\\\"high\\\""));
    }

    @Test
    void roundtripSummary() {
        var summary = new DebugViewSnapshot.DebugSummaryView(500, 12.5f, 75f, 8, 200, 6);
        var original = new DebugViewSnapshot(Map.of(), List.of(), summary, 500);
        String json = DebugSnapshotJson.toJson(original);
        var parsed = DebugSnapshotJson.fromJson(json);

        assertEquals(500, parsed.tick());
        assertEquals(12.5f, parsed.summary().frameTimeMs(), 0.1f);
        assertEquals(75f, parsed.summary().budgetPercent(), 0.1f);
        assertEquals(8, parsed.summary().sourceCount());
    }

    @Test
    void singleLineOutput() {
        var snapshot = new DebugViewSnapshot(Map.of(), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 1);
        String json = DebugSnapshotJson.toJson(snapshot);
        assertFalse(json.contains("\n"), "NDJSON output must be single line");
    }

    @Test
    void schemaVersionIncluded() {
        String json = DebugSnapshotJson.toJson(DebugViewSnapshot.EMPTY);
        assertTrue(json.contains("\"v\":" + DebugSnapshotJson.SCHEMA_VERSION));
    }

    @Test
    void roundtripAlerts() {
        var alerts = List.of(
            new DebugViewSnapshot.DebugAlertView("engine.high", "WARNING", "frame > 8ms", "", ""),
            new DebugViewSnapshot.DebugAlertView("physics.step", "ERROR", "step > 4ms", "", "")
        );
        var original = new DebugViewSnapshot(Map.of(), alerts,
            DebugViewSnapshot.DebugSummaryView.EMPTY, 100);
        var parsed = DebugSnapshotJson.fromJson(DebugSnapshotJson.toJson(original));

        assertEquals(2, parsed.alerts().size());
        assertEquals("engine.high", parsed.alerts().get(0).ruleName());
        assertEquals("WARNING", parsed.alerts().get(0).severity());
        assertEquals("physics.step", parsed.alerts().get(1).ruleName());
        assertEquals("ERROR", parsed.alerts().get(1).severity());
    }

    @Test
    void roundtripTimelineEvents() {
        var events = List.of(
            new DebugViewSnapshot.DebugTimelineEvent(842, 1000, "WARNING", "physics", "step.high", "4.5ms")
        );
        var original = new DebugViewSnapshot(Map.of(), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 842, events);
        var parsed = DebugSnapshotJson.fromJson(DebugSnapshotJson.toJson(original));

        assertEquals(1, parsed.timelineEvents().size());
        assertEquals(842, parsed.timelineEvents().getFirst().frameNumber());
        assertEquals("WARNING", parsed.timelineEvents().getFirst().severity());
        assertEquals("physics", parsed.timelineEvents().getFirst().source());
        assertEquals("step.high", parsed.timelineEvents().getFirst().name());
    }

    @Test
    void roundtripCategories() {
        var trend = new DebugMiniTrend("engine.frameTimeMs", 3.0, 15.0, List.of(3.0, 8.0, 15.0));
        var cat = new DebugViewSnapshot.DebugCategoryView("Engine",
            Map.of("worldengine", Map.of("frameTimeMs", "14.2", "tickRate", "60")),
            Map.of("healthy", "ACTIVE"),
            List.of(trend));
        var original = new DebugViewSnapshot(Map.of("engine", cat), List.of(),
            DebugViewSnapshot.DebugSummaryView.EMPTY, 200);
        var parsed = DebugSnapshotJson.fromJson(DebugSnapshotJson.toJson(original));

        assertTrue(parsed.categories().containsKey("engine"));
        var parsedCat = parsed.categories().get("engine");
        assertEquals("Engine", parsedCat.categoryName());
        assertTrue(parsedCat.sources().containsKey("worldengine"));
        assertEquals("14.2", parsedCat.sources().get("worldengine").get("frameTimeMs"));
        assertEquals("ACTIVE", parsedCat.flags().get("healthy"));
        assertEquals(1, parsedCat.trends().size());
        assertEquals("engine.frameTimeMs", parsedCat.trends().getFirst().metricName());
        assertEquals(3, parsedCat.trends().getFirst().values().size());
    }

    @Test
    void fullRoundtrip() {
        var summary = new DebugViewSnapshot.DebugSummaryView(500, 12.5f, 75f, 5, 300, 4);
        var alert = new DebugViewSnapshot.DebugAlertView("test.rule", "WARNING", "high", "", "");
        var event = new DebugViewSnapshot.DebugTimelineEvent(498, 999, "ERROR", "engine", "crash", "bad");
        var trend = new DebugMiniTrend("metric", 1.0, 10.0, List.of(1.0, 5.0, 10.0));
        var cat = new DebugViewSnapshot.DebugCategoryView("Test",
            Map.of("src", Map.of("val", "42")), Map.of("flag", "OK"), List.of(trend));

        var original = new DebugViewSnapshot(
            Map.of("test", cat), List.of(alert), summary, 500, List.of(event));
        String json = DebugSnapshotJson.toJson(original);
        var parsed = DebugSnapshotJson.fromJson(json);

        assertEquals(500, parsed.tick());
        assertEquals(12.5f, parsed.summary().frameTimeMs(), 0.1f);
        assertEquals(1, parsed.alerts().size());
        assertEquals(1, parsed.timelineEvents().size());
        assertEquals(1, parsed.categories().size());
        assertEquals(1, parsed.categories().get("test").trends().size());
    }
}
