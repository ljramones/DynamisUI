package org.dynamisengine.ui.debug.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A rich debug overlay panel with severity, flags, trends, and layout region.
 *
 * <p>This is the production overlay panel type that supports the full
 * debug overlay layout spec: severity propagation, flag display,
 * mini trend strips, and layout region placement.
 *
 * <p>Build via the fluent {@link Builder}.
 */
public final class DebugOverlayPanel {

    private final DebugOverlayPanelId id;
    private final String title;
    private final PanelRegion region;
    private final int order;
    private final PanelSeverity severity;
    private final List<DebugOverlayRow> rows;
    private final List<DebugFlagView> flags;
    private final List<DebugMiniTrend> trends;
    private final boolean highlighted;

    private DebugOverlayPanel(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.region = builder.region;
        this.order = builder.order;
        this.severity = builder.severity != null ? builder.severity : propagateSeverity(builder.rows);
        this.rows = List.copyOf(builder.rows);
        this.flags = List.copyOf(builder.flags);
        this.trends = List.copyOf(builder.trends);
        this.highlighted = builder.highlighted;
    }

    public DebugOverlayPanelId id() { return id; }
    public String title() { return title; }
    public PanelRegion region() { return region; }
    public int order() { return order; }
    public PanelSeverity severity() { return severity; }
    public List<DebugOverlayRow> rows() { return rows; }
    public List<DebugFlagView> flags() { return flags; }
    public List<DebugMiniTrend> trends() { return trends; }
    public boolean highlighted() { return highlighted; }

    /** Severity propagation: panel severity = max(row severity). */
    private static PanelSeverity propagateSeverity(List<DebugOverlayRow> rows) {
        PanelSeverity max = PanelSeverity.NORMAL;
        for (var row : rows) {
            PanelSeverity mapped = switch (row.severity()) {
                case NORMAL -> PanelSeverity.NORMAL;
                case WARNING -> PanelSeverity.WARNING;
                case ERROR -> PanelSeverity.ERROR;
            };
            if (mapped.ordinal() > max.ordinal()) max = mapped;
        }
        return max;
    }

    public static Builder builder(DebugOverlayPanelId id, String title) {
        return new Builder(id, title);
    }

    public static final class Builder {
        private final DebugOverlayPanelId id;
        private final String title;
        private PanelRegion region = PanelRegion.GRID;
        private int order = 0;
        private PanelSeverity severity; // null = auto-propagate
        private final List<DebugOverlayRow> rows = new ArrayList<>();
        private final List<DebugFlagView> flags = new ArrayList<>();
        private final List<DebugMiniTrend> trends = new ArrayList<>();
        private boolean highlighted;

        private Builder(DebugOverlayPanelId id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder region(PanelRegion region) { this.region = region; return this; }
        public Builder order(int order) { this.order = order; return this; }
        public Builder severity(PanelSeverity severity) { this.severity = severity; return this; }
        public Builder highlighted(boolean highlighted) { this.highlighted = highlighted; return this; }

        public Builder row(String label, String value) {
            rows.add(DebugOverlayRow.of(label, value));
            return this;
        }

        public Builder row(String label, String value, RowSeverity severity) {
            rows.add(DebugOverlayRow.of(label, value, severity));
            return this;
        }

        public Builder row(DebugOverlayRow row) {
            rows.add(row);
            return this;
        }

        public Builder flag(String name, FlagState state) {
            flags.add(new DebugFlagView(name, state));
            return this;
        }

        public Builder flag(DebugFlagView flag) {
            flags.add(flag);
            return this;
        }

        public Builder trend(DebugMiniTrend trend) {
            trends.add(trend);
            return this;
        }

        public DebugOverlayPanel build() {
            return new DebugOverlayPanel(this);
        }
    }
}
