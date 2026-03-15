package org.dynamisengine.ui.api;

import org.dynamisengine.ui.api.value.Padding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaddingTest {

    @Test
    void ofSingleSetsAllSides() {
        Padding padding = Padding.of(10f);
        assertEquals(10f, padding.top());
        assertEquals(10f, padding.right());
        assertEquals(10f, padding.bottom());
        assertEquals(10f, padding.left());
    }

    @Test
    void ofVerticalHorizontalSetsExpectedSides() {
        Padding padding = Padding.of(5f, 10f);
        assertEquals(5f, padding.top());
        assertEquals(10f, padding.right());
        assertEquals(5f, padding.bottom());
        assertEquals(10f, padding.left());
    }

    @Test
    void horizontalReturnsLeftPlusRight() {
        Padding padding = Padding.of(1f, 2f, 3f, 4f);
        assertEquals(6f, padding.horizontal());
    }

    @Test
    void verticalReturnsTopPlusBottom() {
        Padding padding = Padding.of(1f, 2f, 3f, 4f);
        assertEquals(4f, padding.vertical());
    }
}
