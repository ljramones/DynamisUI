package org.dynamisengine.ui.api.event;

/**
 * Keyboard input event.
 */
public record KeyEvent(
        long tick,
        int keyCode,
        char character,
        KeyAction action,
        boolean shift,
        boolean ctrl,
        boolean alt) implements UIEvent {

    public enum KeyAction { PRESSED, RELEASED, TYPED }
}
