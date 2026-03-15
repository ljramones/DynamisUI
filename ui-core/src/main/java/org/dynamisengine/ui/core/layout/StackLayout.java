package org.dynamisengine.ui.core.layout;

import org.dynamisengine.ui.api.UIComponent;
import org.dynamisengine.ui.api.layout.UILayout;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.core.UINode;

import java.util.List;

/**
 * Stacks all children to fill the available bounds.
 * Each child is sized to the full available area (z-order layering).
 */
public final class StackLayout implements UILayout {

    public static final StackLayout INSTANCE = new StackLayout();

    @Override
    public void layout(Bounds available, List<? extends UIComponent> children) {
        for (UIComponent child : children) {
            if (child instanceof UINode node) {
                node.setBounds(available);
            }
        }
    }
}
