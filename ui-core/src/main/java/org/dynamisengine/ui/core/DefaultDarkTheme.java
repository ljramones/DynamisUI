package org.dynamisengine.ui.core;

import org.dynamisengine.ui.api.UITheme;
import org.dynamisengine.ui.api.value.Color;
import org.dynamisengine.ui.api.value.FontDescriptor;

/**
 * Default dark theme for DynamisUI.
 * A clean, game-appropriate dark palette with readable contrast ratios.
 */
public final class DefaultDarkTheme implements UITheme {

    public static final DefaultDarkTheme INSTANCE = new DefaultDarkTheme();

    private DefaultDarkTheme() {}

    @Override public Color background()     { return Color.ofHex(0x1A1A2EFF); }
    @Override public Color backgroundAlt()  { return Color.ofHex(0x16213EFF); }
    @Override public Color surface()        { return Color.ofHex(0x0F3460FF); }
    @Override public Color surfaceAlt()     { return Color.ofHex(0x1A4A7AFF); }
    @Override public Color primary()        { return Color.ofHex(0xE94560FF); }
    @Override public Color primaryHover()   { return Color.ofHex(0xFF6B81FF); }
    @Override public Color primaryActive()  { return Color.ofHex(0xC73652FF); }
    @Override public Color textPrimary()    { return Color.ofHex(0xEEEEEEFF); }
    @Override public Color textSecondary()  { return Color.ofHex(0xA0A0B0FF); }
    @Override public Color textDisabled()   { return Color.ofHex(0x606070FF); }
    @Override public Color border()         { return Color.ofHex(0x2A2A4AFF); }
    @Override public Color danger()         { return Color.ofHex(0xFF4444FF); }
    @Override public Color success()        { return Color.ofHex(0x44FF88FF); }
    @Override public Color warning()        { return Color.ofHex(0xFFAA00FF); }

    @Override public FontDescriptor fontBody()    { return FontDescriptor.DEFAULT; }
    @Override public FontDescriptor fontHeading() { return FontDescriptor.HEADING; }
    @Override public FontDescriptor fontCaption() { return new FontDescriptor("sans-serif", 11f, FontDescriptor.FontWeight.NORMAL); }
    @Override public FontDescriptor fontMono()    { return FontDescriptor.MONOSPACE; }

    @Override public float spacingXs() { return 4f; }
    @Override public float spacingSm() { return 8f; }
    @Override public float spacingMd() { return 16f; }
    @Override public float spacingLg() { return 24f; }
    @Override public float spacingXl() { return 40f; }
    @Override public float cornerRadius() { return 6f; }
    @Override public float borderWidth()  { return 1f; }
}
