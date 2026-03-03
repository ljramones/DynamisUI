package org.dynamisui.api.event;

/**
 * Mouse input event.
 */
public record MouseEvent(
        long tick,
        float x,
        float y,
        MouseButton button,
        MouseAction action) implements UIEvent {

    public enum MouseButton { LEFT, RIGHT, MIDDLE, NONE }
    public enum MouseAction { PRESSED, RELEASED, MOVED, DRAGGED }
}
