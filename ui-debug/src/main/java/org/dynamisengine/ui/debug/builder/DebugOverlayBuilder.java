package org.dynamisengine.ui.debug.builder;

import org.dynamisengine.ui.debug.model.*;
import org.dynamisengine.ui.debug.runtime.DebugOverlayOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a deterministic, ordered list of {@link DebugOverlayPanel} instances
 * from a {@link DebugViewSnapshot}.
 *
 * <p>This is the bridge between the stable debug data contract and the UI model.
 * The builder does not query subsystems directly and does not know rendering details.
 *
 * <p>Panel order follows the canonical roadmap:
 * <ol>
 *   <li>Engine Summary (TOP)</li>
 *   <li>Alerts (TOP)</li>
 *   <li>Physics (GRID)</li>
 *   <li>ECS (GRID)</li>
 *   <li>Rendering (GRID)</li>
 *   <li>Audio (GRID)</li>
 *   <li>AI (GRID)</li>
 *   <li>Scripting (GRID)</li>
 *   <li>Content (GRID)</li>
 *   <li>Input (GRID)</li>
 *   <li>UI (GRID)</li>
 *   <li>Engine (GRID)</li>
 *   <li>Timeline (BOTTOM, optional)</li>
 * </ol>
 */
public final class DebugOverlayBuilder {

    private static final DebugOverlayPanelId ID_SUMMARY   = new DebugOverlayPanelId("engine", "summary");
    private static final DebugOverlayPanelId ID_ALERTS    = new DebugOverlayPanelId("engine", "alerts");
    private static final DebugOverlayPanelId ID_PHYSICS   = new DebugOverlayPanelId("physics", "physics");
    private static final DebugOverlayPanelId ID_ECS       = new DebugOverlayPanelId("ecs", "ecs");
    private static final DebugOverlayPanelId ID_RENDERING = new DebugOverlayPanelId("rendering", "rendering");
    private static final DebugOverlayPanelId ID_AUDIO     = new DebugOverlayPanelId("audio", "audio");
    private static final DebugOverlayPanelId ID_AI        = new DebugOverlayPanelId("ai", "ai");
    private static final DebugOverlayPanelId ID_SCRIPTING = new DebugOverlayPanelId("scripting", "scripting");
    private static final DebugOverlayPanelId ID_CONTENT   = new DebugOverlayPanelId("content", "content");
    private static final DebugOverlayPanelId ID_INPUT     = new DebugOverlayPanelId("input", "input");
    private static final DebugOverlayPanelId ID_UI        = new DebugOverlayPanelId("ui", "ui");
    private static final DebugOverlayPanelId ID_ENGINE    = new DebugOverlayPanelId("engine", "engine");
    private static final DebugOverlayPanelId ID_TIMELINE  = new DebugOverlayPanelId("engine", "timeline");

    private final DebugOverlayOptions options;

    public DebugOverlayBuilder(DebugOverlayOptions options) {
        this.options = options;
    }

    public DebugOverlayBuilder() {
        this(DebugOverlayOptions.DEFAULT);
    }

    /**
     * Builds panels from the given snapshot in canonical order.
     * Returns an empty list for a null or empty snapshot.
     */
    public List<DebugOverlayPanel> buildAll(DebugViewSnapshot snapshot) {
        if (snapshot == null) return List.of();

        List<DebugOverlayPanel> panels = new ArrayList<>();
        int order = 0;

        panels.add(buildSummaryPanel(snapshot, order++));

        if (options.showAlerts()) {
            panels.add(buildAlertPanel(snapshot, order++));
        }

        panels.add(buildCategoryPanel(snapshot, ID_PHYSICS,   "Physics",   "physics",   order++));
        panels.add(buildCategoryPanel(snapshot, ID_ECS,       "ECS",       "ecs",       order++));
        panels.add(buildCategoryPanel(snapshot, ID_RENDERING, "Rendering", "rendering", order++));
        panels.add(buildCategoryPanel(snapshot, ID_AUDIO,     "Audio",     "audio",     order++));
        panels.add(buildCategoryPanel(snapshot, ID_AI,        "AI",        "ai",        order++));
        panels.add(buildCategoryPanel(snapshot, ID_SCRIPTING, "Scripting", "scripting", order++));
        panels.add(buildCategoryPanel(snapshot, ID_CONTENT,   "Content",   "content",   order++));
        panels.add(buildCategoryPanel(snapshot, ID_INPUT,     "Input",     "input",     order++));
        panels.add(buildCategoryPanel(snapshot, ID_UI,        "UI",        "ui",        order++));
        panels.add(buildCategoryPanel(snapshot, ID_ENGINE,    "Engine",    "engine",    order++));

        if (options.showTimeline()) {
            panels.add(buildTimelinePanel(snapshot, order));
        }

        // Enforce budget
        if (panels.size() > options.maxPanels()) {
            panels = new ArrayList<>(panels.subList(0, options.maxPanels()));
        }

        return List.copyOf(panels);
    }

    private DebugOverlayPanel buildSummaryPanel(DebugViewSnapshot snapshot, int order) {
        var summary = snapshot.summary();
        return DebugOverlayPanel.builder(ID_SUMMARY, "Engine Summary")
            .region(PanelRegion.TOP)
            .order(order)
            .row("Tick", String.valueOf(summary.tick()))
            .row("Frame", String.format("%.2f ms", summary.frameTimeMs()))
            .row("Budget", String.format("%.0f%%", summary.budgetPercent()))
            .row("Sources", String.valueOf(summary.sourceCount()))
            .row("History", String.valueOf(summary.historyDepth()))
            .row("Healthy", String.valueOf(summary.healthySources()))
            .build();
    }

    private DebugOverlayPanel buildAlertPanel(DebugViewSnapshot snapshot, int order) {
        var builder = DebugOverlayPanel.builder(ID_ALERTS, "Alerts")
            .region(PanelRegion.TOP)
            .order(order);

        if (snapshot.alerts().isEmpty()) {
            builder.row("status", "No active alerts");
            return builder.build();
        }

        // Group alerts by ruleName
        var grouped = new java.util.LinkedHashMap<String, AlertGroup>();
        for (var alert : snapshot.alerts()) {
            grouped.computeIfAbsent(alert.ruleName(), k -> new AlertGroup(alert))
                   .add(alert);
        }

        // Sort: CRITICAL/ERROR first, then WARNING, then by count descending
        var sorted = new java.util.ArrayList<>(grouped.values());
        sorted.sort((a, b) -> {
            int sevCmp = severityRank(b.severity) - severityRank(a.severity);
            if (sevCmp != 0) return sevCmp;
            return Integer.compare(b.count, a.count);
        });

        // Summary counts
        int criticalCount = 0, errorCount = 0, warningCount = 0;
        for (var g : sorted) {
            switch (g.severity) {
                case "CRITICAL" -> criticalCount += g.count;
                case "ERROR" -> errorCount += g.count;
                default -> warningCount += g.count;
            }
        }
        RowSeverity summaryRowSev = criticalCount > 0 || errorCount > 0 ? RowSeverity.ERROR
            : warningCount > 0 ? RowSeverity.WARNING : RowSeverity.NORMAL;
        builder.row("summary",
            String.format("C:%d  E:%d  W:%d", criticalCount, errorCount, warningCount),
            summaryRowSev);

        // Render grouped rows up to max
        int maxAlertRows = Math.max(1, options.maxRowsPerPanel() - 1); // -1 for summary
        int shown = 0;
        PanelSeverity maxSeverity = PanelSeverity.NORMAL;

        for (var group : sorted) {
            if (shown >= maxAlertRows) break;

            RowSeverity rowSev = "ERROR".equals(group.severity) || "CRITICAL".equals(group.severity)
                ? RowSeverity.ERROR : RowSeverity.WARNING;
            String prefix = rowSev == RowSeverity.ERROR ? "[E]" : "[W]";
            String countSuffix = group.count > 1 ? " x" + group.count : "";
            builder.row(prefix + " " + group.ruleName + countSuffix, group.latestMessage, rowSev);

            if (rowSev == RowSeverity.ERROR) maxSeverity = PanelSeverity.ERROR;
            else if (rowSev == RowSeverity.WARNING && maxSeverity != PanelSeverity.ERROR)
                maxSeverity = PanelSeverity.WARNING;
            shown++;
        }

        int hidden = sorted.size() - shown;
        if (hidden > 0) {
            builder.row("", "+" + hidden + " more alert types");
        }

        builder.severity(maxSeverity);
        builder.highlighted(true);

        return builder.build();
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 3;
            case "ERROR" -> 2;
            case "WARNING" -> 1;
            default -> 0;
        };
    }

    /** Groups repeated alerts by rule name. */
    private static final class AlertGroup {
        final String ruleName;
        final String severity;
        String latestMessage;
        int count;

        AlertGroup(DebugViewSnapshot.DebugAlertView first) {
            this.ruleName = first.ruleName();
            this.severity = first.severity();
            this.latestMessage = first.message();
            this.count = 0;
        }

        void add(DebugViewSnapshot.DebugAlertView alert) {
            count++;
            latestMessage = alert.message(); // latest wins
        }
    }

    private DebugOverlayPanel buildCategoryPanel(DebugViewSnapshot snapshot,
                                                  DebugOverlayPanelId id,
                                                  String title,
                                                  String categoryKey,
                                                  int order) {
        var builder = DebugOverlayPanel.builder(id, title)
            .region(PanelRegion.GRID)
            .order(order);

        var category = snapshot.categories().get(categoryKey);
        if (category != null) {
            int rowCount = 0;
            for (var entry : category.sources().entrySet()) {
                String source = entry.getKey();
                Map<String, String> metrics = entry.getValue();
                for (var metric : metrics.entrySet()) {
                    if (rowCount >= options.maxRowsPerPanel()) break;
                    String label = category.sources().size() > 1
                        ? source + "." + metric.getKey()
                        : metric.getKey();
                    builder.row(label, metric.getValue());
                    rowCount++;
                }
            }

            if (options.showFlags() && !category.flags().isEmpty()) {
                for (var flag : category.flags().entrySet()) {
                    FlagState state = parseFlagState(flag.getValue());
                    builder.flag(flag.getKey(), state);
                }
            }

            // Add trends from real history data
            for (var trend : category.trends()) {
                builder.trend(trend);
            }
        } else {
            builder.row("status", "no data");
        }

        return builder.build();
    }

    private DebugOverlayPanel buildTimelinePanel(DebugViewSnapshot snapshot, int order) {
        var builder = DebugOverlayPanel.builder(ID_TIMELINE, "Timeline")
            .region(PanelRegion.BOTTOM)
            .order(order);

        var events = snapshot.timelineEvents();
        if (events.isEmpty()) {
            builder.row("status", "no recent events");
        } else {
            PanelSeverity maxSev = PanelSeverity.NORMAL;
            int shown = 0;
            // Show most recent events first (list is chronological, reverse for display)
            for (int i = events.size() - 1; i >= 0 && shown < options.maxRowsPerPanel(); i--) {
                var event = events.get(i);
                RowSeverity rowSev = switch (event.severity()) {
                    case "ERROR", "CRITICAL" -> RowSeverity.ERROR;
                    case "WARNING" -> RowSeverity.WARNING;
                    default -> RowSeverity.NORMAL;
                };
                String label = "T" + event.frameNumber() + " " + event.source();
                builder.row(label, event.message(), rowSev);

                if (rowSev == RowSeverity.ERROR && maxSev != PanelSeverity.ERROR) maxSev = PanelSeverity.ERROR;
                else if (rowSev == RowSeverity.WARNING && maxSev == PanelSeverity.NORMAL) maxSev = PanelSeverity.WARNING;
                shown++;
            }
            builder.severity(maxSev);
            builder.row("total", events.size() + " events in window");
        }

        return builder.build();
    }

    private static FlagState parseFlagState(String value) {
        if (value == null) return FlagState.OK;
        return switch (value.toUpperCase()) {
            case "ACTIVE" -> FlagState.ACTIVE;
            case "WARNING" -> FlagState.WARNING;
            case "ERROR" -> FlagState.ERROR;
            default -> FlagState.OK;
        };
    }
}
