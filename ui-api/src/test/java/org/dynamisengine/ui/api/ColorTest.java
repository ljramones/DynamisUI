package org.dynamisengine.ui.api;

import org.dynamisengine.core.exception.DynamisException;
import org.dynamisengine.ui.api.value.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColorTest {

    @Test
    void ofHexParsesRedWithFullAlpha() {
        Color color = Color.ofHex(0xFF0000FF);
        assertEquals(1f, color.r());
        assertEquals(0f, color.g());
        assertEquals(0f, color.b());
        assertEquals(1f, color.a());
    }

    @Test
    void withAlphaReturnsSameRgbWithNewAlpha() {
        Color color = Color.ofRgb(0.2f, 0.3f, 0.4f).withAlpha(0.5f);
        assertEquals(0.2f, color.r());
        assertEquals(0.3f, color.g());
        assertEquals(0.4f, color.b());
        assertEquals(0.5f, color.a());
    }

    @Test
    void outOfRangeComponentThrowsDynamisException() {
        assertThrows(DynamisException.class, () -> Color.of(-0.1f, 0f, 0f, 1f));
    }

    @Test
    void constantsHaveExpectedValues() {
        assertEquals(new Color(1f, 1f, 1f, 1f), Color.WHITE);
        assertEquals(new Color(0f, 0f, 0f, 1f), Color.BLACK);
        assertEquals(new Color(0f, 0f, 0f, 0f), Color.TRANSPARENT);
    }
}
