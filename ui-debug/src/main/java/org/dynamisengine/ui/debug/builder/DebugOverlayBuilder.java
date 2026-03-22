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

        PanelSeverity maxSeverity = PanelSeverity.NORMAL;
        int rowCount = 0;

        for (var alert : snapshot.alerts()) {
            if (rowCount >= options.maxRowsPerPanel()) break;

            RowSeverity rowSev = "ERROR".equals(alert.severity()) ? RowSeverity.ERROR : RowSeverity.WARNING;
            String prefix = "ERROR".equals(alert.severity()) ? "[E]" : "[W]";
            builder.row(prefix + " " + alert.ruleName(), alert.message(), rowSev);

            if (rowSev == RowSeverity.ERROR) maxSeverity = PanelSeverity.ERROR;
            else if (rowSev == RowSeverity.WARNING && maxSeverity != PanelSeverity.ERROR) maxSeverity = PanelSeverity.WARNING;

            rowCount++;
        }

        if (snapshot.alerts().isEmpty()) {
            builder.row("status", "No active alerts");
        }

        builder.severity(maxSeverity);
        builder.highlighted(!snapshot.alerts().isEmpty());

        return builder.build();
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
        } else {
            builder.row("status", "no data");
        }

        return builder.build();
    }

    private DebugOverlayPanel buildTimelinePanel(DebugViewSnapshot snapshot, int order) {
        return DebugOverlayPanel.builder(ID_TIMELINE, "Timeline")
            .region(PanelRegion.BOTTOM)
            .order(order)
            .row("status", "timeline data pending")
            .build();
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
