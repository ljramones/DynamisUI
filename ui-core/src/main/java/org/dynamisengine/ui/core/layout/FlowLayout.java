package org.dynamisengine.ui.core.layout;

import org.dynamisengine.ui.api.UIComponent;
import org.dynamisengine.ui.api.layout.UILayout;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.core.UINode;

import java.util.List;

/**
 * Arranges children in a horizontal or vertical flow.
 * Children retain their own width and height; only position is set.
 */
public final class FlowLayout implements UILayout {

    public enum Direction { HORIZONTAL, VERTICAL }

    private final Direction direction;
    private final float gap;

    public FlowLayout(Direction direction, float gap) {
        this.direction = direction;
        this.gap = gap;
    }

    @Override
    public void layout(Bounds available, List<? extends UIComponent> children) {
        float cursor = direction == Direction.HORIZONTAL ? available.x() : available.y();
        for (UIComponent child : children) {
            if (!(child instanceof UINode node)) continue;
            Bounds cb = node.bounds();
            if (direction == Direction.HORIZONTAL) {
                node.setBounds(Bounds.of(cursor, available.y(), cb.width(), cb.height()));
                cursor += cb.width() + gap;
            } else {
                node.setBounds(Bounds.of(available.x(), cursor, cb.width(), cb.height()));
                cursor += cb.height() + gap;
            }
        }
    }
}
