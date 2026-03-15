package org.dynamisengine.ui.core;

import org.dynamisengine.ui.api.spi.UIRenderer;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.api.value.Color;
import org.dynamisengine.ui.api.value.FontDescriptor;

import java.util.ArrayList;
import java.util.List;

final class StubUIRenderer implements UIRenderer {

    final List<String> calls = new ArrayList<>();

    @Override
    public void beginFrame(float screenWidth, float screenHeight) {
        calls.add("beginFrame");
    }

    @Override
    public void endFrame() {
        calls.add("endFrame");
    }

    @Override
    public void drawRect(Bounds bounds, Color color) {
        calls.add("drawRect");
    }

    @Override
    public void drawRectOutline(Bounds bounds, Color color, float strokeWidth) {
        calls.add("drawRectOutline");
    }

    @Override
    public void drawRoundRect(Bounds bounds, Color color, float cornerRadius) {
        calls.add("drawRoundRect");
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, Color color, float strokeWidth) {
        calls.add("drawLine");
    }

    @Override
    public float drawText(String text, float x, float y, FontDescriptor font, Color color) {
        calls.add("drawText");
        return 0;
    }

    @Override
    public float measureText(String text, FontDescriptor font) {
        calls.add("measureText");
        return 0;
    }

    @Override
    public float measureLineHeight(FontDescriptor font) {
        calls.add("measureLineHeight");
        return 0;
    }

    @Override
    public void drawImage(Object imageHandle, Bounds bounds, float alpha) {
        calls.add("drawImage");
    }

    @Override
    public void pushClip(Bounds bounds) {
        calls.add("pushClip");
    }

    @Override
    public void popClip() {
        calls.add("popClip");
    }

    @Override
    public void setGlobalAlpha(float alpha) {
        calls.add("setGlobalAlpha");
    }
}
