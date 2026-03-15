package org.dynamisengine.ui.debug;

import org.dynamisengine.core.logging.DynamisLogger;
import org.dynamisengine.ui.api.spi.UIRenderer;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.api.value.Color;
import org.dynamisengine.ui.api.value.FontDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Immediate-mode debug overlay drawn on top of the retained scene graph.
 *
 * Each frame: panels register themselves via addPanel(), the overlay
 * renders all visible panels, then the list is cleared.
 * Zero cost when disabled — all draw calls are skipped.
 *
 * Usage per frame:
 * <pre>
 *   overlay.beginFrame();
 *   overlay.addPanel(aiPanel);
 *   overlay.addPanel(perfPanel);
 *   overlay.render(renderer);
 * </pre>
 */
public final class DebugOverlay {

    private static final DynamisLogger log = DynamisLogger.get(DebugOverlay.class);

    private static final Color OVERLAY_BG   = Color.ofHex(0x000000CC);
    private static final Color OVERLAY_TEXT = Color.ofHex(0x00FF88FF);
    private static final Color OVERLAY_HEADER = Color.ofHex(0xFFAA00FF);
    private static final FontDescriptor FONT = FontDescriptor.MONOSPACE;

    private boolean enabled;
    private final List<DebugPanel> panels = new ArrayList<>();

    public DebugOverlay(boolean enabledByDefault) {
        this.enabled = enabledByDefault;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() { enabled = !enabled; }

    /** Clears panels from the previous frame. Call at the start of each frame. */
    public void beginFrame() {
        panels.clear();
    }

    /** Registers a panel for rendering this frame. */
    public void addPanel(DebugPanel panel) {
        if (panel != null) panels.add(panel);
    }

    /**
     * Renders all registered panels.
     * Call after UIStage.render() and before renderer.endFrame().
     */
    public void render(UIRenderer renderer) {
        if (!enabled || panels.isEmpty()) return;

        float x = 8f;
        float y = 8f;
        float panelWidth = 260f;
        float rowHeight = 16f;
        float padding = 8f;
        float panelGap = 6f;

        for (DebugPanel panel : panels) {
            if (!panel.visible()) continue;

            int rowCount = 1 + panel.rows().size(); // header + rows
            float panelHeight = padding * 2 + rowCount * rowHeight;

            Bounds bg = Bounds.of(x, y, panelWidth, panelHeight);
            renderer.drawRect(bg, OVERLAY_BG);

            // Header
            renderer.drawText(panel.title(), x + padding, y + padding, FONT, OVERLAY_HEADER);

            // Rows
            float rowY = y + padding + rowHeight;
            for (DebugPanel.Row row : panel.rows()) {
                String line = row.label() + ": " + row.value();
                renderer.drawText(line, x + padding, rowY, FONT, OVERLAY_TEXT);
                rowY += rowHeight;
            }

            y += panelHeight + panelGap;
        }
    }
}
