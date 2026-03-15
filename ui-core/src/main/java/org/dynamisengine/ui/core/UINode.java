package org.dynamisengine.ui.core;

import org.dynamisengine.core.exception.DynamisException;
import org.dynamisengine.core.logging.DynamisLogger;
import org.dynamisengine.ui.api.UIComponent;
import org.dynamisengine.ui.api.UITheme;
import org.dynamisengine.ui.api.event.UIEvent;
import org.dynamisengine.ui.api.layout.UILayout;
import org.dynamisengine.ui.api.spi.UIRenderer;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.api.value.Padding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base retained-mode scene graph node.
 *
 * UINode is the concrete base class for all DynamisUI components.
 * It manages children, layout, visibility, padding, and event routing.
 *
 * Subclass and override {@link #renderSelf(UIRenderer)} to draw the node's
 * own content. Children are rendered automatically after renderSelf().
 */
public class UINode implements UIComponent {

    private static final DynamisLogger log = DynamisLogger.get(UINode.class);

    private final String id;
    private Bounds bounds;
    private Padding padding;
    private boolean visible;
    private UILayout layout;
    private UITheme theme;

    private final List<UINode> children = new ArrayList<>();
    private final List<UIEventListener> listeners = new CopyOnWriteArrayList<>();

    public UINode(String id, Bounds bounds) {
        if (id == null || id.isBlank()) {
            throw new DynamisException("UINode id must not be null or blank");
        }
        this.id = id;
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.padding = Padding.NONE;
        this.visible = true;
    }

    // ── UIComponent contract ──────────────────────────────────────────

    @Override
    public String id() { return id; }

    @Override
    public Bounds bounds() { return bounds; }

    @Override
    public boolean visible() { return visible; }

    @Override
    public final void render(UIRenderer renderer) {
        if (!visible) return;
        renderer.pushClip(bounds);
        renderSelf(renderer);
        for (UINode child : children) {
            child.render(renderer);
        }
        renderer.popClip();
    }

    @Override
    public boolean dispatchEvent(UIEvent event) {
        if (!visible) return false;
        // Depth-first: children get first chance
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).dispatchEvent(event)) return true;
        }
        return notifyListeners(event);
    }

    @Override
    public void update(long tick) {
        for (UINode child : children) {
            child.update(tick);
        }
    }

    // ── Scene graph ───────────────────────────────────────────────────

    public UINode addChild(UINode child) {
        Objects.requireNonNull(child, "child");
        children.add(child);
        if (layout != null) {
            layout.layout(contentBounds(), Collections.unmodifiableList(children));
        }
        return this;
    }

    public UINode removeChild(UINode child) {
        children.remove(child);
        return this;
    }

    public List<UINode> children() {
        return Collections.unmodifiableList(children);
    }

    // ── Layout ───────────────────────────────────────────────────────

    public void setBounds(Bounds bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        relayout();
    }

    public void setPadding(Padding padding) {
        this.padding = Objects.requireNonNull(padding, "padding");
        relayout();
    }

    public void setLayout(UILayout layout) {
        this.layout = layout;
        relayout();
    }

    public void setTheme(UITheme theme) {
        this.theme = theme;
        for (UINode child : children) {
            child.setTheme(theme);
        }
    }

    public UITheme theme() { return theme; }

    /** The bounds available for children after applying padding. */
    public Bounds contentBounds() {
        return new Bounds(
            bounds.x() + padding.left(),
            bounds.y() + padding.top(),
            Math.max(0, bounds.width() - padding.horizontal()),
            Math.max(0, bounds.height() - padding.vertical()));
    }

    public void relayout() {
        if (layout != null && !children.isEmpty()) {
            layout.layout(contentBounds(), Collections.unmodifiableList(children));
        }
    }

    // ── Visibility ───────────────────────────────────────────────────

    public void setVisible(boolean visible) { this.visible = visible; }

    // ── Event listeners ──────────────────────────────────────────────

    public UINode addListener(UIEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    public UINode removeListener(UIEventListener listener) {
        listeners.remove(listener);
        return this;
    }

    private boolean notifyListeners(UIEvent event) {
        for (UIEventListener listener : listeners) {
            if (listener.onEvent(event)) return true;
        }
        return false;
    }

    // ── Subclass hook ────────────────────────────────────────────────

    /**
     * Override to draw this node's own content.
     * Called before children are rendered.
     * Clipping to this node's bounds is already active.
     */
    protected void renderSelf(UIRenderer renderer) {
        // Default: draw nothing. Subclasses override.
    }
}
