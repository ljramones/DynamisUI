package org.dynamisengine.ui.debug.render;

import org.dynamisengine.ui.debug.model.DebugFlagView;
import org.dynamisengine.ui.debug.model.DebugMiniTrend;
import org.dynamisengine.ui.debug.model.DebugOverlayPanel;
import org.dynamisengine.ui.debug.model.DebugOverlayRow;

/**
 * Service Provider Interface for debug overlay rendering.
 *
 * <p>Implementations exist per graphics backend (OpenGL, Vulkan, headless).
 * The overlay runtime calls these methods; it never issues GL/VK calls directly.
 *
 * <p>A {@link LayoutBox} describes the screen-space region assigned to each element
 * by the layout engine.
 */
public interface DebugOverlayRenderer {

    /** Begin a new overlay render pass. Called once per frame. */
    void beginOverlay();

    /** Render a panel background, header, and border with severity coloring. */
    void drawPanel(DebugOverlayPanel panel, LayoutBox box);

    /** Render a single metric row within a panel. */
    void drawRow(DebugOverlayRow row, float x, float y, float width);

    /** Render a flag indicator within a panel. */
    void drawFlag(DebugFlagView flag, float x, float y);

    /** Render a mini trend strip within a panel. */
    void drawTrend(DebugMiniTrend trend, LayoutBox box);

    /** Render arbitrary text at the given position. */
    void drawText(String text, float x, float y, int argbColor);

    /** End the overlay render pass. Called once per frame. */
    void endOverlay();

    /**
     * Screen-space rectangle for layout.
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  width in pixels
     * @param height height in pixels
     */
    record LayoutBox(float x, float y, float width, float height) {}
}
