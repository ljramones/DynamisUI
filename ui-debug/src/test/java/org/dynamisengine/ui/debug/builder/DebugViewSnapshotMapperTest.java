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
}
