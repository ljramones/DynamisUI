package org.dynamisui.widgets;

import org.dynamisui.api.spi.UIRenderer;
import org.dynamisui.api.value.Bounds;
import org.dynamisui.api.value.Color;
import org.dynamisui.core.UINode;

/**
 * Horizontal progress bar. Used for health, stamina, XP bars in the HUD.
 *
 * value: 0.0 (empty) to 1.0 (full)
 */
public final class ProgressBar extends UINode {

    private float value;
    private Color fillColor;
    private Color trackColor;
    private float cornerRadius;

    public ProgressBar(String id, Bounds bounds, float initialValue) {
        super(id, bounds);
        this.value = Math.max(0f, Math.min(1f, initialValue));
        this.cornerRadius = 4f;
    }

    public void setValue(float value) {
        this.value = Math.max(0f, Math.min(1f, value));
    }

    public float getValue() { return value; }

    public void setFillColor(Color color) { this.fillColor = color; }
    public void setTrackColor(Color color) { this.trackColor = color; }
    public void setCornerRadius(float r) { this.cornerRadius = r; }

    @Override
    protected void renderSelf(UIRenderer renderer) {
        Bounds b = bounds();

        Color track = trackColor != null ? trackColor : Color.ofHex(0x2A2A4AFF);
        Color fill  = fillColor  != null ? fillColor  : Color.ofHex(0xE94560FF);

        // Track
        renderer.drawRoundRect(b, track, cornerRadius);

        // Fill
        float fillWidth = b.width() * value;
        if (fillWidth > 0) {
            renderer.drawRoundRect(
                Bounds.of(b.x(), b.y(), fillWidth, b.height()),
                fill, cornerRadius);
        }
    }
}
