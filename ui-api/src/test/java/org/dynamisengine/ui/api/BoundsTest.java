package org.dynamisengine.ui.api;

import org.dynamisengine.core.exception.DynamisException;
import org.dynamisengine.ui.api.value.Bounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundsTest {

    @Test
    void containsReturnsTrueInsideAndFalseOutside() {
        Bounds bounds = Bounds.of(10f, 20f, 100f, 50f);
        assertTrue(bounds.contains(30f, 40f));
        assertFalse(bounds.contains(200f, 40f));
    }

    @Test
    void rightAndBottomAreComputedCorrectly() {
        Bounds bounds = Bounds.of(2f, 3f, 10f, 20f);
        assertEquals(12f, bounds.right());
        assertEquals(23f, bounds.bottom());
    }

    @Test
    void translateShiftsXAndY() {
        Bounds bounds = Bounds.of(1f, 2f, 3f, 4f).translate(5f, 6f);
        assertEquals(6f, bounds.x());
        assertEquals(8f, bounds.y());
        assertEquals(3f, bounds.width());
        assertEquals(4f, bounds.height());
    }

    @Test
    void insetShrinksBoundsOnAllSides() {
        Bounds bounds = Bounds.of(10f, 10f, 20f, 10f).inset(2f);
        assertEquals(12f, bounds.x());
        assertEquals(12f, bounds.y());
        assertEquals(16f, bounds.width());
        assertEquals(6f, bounds.height());
    }

    @Test
    void negativeWidthThrowsDynamisException() {
        assertThrows(DynamisException.class, () -> Bounds.of(0f, 0f, -1f, 1f));
    }

    @Test
    void negativeHeightThrowsDynamisException() {
        assertThrows(DynamisException.class, () -> Bounds.of(0f, 0f, 1f, -1f));
    }
}
