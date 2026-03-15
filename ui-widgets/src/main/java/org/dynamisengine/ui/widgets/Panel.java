package org.dynamisengine.ui.widgets;

import org.dynamisengine.ui.api.spi.UIRenderer;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.api.value.Color;
import org.dynamisengine.ui.core.UINode;

/**
 * A filled rectangular panel — the base container widget.
 * Draws a background rect then renders its children.
 */
public class Panel extends UINode {

    private Color backgroundColor;
    private Color borderColor;
    private float cornerRadius;
    private boolean drawBorder;

    public Panel(String id, Bounds bounds) {
        super(id, bounds);
        this.backgroundColor = null; // resolved from theme
        this.cornerRadius = 0f;
        this.drawBorder = false;
    }

    public Panel backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    public Panel borderColor(Color color) {
        this.borderColor = color;
        this.drawBorder = true;
        return this;
    }

    public Panel cornerRadius(float r) {
        this.cornerRadius = r;
        return this;
    }

    @Override
    protected void renderSelf(UIRenderer renderer) {
        Color bg = backgroundColor != null ? backgroundColor
            : (theme() != null ? theme().surface() : Color.ofHex(0x1A1A2EFF));

        Bounds b = bounds();
        if (cornerRadius > 0) {
            renderer.drawRoundRect(b, bg, cornerRadius);
        } else {
            renderer.drawRect(b, bg);
        }

        if (drawBorder) {
            Color bc = borderColor != null ? borderColor
                : (theme() != null ? theme().border() : Color.ofHex(0x2A2A4AFF));
            float bw = theme() != null ? theme().borderWidth() : 1f;
            renderer.drawRectOutline(b, bc, bw);
        }
    }
}
