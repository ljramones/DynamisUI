package org.dynamisengine.ui.core.layout;

import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.core.UINode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowLayoutTest {

    @Test
    void emptyChildrenDoesNothing() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.HORIZONTAL, 5f);
        layout.layout(Bounds.of(0f, 0f, 200f, 100f), List.of());
        // No exception — pass
    }

    @Test
    void singleChildHorizontalPositionedAtAvailableOrigin() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.HORIZONTAL, 0f);
        UINode child = new UINode("c1", Bounds.of(0f, 0f, 30f, 20f));

        layout.layout(Bounds.of(10f, 5f, 200f, 100f), List.of(child));

        assertEquals(10f, child.bounds().x());
        assertEquals(5f, child.bounds().y());
        assertEquals(30f, child.bounds().width());
        assertEquals(20f, child.bounds().height());
    }

    @Test
    void multipleChildrenHorizontalWithGap() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.HORIZONTAL, 4f);
        UINode a = new UINode("a", Bounds.of(0f, 0f, 20f, 10f));
        UINode b = new UINode("b", Bounds.of(0f, 0f, 30f, 15f));
        UINode c = new UINode("c", Bounds.of(0f, 0f, 10f, 10f));

        layout.layout(Bounds.of(5f, 10f, 200f, 100f), List.of(a, b, c));

        assertEquals(5f, a.bounds().x());
        assertEquals(10f, a.bounds().y());

        assertEquals(5f + 20f + 4f, b.bounds().x());
        assertEquals(10f, b.bounds().y());

        assertEquals(5f + 20f + 4f + 30f + 4f, c.bounds().x());
        assertEquals(10f, c.bounds().y());
    }

    @Test
    void singleChildVerticalPositionedAtAvailableOrigin() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.VERTICAL, 0f);
        UINode child = new UINode("c1", Bounds.of(0f, 0f, 30f, 20f));

        layout.layout(Bounds.of(10f, 5f, 200f, 100f), List.of(child));

        assertEquals(10f, child.bounds().x());
        assertEquals(5f, child.bounds().y());
    }

    @Test
    void multipleChildrenVerticalWithGap() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.VERTICAL, 8f);
        UINode a = new UINode("a", Bounds.of(0f, 0f, 20f, 10f));
        UINode b = new UINode("b", Bounds.of(0f, 0f, 30f, 25f));

        layout.layout(Bounds.of(5f, 10f, 200f, 200f), List.of(a, b));

        assertEquals(5f, a.bounds().x());
        assertEquals(10f, a.bounds().y());

        assertEquals(5f, b.bounds().x());
        assertEquals(10f + 10f + 8f, b.bounds().y());
    }

    @Test
    void zeroGapPlacesChildrenContiguously() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.HORIZONTAL, 0f);
        UINode a = new UINode("a", Bounds.of(0f, 0f, 50f, 10f));
        UINode b = new UINode("b", Bounds.of(0f, 0f, 50f, 10f));

        layout.layout(Bounds.of(0f, 0f, 200f, 100f), List.of(a, b));

        assertEquals(50f, b.bounds().x());
    }

    @Test
    void childrenRetainOwnWidthAndHeight() {
        FlowLayout layout = new FlowLayout(FlowLayout.Direction.HORIZONTAL, 2f);
        UINode child = new UINode("c", Bounds.of(0f, 0f, 77f, 33f));

        layout.layout(Bounds.of(0f, 0f, 200f, 100f), List.of(child));

        assertEquals(77f, child.bounds().width());
        assertEquals(33f, child.bounds().height());
    }
}
