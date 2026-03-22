package org.dynamisengine.ui.debug.builder;

import org.dynamisengine.ui.debug.model.DebugMiniTrend;

import java.util.List;
import java.util.Map;

/**
 * Stable data contract between the debug capture pipeline and the UI layer.
 *
 * <p>This is the boundary type that prevents {@code DynamisUI} from depending
 * on internal debug structures. It supports:
 * <ul>
 *   <li>Live overlay rendering</li>
 *   <li>Replay playback</li>
 *   <li>Remote streaming</li>
 *   <li>Serialization for editor tooling</li>
 * </ul>
 *
 * <p>The debug capture pipeline (in {@code DynamisDebug}) produces this;
 * the overlay builder (in {@code DynamisUI}) consumes it.
 *
 * @param categories     per-category snapshots keyed by category name
 * @param alerts         active watchdog alerts
 * @param summary        engine-level summary metrics
 * @param tick           the engine tick this snapshot represents
 * @param timelineEvents recent events for timeline strip rendering
 */
public record DebugViewSnapshot(
    Map<String, DebugCategoryView> categories,
    List<DebugAlertView> alerts,
    DebugSummaryView summary,
    long tick,
    List<DebugTimelineEvent> timelineEvents
) {
    /** Backwards-compatible constructor without timeline events. */
    public DebugViewSnapshot(
            Map<String, DebugCategoryView> categories,
            List<DebugAlertView> alerts,
            DebugSummaryView summary,
            long tick) {
        this(categories, alerts, summary, tick, List.of());
    }

    public DebugViewSnapshot {
        if (categories == null) categories = Map.of();
        else categories = Map.copyOf(categories);
        if (alerts == null) alerts = List.of();
        else alerts = List.copyOf(alerts);
        if (summary == null) summary = DebugSummaryView.EMPTY;
        if (timelineEvents == null) timelineEvents = List.of();
        else timelineEvents = List.copyOf(timelineEvents);
    }

    /** Empty snapshot for initial/absent state. */
    public static final DebugViewSnapshot EMPTY = new DebugViewSnapshot(Map.of(), List.of(), DebugSummaryView.EMPTY, 0, List.of());

    /**
     * A category-level view containing source snapshots and trends.
     *
     * @param categoryName display name (e.g. "PHYSICS", "AI")
     * @param sources      per-source metric maps
     * @param flags        named flags
     * @param trends       mini-trend sparklines for key metrics
     */
    public record DebugCategoryView(
        String categoryName,
        Map<String, Map<String, String>> sources,
        Map<String, String> flags,
        List<DebugMiniTrend> trends
    ) {
        /** Backwards-compatible constructor without trends. */
        public DebugCategoryView(
                String categoryName,
                Map<String, Map<String, String>> sources,
                Map<String, String> flags) {
            this(categoryName, sources, flags, List.of());
        }

        public DebugCategoryView {
            if (categoryName == null) categoryName = "";
            if (sources == null) sources = Map.of();
            if (flags == null) flags = Map.of();
            if (trends == null) trends = List.of();
            else trends = List.copyOf(trends);
        }
    }

    /**
     * A watchdog alert view.
     */
    public record DebugAlertView(
        String ruleName,
        String severity,
        String message,
        String metricValue,
        String threshold
    ) {}

    /**
     * Engine-level summary metrics.
     */
    public record DebugSummaryView(
        long tick,
        float frameTimeMs,
        float budgetPercent,
        int sourceCount,
        int historyDepth,
        int healthySources
    ) {
        public static final DebugSummaryView EMPTY = new DebugSummaryView(0, 0f, 0f, 0, 0, 0);
    }

    /**
     * A timeline event for the bottom timeline strip.
     *
     * @param frameNumber when the event occurred
     * @param timestampMs wall-clock time
     * @param severity    "WARNING", "ERROR", or "CRITICAL"
     * @param source      originating subsystem
     * @param name        event type name
     * @param message     short description
     */
    public record DebugTimelineEvent(
        long frameNumber,
        long timestampMs,
        String severity,
        String source,
        String name,
        String message
    ) {}
}
