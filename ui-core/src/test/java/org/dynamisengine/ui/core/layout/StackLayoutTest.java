package org.dynamisengine.ui.core.layout;

import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.core.UINode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StackLayoutTest {

    @Test
    void emptyChildrenDoesNothing() {
        StackLayout.INSTANCE.layout(Bounds.of(0f, 0f, 100f, 100f), List.of());
        // No exception — pass
    }

    @Test
    void singleChildFillsAvailableBounds() {
        UINode child = new UINode("c1", Bounds.of(0f, 0f, 10f, 10f));
        Bounds available = Bounds.of(5f, 10f, 200f, 150f);

        StackLayout.INSTANCE.layout(available, List.of(child));

        assertEquals(available, child.bounds());
    }

    @Test
    void allChildrenFilledToSameBounds() {
        UINode a = new UINode("a", Bounds.of(0f, 0f, 10f, 10f));
        UINode b = new UINode("b", Bounds.of(0f, 0f, 50f, 50f));
        Bounds available = Bounds.of(0f, 0f, 300f, 200f);

        StackLayout.INSTANCE.layout(available, List.of(a, b));

        assertEquals(available, a.bounds());
        assertEquals(available, b.bounds());
    }
}
