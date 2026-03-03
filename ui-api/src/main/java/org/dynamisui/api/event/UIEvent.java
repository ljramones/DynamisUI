package org.dynamisui.api.event;

/**
 * Sealed root of the UI event hierarchy.
 * UI events are internal to DynamisUI — they do not flow through DynamisEvent EventBus.
 */
public sealed interface UIEvent
    permits MouseEvent, KeyEvent, ScrollEvent, FocusEvent {

    /** The simulation tick at which this event was generated. */
    long tick();
}
