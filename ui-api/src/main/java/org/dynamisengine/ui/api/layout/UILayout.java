package org.dynamisengine.ui.api.layout;

import org.dynamisengine.ui.api.UIComponent;
import org.dynamisengine.ui.api.value.Bounds;

import java.util.List;

/**
 * Computes child positions within available bounds.
 * Stateless — may be shared across nodes.
 */
@FunctionalInterface
public interface UILayout {

    /**
     * Arranges children within the available bounds.
     * Implementations set each child's bounds by calling
     * the component's internal layout method.
     *
     * @param available the space available for children
     * @param children  the components to lay out
     */
    void layout(Bounds available, List<? extends UIComponent> children);
}
