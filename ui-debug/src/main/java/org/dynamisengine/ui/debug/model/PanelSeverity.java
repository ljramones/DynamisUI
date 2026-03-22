package org.dynamisengine.ui.debug.model;

/**
 * Severity level for an entire debug overlay panel.
 * Determined by propagation: {@code PanelSeverity = max(RowSeverity)}.
 */
public enum PanelSeverity {
    NORMAL,
    WARNING,
    ERROR
}
