package org.dynamisui.demo.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.dynamisui.api.spi.UIRenderer;
import org.dynamisui.api.value.Bounds;
import org.dynamisui.api.value.FontDescriptor;

/**
 * JavaFX Canvas implementation of UIRenderer.
 *
 * Bridges DynamisUI's renderer-agnostic drawing contract
 * to JavaFX GraphicsContext. This is the only class in DynamisUI
 * that imports JavaFX — all other modules are renderer-agnostic.
 */
public final class JavaFxUIRenderer implements UIRenderer {

    private final GraphicsContext gc;
    private float globalAlpha = 1.0f;

    public JavaFxUIRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    @Override
    public void beginFrame(float screenWidth, float screenHeight) {
        gc.clearRect(0, 0, screenWidth, screenHeight);
    }

    @Override
    public void endFrame() {
        // JavaFX renders immediately — no explicit flush needed
    }

    @Override
    public void drawRect(Bounds b, org.dynamisui.api.value.Color c) {
        gc.setFill(toFx(c));
        gc.fillRect(b.x(), b.y(), b.width(), b.height());
    }

    @Override
    public void drawRectOutline(Bounds b, org.dynamisui.api.value.Color c, float stroke) {
        gc.setStroke(toFx(c));
        gc.setLineWidth(stroke);
        gc.strokeRect(b.x(), b.y(), b.width(), b.height());
    }

    @Override
    public void drawRoundRect(Bounds b, org.dynamisui.api.value.Color c, float radius) {
        gc.setFill(toFx(c));
        gc.fillRoundRect(b.x(), b.y(), b.width(), b.height(), radius * 2, radius * 2);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2,
                         org.dynamisui.api.value.Color c, float stroke) {
        gc.setStroke(toFx(c));
        gc.setLineWidth(stroke);
        gc.strokeLine(x1, y1, x2, y2);
    }

    @Override
    public float drawText(String text, float x, float y, FontDescriptor font,
                          org.dynamisui.api.value.Color c) {
        gc.setFont(toFx(font));
        gc.setFill(toFx(c));
        // JavaFX y is baseline — shift down by font size
        gc.fillText(text, x, y + font.size());
        return measureText(text, font);
    }

    @Override
    public float measureText(String text, FontDescriptor font) {
        // Approximation: average character width ≈ 60% of font size
        return text.length() * font.size() * 0.6f;
    }

    @Override
    public float measureLineHeight(FontDescriptor font) {
        return font.size() * 1.4f;
    }

    @Override
    public void drawImage(Object imageHandle, Bounds b, float alpha) {
        if (imageHandle instanceof javafx.scene.image.Image img) {
            gc.setGlobalAlpha(alpha * globalAlpha);
            gc.drawImage(img, b.x(), b.y(), b.width(), b.height());
            gc.setGlobalAlpha(globalAlpha);
        }
    }

    @Override
    public void pushClip(Bounds b) {
        gc.save();
        gc.beginPath();
        gc.rect(b.x(), b.y(), b.width(), b.height());
        gc.clip();
    }

    @Override
    public void popClip() {
        gc.restore();
    }

    @Override
    public void setGlobalAlpha(float alpha) {
        this.globalAlpha = Math.max(0f, Math.min(1f, alpha));
        gc.setGlobalAlpha(globalAlpha);
    }

    // ── Converters ──────────────────────────────────────────────────

    private static Color toFx(org.dynamisui.api.value.Color c) {
        return new Color(c.r(), c.g(), c.b(), c.a());
    }

    private static Font toFx(FontDescriptor fd) {
        FontWeight weight = fd.weight() == FontDescriptor.FontWeight.BOLD
            ? FontWeight.BOLD : FontWeight.NORMAL;
        return Font.font(fd.family(), weight, fd.size());
    }
}
