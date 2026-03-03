package org.dynamisui.api.event;

/**
 * Mouse scroll / trackpad scroll event.
 */
public record ScrollEvent(
        long tick,
        float x,
        float y,
        float deltaX,
        float deltaY) implements UIEvent {}
