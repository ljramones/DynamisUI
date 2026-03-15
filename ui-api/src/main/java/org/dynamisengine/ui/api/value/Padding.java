package org.dynamisengine.ui.api.value;

/**
 * Inset padding on each edge (top, right, bottom, left) in pixels.
 */
public record Padding(float top, float right, float bottom, float left) {

    public static final Padding NONE = new Padding(0, 0, 0, 0);

    public static Padding of(float all) {
        return new Padding(all, all, all, all);
    }

    public static Padding of(float vertical, float horizontal) {
        return new Padding(vertical, horizontal, vertical, horizontal);
    }

    public static Padding of(float top, float right, float bottom, float left) {
        return new Padding(top, right, bottom, left);
    }

    public float horizontal() { return left + right; }
    public float vertical()   { return top + bottom; }
}
