package org.dynamisui.api.event;

/**
 * Focus gained or lost by a UI component.
 */
public record FocusEvent(
        long tick,
        FocusAction action) implements UIEvent {

    public enum FocusAction { GAINED, LOST }
}
