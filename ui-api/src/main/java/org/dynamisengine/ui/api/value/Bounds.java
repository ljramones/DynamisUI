package org.dynamisengine.ui.api.value;

import org.dynamisengine.core.exception.DynamisException;

/**
 * Axis-aligned rectangular bounds in screen space.
 * Origin is top-left. x and y are in pixels (float for sub-pixel precision).
 */
public record Bounds(float x, float y, float width, float height) {

    public static final Bounds ZERO = new Bounds(0, 0, 0, 0);

    public Bounds {
        if (width < 0) throw new DynamisException("Bounds width must be >= 0, got: " + width);
        if (height < 0) throw new DynamisException("Bounds height must be >= 0, got: " + height);
    }

    public static Bounds of(float x, float y, float width, float height) {
        return new Bounds(x, y, width, height);
    }

    public float right()  { return x + width; }
    public float bottom() { return y + height; }

    public boolean contains(float px, float py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    public Bounds translate(float dx, float dy) {
        return new Bounds(x + dx, y + dy, width, height);
    }

    public Bounds inset(float amount) {
        return new Bounds(x + amount, y + amount,
            Math.max(0, width - amount * 2),
            Math.max(0, height - amount * 2));
    }
}
