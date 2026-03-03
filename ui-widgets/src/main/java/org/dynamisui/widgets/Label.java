package org.dynamisui.widgets;

import org.dynamisui.api.spi.UIRenderer;
import org.dynamisui.api.value.Bounds;
import org.dynamisui.api.value.Color;
import org.dynamisui.api.value.FontDescriptor;
import org.dynamisui.core.LocalizedNode;
import org.dynamisui.core.UINode;
import org.dynamislocalization.api.LocalizationService;
import org.dynamislocalization.api.value.LocaleKey;
import org.dynamislocalization.api.value.MissingKeyBehavior;

/**
 * Displays a single line of localized text.
 * Holds a LocaleKey — text is resolved from LocalizationService on render
 * and re-resolved on locale change via LocalizedNode.onLocaleChanged().
 */
public final class Label extends UINode implements LocalizedNode {

    private final LocalizationService loc;
    private LocaleKey localeKey;
    private String resolvedText;
    private Color color;
    private FontDescriptor font;

    public Label(String id, Bounds bounds, LocaleKey localeKey, LocalizationService loc) {
        super(id, bounds);
        this.loc = loc;
        this.localeKey = localeKey;
        this.resolvedText = loc.get(localeKey, MissingKeyBehavior.RETURN_KEY);
        this.color = null;  // resolved from theme on first render
        this.font = null;
    }

    /** Constructor for raw (non-localized) text. Use sparingly — prefer LocaleKey. */
    public Label(String id, Bounds bounds, String rawText) {
        super(id, bounds);
        this.loc = null;
        this.localeKey = null;
        this.resolvedText = rawText;
    }

    @Override
    public void onLocaleChanged() {
        if (loc != null && localeKey != null) {
            resolvedText = loc.get(localeKey, MissingKeyBehavior.RETURN_KEY);
        }
    }

    public void setLocaleKey(LocaleKey key) {
        this.localeKey = key;
        onLocaleChanged();
    }

    public void setText(String rawText) {
        this.localeKey = null;
        this.resolvedText = rawText;
    }

    public void setColor(Color color) { this.color = color; }
    public void setFont(FontDescriptor font) { this.font = font; }

    @Override
    protected void renderSelf(UIRenderer renderer) {
        Color c = color != null ? color
            : (theme() != null ? theme().textPrimary() : Color.WHITE);
        FontDescriptor f = font != null ? font
            : (theme() != null ? theme().fontBody() : FontDescriptor.DEFAULT);
        renderer.drawText(resolvedText, bounds().x(), bounds().y(), f, c);
    }

    public String resolvedText() { return resolvedText; }
}
