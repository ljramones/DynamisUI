package org.dynamisui.api.spi;

import org.dynamisui.api.value.Bounds;
import org.dynamisui.api.value.Color;
import org.dynamisui.api.value.FontDescriptor;

/**
 * Renderer-agnostic drawing contract.
 *
 * Implementations are provided by the game, not by DynamisUI.
 * All coordinates are in screen space pixels, origin top-left.
 *
 * Contract: beginFrame() must be called before any draw call.
 *           endFrame() must be called exactly once per frame after all draw calls.
 *           pushClip/popClip calls must be balanced within a frame.
 */
public interface UIRenderer {

    /** Called once at the start of each frame before any draw calls. */
    void beginFrame(float screenWidth, float screenHeight);

    /** Called once at the end of each frame after all draw calls. */
    void endFrame();

    /** Draws a filled rectangle. */
    void drawRect(Bounds bounds, Color color);

    /** Draws a rectangle outline. */
    void drawRectOutline(Bounds bounds, Color color, float strokeWidth);

    /** Draws a filled rounded rectangle. */
    void drawRoundRect(Bounds bounds, Color color, float cornerRadius);

    /** Draws a single line. */
    void drawLine(float x1, float y1, float x2, float y2, Color color, float strokeWidth);

    /**
     * Draws text. Returns the measured text width in pixels.
     * Clips to bounds if the text exceeds the available width.
     */
    float drawText(String text, float x, float y, FontDescriptor font, Color color);

    /**
     * Measures the width of text without drawing it.
     * Used by layout engines to compute required space.
     */
    float measureText(String text, FontDescriptor font);

    /**
     * Measures the height of a line for the given font.
     */
    float measureLineHeight(FontDescriptor font);

    /**
     * Draws an image by handle. Handle is renderer-specific
     * (e.g. OpenGL texture ID, JavaFX Image reference).
     */
    void drawImage(Object imageHandle, Bounds bounds, float alpha);

    /**
     * Pushes a clipping rectangle. Draw calls outside this region are discarded.
     * Must be paired with popClip().
     */
    void pushClip(Bounds bounds);

    /** Pops the most recently pushed clipping rectangle. */
    void popClip();

    /**
     * Sets global alpha multiplier for all subsequent draw calls.
     * 1.0 = fully opaque, 0.0 = fully transparent.
     */
    void setGlobalAlpha(float alpha);

    /** Resets global alpha to 1.0. */
    default void resetGlobalAlpha() { setGlobalAlpha(1.0f); }
}
