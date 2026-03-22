package org.dynamisengine.ui.debug.model;

/**
 * A single metric row within a debug overlay panel.
 *
 * @param label    the metric name (e.g. "bodies", "fps")
 * @param value    the formatted value string
 * @param severity row-level severity for highlighting
 */
public record DebugOverlayRow(
    String label,
    String value,
    RowSeverity severity
) {
    public DebugOverlayRow {
        if (label == null) throw new IllegalArgumentException("label required");
        if (value == null) value = "";
        if (severity == null) severity = RowSeverity.NORMAL;
    }

    /** Convenience factory with NORMAL severity. */
    public static DebugOverlayRow of(String label, String value) {
        return new DebugOverlayRow(label, value, RowSeverity.NORMAL);
    }

    /** Convenience factory with explicit severity. */
    public static DebugOverlayRow of(String label, String value, RowSeverity severity) {
        return new DebugOverlayRow(label, value, severity);
    }
}
