package org.dynamisengine.ui.debug.model;

/**
 * Layout region for a debug overlay panel.
 * The renderer uses this to position panels without hardcoded layout logic.
 */
public enum PanelRegion {
    /** Top bar: engine summary and alerts. */
    TOP,
    /** Main category grid. */
    GRID,
    /** Bottom strip: timeline, trends. */
    BOTTOM
}
