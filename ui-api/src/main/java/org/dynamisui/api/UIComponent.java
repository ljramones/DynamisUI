package org.dynamisui.api;

import org.dynamisui.api.event.UIEvent;
import org.dynamisui.api.spi.UIRenderer;
import org.dynamisui.api.value.Bounds;

/**
 * Root of the UI component hierarchy.
 * All DynamisUI components implement this interface.
 *
 * Not sealed — implementations live in ui-core and ui-widgets,
 * which depend on ui-api, not the reverse.
 */
public interface UIComponent {

    /** Stable identifier for this component instance. */
    String id();

    /** Current computed bounds in screen space. */
    Bounds bounds();

    /** Whether this component is visible. Invisible components are not rendered. */
    boolean visible();

    /** Renders this component using the provided renderer. */
    void render(UIRenderer renderer);

    /**
     * Dispatches a UI event to this component.
     * Returns true if the event was consumed (stops propagation).
     */
    boolean dispatchEvent(UIEvent event);

    /** Called each tick to update animations and time-dependent state. */
    void update(long tick);
}
