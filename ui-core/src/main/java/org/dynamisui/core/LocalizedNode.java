package org.dynamisui.core;

/**
 * Implemented by UINodes that display localized text.
 * UIStage.rebind() calls onLocaleChanged() on all LocalizedNodes
 * in the scene graph when the active locale switches.
 */
public interface LocalizedNode {

    /**
     * Called after a locale switch.
     * Re-resolve all LocaleKeys and update displayed strings.
     */
    void onLocaleChanged();
}
