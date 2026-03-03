package org.dynamisui.core.layout;

import org.dynamisui.api.UIComponent;
import org.dynamisui.api.layout.UILayout;
import org.dynamisui.api.value.Bounds;
import org.dynamisui.core.UINode;

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
