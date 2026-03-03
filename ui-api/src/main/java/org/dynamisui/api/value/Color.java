package org.dynamisui.api.value;

import org.dynamis.core.exception.DynamisException;

/**
 * RGBA color with float components in [0, 1].
 */
public record Color(float r, float g, float b, float a) {

    public static final Color WHITE       = new Color(1, 1, 1, 1);
    public static final Color BLACK       = new Color(0, 0, 0, 1);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color RED         = new Color(1, 0, 0, 1);
    public static final Color GREEN       = new Color(0, 1, 0, 1);
    public static final Color BLUE        = new Color(0, 0, 1, 1);

    public Color {
        if (r < 0 || r > 1) throw new DynamisException("Color r must be in [0,1]: " + r);
        if (g < 0 || g > 1) throw new DynamisException("Color g must be in [0,1]: " + g);
        if (b < 0 || b > 1) throw new DynamisException("Color b must be in [0,1]: " + b);
        if (a < 0 || a > 1) throw new DynamisException("Color a must be in [0,1]: " + a);
    }

    public static Color of(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }

    public static Color ofRgb(float r, float g, float b) {
        return new Color(r, g, b, 1f);
    }

    /** Parse from 0xRRGGBBAA hex integer. */
    public static Color ofHex(int rgba) {
        return new Color(
            ((rgba >> 24) & 0xFF) / 255f,
            ((rgba >> 16) & 0xFF) / 255f,
            ((rgba >>  8) & 0xFF) / 255f,
            ( rgba        & 0xFF) / 255f);
    }

    public Color withAlpha(float alpha) {
        return new Color(r, g, b, alpha);
    }
}
