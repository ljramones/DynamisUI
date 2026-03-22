package org.dynamisengine.ui.debug.model;

import java.util.List;

/**
 * A miniature trend strip for a single metric, displayed within a panel.
 * Contains the last N frame values plus min/max for normalization.
 *
 * @param metricName the metric being trended
 * @param min        minimum value in the window (for normalization)
 * @param max        maximum value in the window (for normalization)
 * @param values     recent values (oldest first)
 */
public record DebugMiniTrend(
    String metricName,
    double min,
    double max,
    List<Double> values
) {
    public DebugMiniTrend {
        if (metricName == null || metricName.isBlank()) throw new IllegalArgumentException("metricName required");
        if (values == null) values = List.of();
        else values = List.copyOf(values);
    }
}
