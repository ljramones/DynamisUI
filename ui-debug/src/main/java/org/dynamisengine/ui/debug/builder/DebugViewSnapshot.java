package org.dynamisengine.ui.debug.builder;

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
 * @param categories per-category snapshots keyed by category name
 * @param alerts     active watchdog alerts
 * @param summary    engine-level summary metrics
 * @param tick       the engine tick this snapshot represents
 */
public record DebugViewSnapshot(
    Map<String, DebugCategoryView> categories,
    List<DebugAlertView> alerts,
    DebugSummaryView summary,
    long tick
) {
    public DebugViewSnapshot {
        if (categories == null) categories = Map.of();
        else categories = Map.copyOf(categories);
        if (alerts == null) alerts = List.of();
        else alerts = List.copyOf(alerts);
        if (summary == null) summary = DebugSummaryView.EMPTY;
    }

    /** Empty snapshot for initial/absent state. */
    public static final DebugViewSnapshot EMPTY = new DebugViewSnapshot(Map.of(), List.of(), DebugSummaryView.EMPTY, 0);

    /**
     * A category-level view containing source snapshots.
     *
     * @param categoryName display name (e.g. "PHYSICS", "AI")
     * @param sources      per-source metric maps
     */
    public record DebugCategoryView(
        String categoryName,
        Map<String, Map<String, String>> sources,
        Map<String, String> flags
    ) {
        public DebugCategoryView {
            if (categoryName == null) categoryName = "";
            if (sources == null) sources = Map.of();
            if (flags == null) flags = Map.of();
        }
    }

    /**
     * A watchdog alert view.
     *
     * @param ruleName    the watchdog rule that fired
     * @param severity    "WARNING" or "ERROR"
     * @param message     human-readable alert message
     * @param metricValue the metric value that triggered the alert
     * @param threshold   the threshold that was exceeded
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
     *
     * @param tick           current engine tick
     * @param frameTimeMs    frame time in milliseconds
     * @param budgetPercent  budget usage percentage
     * @param sourceCount    number of registered telemetry sources
     * @param historyDepth   number of frames in history
     * @param healthySources number of healthy sources
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
}
