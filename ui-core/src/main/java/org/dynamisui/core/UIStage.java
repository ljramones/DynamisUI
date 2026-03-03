package org.dynamisui.core;

import org.dynamis.core.exception.DynamisException;
import org.dynamis.core.logging.DynamisLogger;
import org.dynamisui.api.UITheme;
import org.dynamisui.api.event.UIEvent;
import org.dynamisui.api.spi.UIRenderer;
import org.dynamisui.api.value.Bounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Root of the UI scene graph.
 *
 * UIStage owns the top-level layer stack and drives rendering and
 * event dispatch each frame. The game loop calls:
 * <pre>
 *   stage.update(tick);
 *   stage.render(renderer);
 * </pre>
 *
 * Layers are rendered in order (index 0 = bottom, last = top).
 * Events are dispatched top-to-bottom (last layer first).
 */
public final class UIStage {

    private static final DynamisLogger log = DynamisLogger.get(UIStage.class);

    private float screenWidth;
    private float screenHeight;
    private UITheme theme;
    private final List<UINode> layers = new ArrayList<>();

    public UIStage(float screenWidth, float screenHeight, UITheme theme) {
        if (screenWidth <= 0) throw new DynamisException("screenWidth must be > 0");
        if (screenHeight <= 0) throw new DynamisException("screenHeight must be > 0");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    /** Adds a layer on top of the stack. */
    public void addLayer(UINode layer) {
        Objects.requireNonNull(layer, "layer");
        layer.setTheme(theme);
        layers.add(layer);
        log.debug(String.format("Layer added: %s (total=%d)", layer.id(), layers.size()));
    }

    /** Removes a layer by id. */
    public void removeLayer(String id) {
        layers.removeIf(n -> n.id().equals(id));
    }

    /** Returns all layers (unmodifiable). */
    public List<UINode> layers() {
        return Collections.unmodifiableList(layers);
    }

    /** Updates all layers for the current tick. */
    public void update(long tick) {
        for (UINode layer : layers) {
            layer.update(tick);
        }
    }

    /**
     * Renders all visible layers bottom-to-top.
     * Caller must have already called renderer.beginFrame().
     */
    public void render(UIRenderer renderer) {
        for (UINode layer : layers) {
            layer.render(renderer);
        }
    }

    /**
     * Dispatches a UI event top-to-bottom through layers.
     * Returns true if any layer consumed the event.
     */
    public boolean dispatchEvent(UIEvent event) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (layers.get(i).dispatchEvent(event)) return true;
        }
        return false;
    }

    /**
     * Notifies all nodes that the locale has changed.
     * Nodes that display localized text should override onLocaleChanged().
     */
    public void rebind() {
        for (UINode layer : layers) {
            rebindNode(layer);
        }
        log.debug("UIStage.rebind() complete");
    }

    private void rebindNode(UINode node) {
        if (node instanceof LocalizedNode ln) {
            ln.onLocaleChanged();
        }
        for (UINode child : node.children()) {
            rebindNode(child);
        }
    }

    /** Resizes the stage. Call when the game window is resized. */
    public void resize(float width, float height) {
        if (width <= 0 || height <= 0) {
            throw new DynamisException("Stage dimensions must be > 0");
        }
        this.screenWidth = width;
        this.screenHeight = height;
        Bounds fullscreen = Bounds.of(0, 0, width, height);
        for (UINode layer : layers) {
            layer.setBounds(fullscreen);
        }
        log.debug(String.format("UIStage resized: %.0fx%.0f", width, height));
    }

    public float screenWidth() { return screenWidth; }
    public float screenHeight() { return screenHeight; }
    public UITheme theme() { return theme; }

    public Bounds fullscreenBounds() {
        return Bounds.of(0, 0, screenWidth, screenHeight);
    }
}
