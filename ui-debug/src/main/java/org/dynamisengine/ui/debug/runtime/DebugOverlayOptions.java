package org.dynamisengine.ui.debug.runtime;

/**
 * Configuration for the debug overlay builder and renderer.
 *
 * @param showTimeline       whether to include the timeline strip panel
 * @param showFlags          whether to include flag displays in panels
 * @param showAlerts         whether to include the alert panel
 * @param trendFrameCount    number of frames for mini trend strips
 * @param maxPanels          maximum panels to render (budget protection)
 * @param maxRowsPerPanel    maximum rows per panel (density protection)
 * @param throttleHeavyPanels whether to skip expensive panels under pressure
 */
public record DebugOverlayOptions(
    boolean showTimeline,
    boolean showFlags,
    boolean showAlerts,
    int trendFrameCount,
    int maxPanels,
    int maxRowsPerPanel,
    boolean throttleHeavyPanels
) {
    /** Sensible defaults for development use. */
    public static final DebugOverlayOptions DEFAULT = new DebugOverlayOptions(
        false,  // timeline off by default
        true,   // flags on
        true,   // alerts on
        60,     // 60 frame trend window
        16,     // max 16 panels
        8,      // max 8 rows per panel
        false   // no throttling by default
    );

    /** Minimal overlay for performance-sensitive contexts. */
    public static final DebugOverlayOptions MINIMAL = new DebugOverlayOptions(
        false, false, true, 0, 4, 4, true
    );
}
