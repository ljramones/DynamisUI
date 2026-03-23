package org.dynamisengine.ui.debug.builder;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.core.DebugHistory;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.debug.core.DebugTimeline;
import org.dynamisengine.ui.debug.model.DebugMiniTrend;

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

    /**
     * Key metrics to trend per source. Only metrics that actually exist in
     * history will produce trends — no synthetic data.
     */
    private static final Map<String, List<String>> TREND_METRICS = Map.of(
        "worldengine", List.of("frameTimeMs", "budgetPercent"),
        "physics", List.of("stepTimeMs", "contacts"),
        "ecs", List.of("entityCount"),
        "audio", List.of("dspBudget", "voices"),
        "gpu", List.of("backlog"),
        "lightengine", List.of("drawCalls"),
        "ai", List.of("budgetUsage"),
        "scripting", List.of("commitRate")
    );

    private final DebugSession session;
    private int trendFrameCount = 60;

    public DebugViewSnapshotMapper(DebugSession session) {
        this.session = Objects.requireNonNull(session, "session required");
    }

    public void setTrendFrameCount(int count) {
        this.trendFrameCount = Math.max(2, count);
    }

    /**
     * Maps the current debug state into a {@link DebugViewSnapshot}.
     */
    public DebugViewSnapshot map(long frameNumber) {
        Map<String, DebugSnapshot> frameSnapshots = latestFrameSnapshots();
        return buildSnapshot(frameNumber, frameSnapshots);
    }

    /**
     * Maps from an explicit frame snapshot map.
     */
    public DebugViewSnapshot mapFromFrame(long frameNumber, Map<String, DebugSnapshot> frameSnapshots) {
        return buildSnapshot(frameNumber, frameSnapshots);
    }

    /**
     * Maps a specific historical frame by frame number.
     * Returns {@link DebugViewSnapshot#EMPTY} if the frame is not retained in history.
     */
    public DebugViewSnapshot mapHistoricalFrame(long frameNumber) {
        return session.history().frame(frameNumber)
            .map(record -> buildSnapshot(record.frameNumber(), record.snapshots()))
            .orElse(DebugViewSnapshot.EMPTY);
    }

    private DebugViewSnapshot buildSnapshot(long frameNumber, Map<String, DebugSnapshot> frameSnapshots) {
        return new DebugViewSnapshot(
            mapCategories(frameSnapshots),
            mapAlerts(),
            mapSummary(frameNumber, frameSnapshots),
            frameNumber,
            mapTimelineEvents()
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

        Map<String, Map<String, String>> sources = new LinkedHashMap<>();
        Map<String, String> allFlags = new LinkedHashMap<>();

        for (var entry : snapshots.entrySet()) {
            String source = entry.getKey();
            DebugSnapshot snap = entry.getValue();

            Map<String, String> metricStrings = new LinkedHashMap<>();
            for (var m : snap.metrics().entrySet()) {
                metricStrings.put(m.getKey(), formatMetric(m.getValue()));
            }
            sources.put(source, metricStrings);

            for (var f : snap.flags().entrySet()) {
                allFlags.put(f.getKey(), f.getValue() ? "ACTIVE" : "OK");
            }
        }

        // Extract trends for sources in this category
        List<DebugMiniTrend> trends = extractTrendsForCategory(snapshots);

        String displayName = categoryKey.substring(0, 1).toUpperCase() + categoryKey.substring(1);
        return new DebugViewSnapshot.DebugCategoryView(displayName, sources, allFlags, trends);
    }

    // --- Trend extraction ---

    private List<DebugMiniTrend> extractTrendsForCategory(Map<String, DebugSnapshot> snapshots) {
        if (session.history().size() < 2) return List.of();

        DebugTimeline timeline = session.timeline();
        List<DebugMiniTrend> trends = new ArrayList<>();

        for (var entry : snapshots.entrySet()) {
            String source = entry.getKey();
            List<String> metricsToTrend = TREND_METRICS.getOrDefault(source, List.of());

            for (String metricName : metricsToTrend) {
                var points = timeline.extractMetric(source, metricName, trendFrameCount);
                if (points.size() < 2) continue;

                var stats = timeline.stats(source, metricName, trendFrameCount);
                List<Double> values = new ArrayList<>(points.size());
                for (var p : points) {
                    values.add(p.value());
                }

                trends.add(new DebugMiniTrend(
                    source + "." + metricName,
                    stats.min(),
                    stats.max(),
                    values
                ));
            }
        }

        return trends;
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
                    event.name(), severity, event.message(), "", ""
                ));
            }
        }

        return alerts;
    }

    // --- Timeline event mapping ---

    private List<DebugViewSnapshot.DebugTimelineEvent> mapTimelineEvents() {
        List<DebugEvent> events = session.recentEvents(100);
        List<DebugViewSnapshot.DebugTimelineEvent> timelineEvents = new ArrayList<>();

        for (var event : events) {
            if (event.severity() == DebugSeverity.WARNING
                    || event.severity() == DebugSeverity.ERROR
                    || event.severity() == DebugSeverity.CRITICAL) {

                String severity = switch (event.severity()) {
                    case CRITICAL -> "CRITICAL";
                    case ERROR -> "ERROR";
                    default -> "WARNING";
                };

                timelineEvents.add(new DebugViewSnapshot.DebugTimelineEvent(
                    event.frameNumber(),
                    event.timestampMs(),
                    severity,
                    event.source(),
                    event.name(),
                    event.message()
                ));
            }
        }

        return timelineEvents;
    }

    // --- Summary mapping ---

    private DebugViewSnapshot.DebugSummaryView mapSummary(
            long frameNumber, Map<String, DebugSnapshot> frameSnapshots) {

        DebugHistory history = session.history();
        int historyDepth = history.size();
        int sourceCount = frameSnapshots.size();
        int healthySources = sourceCount;

        float frameTimeMs = 0f;
        float budgetPercent = 0f;

        DebugSnapshot engineSnap = frameSnapshots.get("worldengine");
        if (engineSnap != null) {
            Double ft = engineSnap.metrics().get("frameTimeMs");
            if (ft != null) {
                frameTimeMs = ft.floatValue();
                budgetPercent = (frameTimeMs / 16.667f) * 100f;
            }
        }

        return new DebugViewSnapshot.DebugSummaryView(
            frameNumber, frameTimeMs, budgetPercent,
            sourceCount, historyDepth, healthySources
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
