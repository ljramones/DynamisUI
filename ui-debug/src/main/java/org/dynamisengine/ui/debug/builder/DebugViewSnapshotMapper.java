package org.dynamisengine.ui.debug.builder;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.core.DebugHistory;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.debug.core.DebugTimeline;

import java.util.*;

/**
 * Maps runtime debug state from {@link DebugSession} into the UI-facing
 * {@link DebugViewSnapshot} contract.
 *
 * <p>This is the authoritative production bridge between DynamisDebug (diagnostic
 * truth) and DynamisUI (diagnostic presentation). Proving modules should use this
 * mapper rather than building their own transformation logic.
 *
 * <p>The mapper is deterministic, loss-aware (empty categories produce valid
 * empty snapshots), and never invents synthetic data.
 */
public final class DebugViewSnapshotMapper {

    /** Category key → DebugCategory enum mapping for canonical panel ordering. */
    private static final Map<String, DebugCategory> CATEGORY_MAP = Map.ofEntries(
        Map.entry("physics", DebugCategory.PHYSICS),
        Map.entry("ecs", DebugCategory.ECS),
        Map.entry("rendering", DebugCategory.RENDERING),
        Map.entry("audio", DebugCategory.AUDIO),
        Map.entry("ai", DebugCategory.AI),
        Map.entry("scripting", DebugCategory.SCRIPTING),
        Map.entry("content", DebugCategory.CONTENT),
        Map.entry("input", DebugCategory.INPUT),
        Map.entry("ui", DebugCategory.UI),
        Map.entry("engine", DebugCategory.ENGINE)
    );

    /** Source name → category key mapping. Multiple sources can map to one category. */
    private static final Map<DebugCategory, String> CATEGORY_KEYS = Map.ofEntries(
        Map.entry(DebugCategory.PHYSICS, "physics"),
        Map.entry(DebugCategory.ECS, "ecs"),
        Map.entry(DebugCategory.RENDERING, "rendering"),
        Map.entry(DebugCategory.AUDIO, "audio"),
        Map.entry(DebugCategory.AI, "ai"),
        Map.entry(DebugCategory.SCRIPTING, "scripting"),
        Map.entry(DebugCategory.CONTENT, "content"),
        Map.entry(DebugCategory.INPUT, "input"),
        Map.entry(DebugCategory.UI, "ui"),
        Map.entry(DebugCategory.ENGINE, "engine"),
        Map.entry(DebugCategory.NETWORK, "network"),
        Map.entry(DebugCategory.CUSTOM, "custom")
    );

    private final DebugSession session;

    public DebugViewSnapshotMapper(DebugSession session) {
        this.session = Objects.requireNonNull(session, "session required");
    }

    /**
     * Maps the current debug state into a {@link DebugViewSnapshot}.
     *
     * <p>Reads the latest frame from history, recent events for alerts,
     * session-level flags, and timeline data. Returns a fully populated
     * snapshot suitable for {@link DebugOverlayBuilder}.
     *
     * @param frameNumber the current engine tick
     * @return a complete view snapshot (never null)
     */
    public DebugViewSnapshot map(long frameNumber) {
        Map<String, DebugSnapshot> frameSnapshots = latestFrameSnapshots();

        return new DebugViewSnapshot(
            mapCategories(frameSnapshots),
            mapAlerts(),
            mapSummary(frameNumber, frameSnapshots),
            frameNumber
        );
    }

    /**
     * Maps from an explicit frame snapshot map (e.g. the return value of
     * {@code DebugBridge.captureFrame()}).
     */
    public DebugViewSnapshot mapFromFrame(long frameNumber, Map<String, DebugSnapshot> frameSnapshots) {
        return new DebugViewSnapshot(
            mapCategories(frameSnapshots),
            mapAlerts(),
            mapSummary(frameNumber, frameSnapshots),
            frameNumber
        );
    }

    // --- Category mapping ---

    private Map<String, DebugViewSnapshot.DebugCategoryView> mapCategories(
            Map<String, DebugSnapshot> frameSnapshots) {

        // Group snapshots by category key
        Map<String, Map<String, DebugSnapshot>> grouped = new LinkedHashMap<>();
        for (var entry : frameSnapshots.entrySet()) {
            String source = entry.getKey();
            DebugSnapshot snapshot = entry.getValue();
            String categoryKey = categoryKeyFor(snapshot.category());
            grouped.computeIfAbsent(categoryKey, k -> new LinkedHashMap<>())
                   .put(source, snapshot);
        }

        // Build category views
        Map<String, DebugViewSnapshot.DebugCategoryView> categories = new LinkedHashMap<>();
        for (var entry : grouped.entrySet()) {
            String categoryKey = entry.getKey();
            Map<String, DebugSnapshot> snapshots = entry.getValue();
            categories.put(categoryKey, buildCategoryView(categoryKey, snapshots));
        }

        return categories;
    }

    private DebugViewSnapshot.DebugCategoryView buildCategoryView(
            String categoryKey, Map<String, DebugSnapshot> snapshots) {

        // Sources → metrics (Map<String, Map<String, String>>)
        Map<String, Map<String, String>> sources = new LinkedHashMap<>();
        Map<String, String> allFlags = new LinkedHashMap<>();

        for (var entry : snapshots.entrySet()) {
            String source = entry.getKey();
            DebugSnapshot snap = entry.getValue();

            // Convert numeric metrics to strings
            Map<String, String> metricStrings = new LinkedHashMap<>();
            for (var m : snap.metrics().entrySet()) {
                metricStrings.put(m.getKey(), formatMetric(m.getValue()));
            }
            sources.put(source, metricStrings);

            // Merge flags from snapshot
            for (var f : snap.flags().entrySet()) {
                allFlags.put(f.getKey(), f.getValue() ? "ACTIVE" : "OK");
            }
        }

        String displayName = categoryKey.substring(0, 1).toUpperCase() + categoryKey.substring(1);
        return new DebugViewSnapshot.DebugCategoryView(displayName, sources, allFlags);
    }

    // --- Alert mapping ---

    private List<DebugViewSnapshot.DebugAlertView> mapAlerts() {
        List<DebugEvent> events = session.recentEvents(50);
        List<DebugViewSnapshot.DebugAlertView> alerts = new ArrayList<>();

        for (var event : events) {
            if (event.severity() == DebugSeverity.WARNING
                    || event.severity() == DebugSeverity.ERROR
                    || event.severity() == DebugSeverity.CRITICAL) {

                String severity = event.severity() == DebugSeverity.WARNING ? "WARNING" : "ERROR";
                alerts.add(new DebugViewSnapshot.DebugAlertView(
                    event.name(),
                    severity,
                    event.message(),
                    "", // metric value not always available from events
                    ""  // threshold not always available from events
                ));
            }
        }

        return alerts;
    }

    // --- Summary mapping ---

    private DebugViewSnapshot.DebugSummaryView mapSummary(
            long frameNumber, Map<String, DebugSnapshot> frameSnapshots) {

        DebugHistory history = session.history();
        int historyDepth = history.size();
        int sourceCount = frameSnapshots.size();

        // Count healthy sources (those that produced a snapshot this frame)
        int healthySources = sourceCount;

        // Estimate frame time from timeline if available
        float frameTimeMs = 0f;
        float budgetPercent = 0f;

        // Try to get frame time from engine snapshot
        DebugSnapshot engineSnap = frameSnapshots.get("worldengine");
        if (engineSnap != null) {
            Double ft = engineSnap.metrics().get("frameTimeMs");
            if (ft != null) {
                frameTimeMs = ft.floatValue();
                // Assume 60Hz target = 16.67ms budget
                budgetPercent = (frameTimeMs / 16.667f) * 100f;
            }
        }

        return new DebugViewSnapshot.DebugSummaryView(
            frameNumber,
            frameTimeMs,
            budgetPercent,
            sourceCount,
            historyDepth,
            healthySources
        );
    }

    // --- Helpers ---

    private Map<String, DebugSnapshot> latestFrameSnapshots() {
        return session.history().latest()
            .map(DebugHistory.FrameRecord::snapshots)
            .orElse(Map.of());
    }

    private static String categoryKeyFor(DebugCategory category) {
        return CATEGORY_KEYS.getOrDefault(category, "custom");
    }

    private static String formatMetric(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}
