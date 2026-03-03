package org.dynamisui.core;

import org.dynamisui.api.event.UIEvent;

/**
 * Listener for UI events on a UINode.
 */
@FunctionalInterface
public interface UIEventListener {

    /**
     * Called when a UI event reaches this node.
     *
     * @param event the event
     * @return true if the event was consumed (stops further propagation)
     */
    boolean onEvent(UIEvent event);
}
