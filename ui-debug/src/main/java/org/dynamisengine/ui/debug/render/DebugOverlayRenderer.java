package org.dynamisengine.ui.debug.render;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;
import org.dynamisengine.ui.debug.model.DebugFlagView;
import org.dynamisengine.ui.debug.model.DebugMiniTrend;
import org.dynamisengine.ui.debug.model.DebugOverlayPanel;
import org.dynamisengine.ui.debug.model.DebugOverlayRow;

import java.util.List;

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

    /**
     * Render a single panel in fullscreen focus mode with enlarged trends
     * and recent timeline events for that panel's category.
     *
     * @param panel          the focused panel
     * @param screen         full screen bounds
     * @param timelineEvents recent events filtered for this panel's category
     */
    default void renderFocus(DebugOverlayPanel panel, LayoutBox screen,
                              List<DebugViewSnapshot.DebugTimelineEvent> timelineEvents) {
        // Default: fall back to normal panel rendering
        beginOverlay();
        drawPanel(panel, new LayoutBox(screen.x() + 8, screen.y() + 8,
            screen.width() - 16, screen.height() - 16));
        endOverlay();
    }

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
