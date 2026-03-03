package org.dynamisui.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * A named panel of key-value rows for display in the DebugOverlay.
 *
 * Build per-frame and pass to DebugOverlay.addPanel().
 * Immutable once built via the builder.
 */
public final class DebugPanel {

    public record Row(String label, String value) {}

    private final String title;
    private final List<Row> rows;
    private final boolean visible;

    private DebugPanel(Builder builder) {
        this.title = builder.title;
        this.rows = List.copyOf(builder.rows);
        this.visible = builder.visible;
    }

    public String title() { return title; }
    public List<Row> rows() { return rows; }
    public boolean visible() { return visible; }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final String title;
        private final List<Row> rows = new ArrayList<>();
        private boolean visible = true;

        private Builder(String title) {
            this.title = title;
        }

        public Builder row(String label, String value) {
            rows.add(new Row(label, value));
            return this;
        }

        public Builder row(String label, Object value) {
            return row(label, String.valueOf(value));
        }

        public Builder row(String label, float value, String unit) {
            return row(label, String.format("%.2f %s", value, unit));
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public DebugPanel build() {
            return new DebugPanel(this);
        }
    }
}
