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

    @Test
    void zeroSizeBoundsIsValid() {
        Bounds zero = Bounds.of(5f, 10f, 0f, 0f);
        assertEquals(0f, zero.width());
        assertEquals(0f, zero.height());
        assertEquals(5f, zero.right());
        assertEquals(10f, zero.bottom());
    }

    @Test
    void containsOnBoundaryReturnsTrue() {
        Bounds bounds = Bounds.of(10f, 20f, 100f, 50f);
        // corners
        assertTrue(bounds.contains(10f, 20f));        // top-left
        assertTrue(bounds.contains(110f, 20f));       // top-right
        assertTrue(bounds.contains(10f, 70f));        // bottom-left
        assertTrue(bounds.contains(110f, 70f));       // bottom-right
        // edges
        assertTrue(bounds.contains(50f, 20f));        // top edge
        assertTrue(bounds.contains(10f, 40f));        // left edge
    }

    @Test
    void containsJustOutsideBoundaryReturnsFalse() {
        Bounds bounds = Bounds.of(10f, 20f, 100f, 50f);
        assertFalse(bounds.contains(9.99f, 30f));     // left of left edge
        assertFalse(bounds.contains(110.01f, 30f));   // right of right edge
        assertFalse(bounds.contains(50f, 19.99f));    // above top edge
        assertFalse(bounds.contains(50f, 70.01f));    // below bottom edge
    }

    @Test
    void zeroSizeBoundsContainsItsOwnPoint() {
        Bounds zero = Bounds.of(5f, 10f, 0f, 0f);
        assertTrue(zero.contains(5f, 10f));
        assertFalse(zero.contains(5.01f, 10f));
    }

    @Test
    void insetBeyondSizeClampsToZero() {
        Bounds small = Bounds.of(0f, 0f, 4f, 2f);
        Bounds inset = small.inset(5f);
        assertEquals(0f, inset.width());
        assertEquals(0f, inset.height());
    }

    @Test
    void zeroBoundsConstant() {
        assertEquals(0f, Bounds.ZERO.x());
        assertEquals(0f, Bounds.ZERO.y());
        assertEquals(0f, Bounds.ZERO.width());
        assertEquals(0f, Bounds.ZERO.height());
    }
}
