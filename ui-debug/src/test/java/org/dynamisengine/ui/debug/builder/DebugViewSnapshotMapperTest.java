package org.dynamisengine.ui.debug.builder;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.core.DebugSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugViewSnapshotMapperTest {

    private DebugSession session;
    private DebugViewSnapshotMapper mapper;

    @BeforeEach
    void setUp() {
        session = new DebugSession();
        mapper = new DebugViewSnapshotMapper(session);
    }

    @Test
    void emptySessionProducesValidSnapshot() {
        var snapshot = mapper.map(1);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.tick());
        assertTrue(snapshot.categories().isEmpty());
        assertTrue(snapshot.alerts().isEmpty());
        assertNotNull(snapshot.summary());
    }

    @Test
    void mapFromFramePopulatesCategories() {
        var physicsSnap = new DebugSnapshot(
            100, System.currentTimeMillis(), "physics", DebugCategory.PHYSICS,
            Map.of("bodies", 200.0, "contacts", 45.0),
            Map.of("hasContacts", true),
            "physics ok"
        );
        var audioSnap = new DebugSnapshot(
            100, System.currentTimeMillis(), "audio", DebugCategory.AUDIO,
            Map.of("voices", 12.0, "dspBudget", 72.5),
            Map.of(),
            "audio ok"
        );

        var viewSnap = mapper.mapFromFrame(100, Map.of("physics", physicsSnap, "audio", audioSnap));

        // Categories should exist
        assertTrue(viewSnap.categories().containsKey("physics"));
        assertTrue(viewSnap.categories().containsKey("audio"));

        // Physics category has source "physics" with metrics
        var physicsCat = viewSnap.categories().get("physics");
        assertEquals("Physics", physicsCat.categoryName());
        assertTrue(physicsCat.sources().containsKey("physics"));
        assertEquals("200", physicsCat.sources().get("physics").get("bodies"));
        assertEquals("45", physicsCat.sources().get("physics").get("contacts"));

        // Physics flags
        assertEquals("ACTIVE", physicsCat.flags().get("hasContacts"));

        // Audio category
        var audioCat = viewSnap.categories().get("audio");
        assertEquals("72.50", audioCat.sources().get("audio").get("dspBudget"));
    }

    @Test
    void multipleSourcesSameCategory() {
        var gpuSnap = new DebugSnapshot(
            100, System.currentTimeMillis(), "gpu", DebugCategory.RENDERING,
            Map.of("backlog", 5.0), Map.of(), ""
        );
        var lightSnap = new DebugSnapshot(
            100, System.currentTimeMillis(), "lightengine", DebugCategory.RENDERING,
            Map.of("drawCalls", 120.0), Map.of(), ""
        );

        var viewSnap = mapper.mapFromFrame(100, Map.of("gpu", gpuSnap, "lightengine", lightSnap));

        var rendering = viewSnap.categories().get("rendering");
        assertNotNull(rendering);
        assertEquals(2, rendering.sources().size());
        assertTrue(rendering.sources().containsKey("gpu"));
        assertTrue(rendering.sources().containsKey("lightengine"));
    }

    @Test
    void alertsMappedFromRecentEvents() {
        session.submit(new DebugEvent(100, System.currentTimeMillis(),
            "physics", DebugCategory.PHYSICS, DebugSeverity.WARNING,
            "physics.stepHigh", "6.2ms > 5.0ms"));
        session.submit(new DebugEvent(100, System.currentTimeMillis(),
            "ai", DebugCategory.AI, DebugSeverity.ERROR,
            "ai.budgetExceeded", "112% > 100%"));
        // INFO severity should NOT appear in alerts
        session.submit(new DebugEvent(100, System.currentTimeMillis(),
            "ecs", DebugCategory.ECS, DebugSeverity.INFO,
            "ecs.entityCreated", "new entity 42"));

        var viewSnap = mapper.map(100);

        assertEquals(2, viewSnap.alerts().size());

        var first = viewSnap.alerts().get(0);
        assertEquals("physics.stepHigh", first.ruleName());
        assertEquals("WARNING", first.severity());

        var second = viewSnap.alerts().get(1);
        assertEquals("ai.budgetExceeded", second.ruleName());
        assertEquals("ERROR", second.severity());
    }

    @Test
    void summaryPopulatedFromFrameData() {
        var engineSnap = new DebugSnapshot(
            200, System.currentTimeMillis(), "worldengine", DebugCategory.ENGINE,
            Map.of("frameTimeMs", 14.2), Map.of(), ""
        );
        var physicsSnap = new DebugSnapshot(
            200, System.currentTimeMillis(), "physics", DebugCategory.PHYSICS,
            Map.of("bodies", 100.0), Map.of(), ""
        );

        var viewSnap = mapper.mapFromFrame(200, Map.of("worldengine", engineSnap, "physics", physicsSnap));

        assertEquals(200, viewSnap.summary().tick());
        assertEquals(14.2f, viewSnap.summary().frameTimeMs(), 0.01f);
        assertTrue(viewSnap.summary().budgetPercent() > 80f); // 14.2/16.667 ≈ 85%
        assertEquals(2, viewSnap.summary().sourceCount());
    }

    @Test
    void summaryDefaultsWhenNoEngineSnapshot() {
        var viewSnap = mapper.mapFromFrame(100, Map.of());

        assertEquals(0f, viewSnap.summary().frameTimeMs());
        assertEquals(0f, viewSnap.summary().budgetPercent());
        assertEquals(0, viewSnap.summary().sourceCount());
    }

    @Test
    void historyDepthReflectsRecordedFrames() {
        // Record some frames
        session.history().record(1, Map.of("a", new DebugSnapshot(
            1, 0, "a", DebugCategory.PHYSICS, Map.of(), Map.of(), "")));
        session.history().record(2, Map.of("a", new DebugSnapshot(
            2, 0, "a", DebugCategory.PHYSICS, Map.of(), Map.of(), "")));

        var viewSnap = mapper.map(3);

        assertEquals(2, viewSnap.summary().historyDepth());
    }

    @Test
    void integerMetricsFormattedWithoutDecimals() {
        var snap = new DebugSnapshot(
            100, 0, "ecs", DebugCategory.ECS,
            Map.of("entities", 500.0, "avgTickMs", 2.345),
            Map.of(), ""
        );

        var viewSnap = mapper.mapFromFrame(100, Map.of("ecs", snap));
        var ecsCat = viewSnap.categories().get("ecs");
        var metrics = ecsCat.sources().get("ecs");

        assertEquals("500", metrics.get("entities"));
        assertEquals("2.35", metrics.get("avgTickMs"));
    }

    @Test
    void flagsFalseMapToOk() {
        var snap = new DebugSnapshot(
            100, 0, "physics", DebugCategory.PHYSICS,
            Map.of(), Map.of("sleeping", false), ""
        );

        var viewSnap = mapper.mapFromFrame(100, Map.of("physics", snap));
        assertEquals("OK", viewSnap.categories().get("physics").flags().get("sleeping"));
    }

    @Test
    void mapFromHistoryViaMapMethod() {
        // Record a frame in history
        var snap = new DebugSnapshot(
            50, System.currentTimeMillis(), "audio", DebugCategory.AUDIO,
            Map.of("voices", 8.0), Map.of(), ""
        );
        session.history().record(50, Map.of("audio", snap));

        // map() should read from history
        var viewSnap = mapper.map(51);

        assertTrue(viewSnap.categories().containsKey("audio"));
        assertEquals("8", viewSnap.categories().get("audio").sources().get("audio").get("voices"));
    }

    // --- Trend tests ---

    @Test
    void trendsExtractedFromHistory() {
        // Record 5 frames with worldengine frameTimeMs
        for (int i = 1; i <= 5; i++) {
            session.history().record(i, Map.of("worldengine", new DebugSnapshot(
                i, i * 100L, "worldengine", DebugCategory.ENGINE,
                Map.of("frameTimeMs", 10.0 + i, "budgetPercent", 60.0 + i * 5),
                Map.of(), ""
            )));
        }

        var viewSnap = mapper.map(6);
        var engineCat = viewSnap.categories().get("engine");
        assertNotNull(engineCat);
        assertFalse(engineCat.trends().isEmpty(), "Should have trends from history");

        // Should have frameTimeMs trend
        var frameTimeTrend = engineCat.trends().stream()
            .filter(t -> t.metricName().equals("worldengine.frameTimeMs"))
            .findFirst();
        assertTrue(frameTimeTrend.isPresent());
        assertEquals(5, frameTimeTrend.get().values().size());
        assertEquals(11.0, frameTimeTrend.get().min(), 0.01);
        assertEquals(15.0, frameTimeTrend.get().max(), 0.01);
    }

    @Test
    void noTrendsWithInsufficientHistory() {
        // Only 1 frame — not enough for trends
        session.history().record(1, Map.of("worldengine", new DebugSnapshot(
            1, 100, "worldengine", DebugCategory.ENGINE,
            Map.of("frameTimeMs", 10.0), Map.of(), ""
        )));

        var viewSnap = mapper.map(2);
        var engineCat = viewSnap.categories().get("engine");
        assertNotNull(engineCat);
        assertTrue(engineCat.trends().isEmpty(), "Should have no trends with < 2 frames");
    }

    @Test
    void trendsOnlyForConfiguredMetrics() {
        // Record frames with a non-trended metric
        for (int i = 1; i <= 3; i++) {
            session.history().record(i, Map.of("ecs", new DebugSnapshot(
                i, i * 100L, "ecs", DebugCategory.ECS,
                Map.of("entityCount", 100.0 + i, "unusualMetric", 42.0),
                Map.of(), ""
            )));
        }

        var viewSnap = mapper.map(4);
        var ecsCat = viewSnap.categories().get("ecs");
        assertNotNull(ecsCat);

        // entityCount is configured for ecs trends
        assertTrue(ecsCat.trends().stream().anyMatch(t -> t.metricName().equals("ecs.entityCount")));
        // unusualMetric is NOT configured
        assertTrue(ecsCat.trends().stream().noneMatch(t -> t.metricName().contains("unusualMetric")));
    }

    @Test
    void trendMinMaxNormalized() {
        for (int i = 1; i <= 4; i++) {
            session.history().record(i, Map.of("worldengine", new DebugSnapshot(
                i, 0, "worldengine", DebugCategory.ENGINE,
                Map.of("frameTimeMs", i * 5.0), Map.of(), ""
            )));
        }

        var viewSnap = mapper.map(5);
        var trend = viewSnap.categories().get("engine").trends().stream()
            .filter(t -> t.metricName().equals("worldengine.frameTimeMs"))
            .findFirst().orElseThrow();

        assertEquals(5.0, trend.min(), 0.01);
        assertEquals(20.0, trend.max(), 0.01);
        assertEquals(4, trend.values().size());
    }

    // --- Timeline tests ---

    @Test
    void timelineEventsPopulatedFromRecentEvents() {
        session.submit(new DebugEvent(10, 1000, "physics", DebugCategory.PHYSICS,
            DebugSeverity.WARNING, "physics.stepHigh", "step time high"));
        session.submit(new DebugEvent(12, 1200, "ai", DebugCategory.AI,
            DebugSeverity.ERROR, "ai.budgetExceeded", "budget exceeded"));
        // INFO should not appear in timeline
        session.submit(new DebugEvent(13, 1300, "ecs", DebugCategory.ECS,
            DebugSeverity.INFO, "ecs.created", "entity created"));

        var viewSnap = mapper.map(14);

        assertEquals(2, viewSnap.timelineEvents().size());
        assertEquals("physics.stepHigh", viewSnap.timelineEvents().get(0).name());
        assertEquals("WARNING", viewSnap.timelineEvents().get(0).severity());
        assertEquals("ai.budgetExceeded", viewSnap.timelineEvents().get(1).name());
        assertEquals("ERROR", viewSnap.timelineEvents().get(1).severity());
    }

    @Test
    void emptyTimelineWhenNoEvents() {
        var viewSnap = mapper.map(1);
        assertTrue(viewSnap.timelineEvents().isEmpty());
    }

    @Test
    void criticalEventsMapToTimeline() {
        session.submit(new DebugEvent(1, 100, "engine", DebugCategory.ENGINE,
            DebugSeverity.CRITICAL, "engine.crash", "fatal error"));

        var viewSnap = mapper.map(2);
        assertEquals(1, viewSnap.timelineEvents().size());
        assertEquals("CRITICAL", viewSnap.timelineEvents().getFirst().severity());
    }
}
