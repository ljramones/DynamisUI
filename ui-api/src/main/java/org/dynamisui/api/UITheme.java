package org.dynamisui.api;

import org.dynamisui.api.value.Color;
import org.dynamisui.api.value.FontDescriptor;
import org.dynamisui.api.value.Padding;

/**
 * Provides visual constants for the UI system.
 * Implement to create custom visual themes.
 * The default implementation is DefaultDarkTheme in ui-core.
 */
public interface UITheme {

    // --- Colors ---
    Color background();
    Color backgroundAlt();
    Color surface();
    Color surfaceAlt();
    Color primary();
    Color primaryHover();
    Color primaryActive();
    Color textPrimary();
    Color textSecondary();
    Color textDisabled();
    Color border();
    Color danger();
    Color success();
    Color warning();

    // --- Fonts ---
    FontDescriptor fontBody();
    FontDescriptor fontHeading();
    FontDescriptor fontCaption();
    FontDescriptor fontMono();

    // --- Spacing ---
    float spacingXs();
    float spacingSm();
    float spacingMd();
    float spacingLg();
    float spacingXl();

    // --- Shape ---
    float cornerRadius();
    float borderWidth();

    // --- Padding ---
    default Padding paddingButton() {
        return Padding.of(spacingSm(), spacingMd());
    }

    default Padding paddingPanel() {
        return Padding.of(spacingMd());
    }
}
