package org.dynamisengine.ui.core;

import org.dynamisengine.core.exception.DynamisException;
import org.dynamisengine.ui.api.event.FocusEvent;
import org.dynamisengine.ui.api.event.UIEvent;
import org.dynamisengine.ui.api.spi.UIRenderer;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.api.value.Padding;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UINodeTest {

    @Test
    void newNodeIsVisibleByDefault() {
        UINode node = new UINode("root", Bounds.of(0f, 0f, 100f, 100f));
        assertTrue(node.visible());
    }

    @Test
    void setVisibleFalseSkipsRenderClipCalls() {
        UINode node = new UINode("root", Bounds.of(0f, 0f, 100f, 100f));
        node.setVisible(false);
        StubUIRenderer renderer = new StubUIRenderer();

        node.render(renderer);

        assertTrue(renderer.calls.isEmpty());
    }

    @Test
    void addChildAddsToChildrenList() {
        UINode parent = new UINode("parent", Bounds.of(0f, 0f, 100f, 100f));
        UINode child = new UINode("child", Bounds.of(0f, 0f, 10f, 10f));

        parent.addChild(child);

        assertEquals(1, parent.children().size());
        assertTrue(parent.children().contains(child));
    }

    @Test
    void removeChildRemovesFromChildrenList() {
        UINode parent = new UINode("parent", Bounds.of(0f, 0f, 100f, 100f));
        UINode child = new UINode("child", Bounds.of(0f, 0f, 10f, 10f));
        parent.addChild(child);

        parent.removeChild(child);

        assertFalse(parent.children().contains(child));
    }

    @Test
    void dispatchEventPropagatesToChildrenDepthFirst() {
        UINode parent = new UINode("parent", Bounds.of(0f, 0f, 100f, 100f));
        UINode childA = new UINode("childA", Bounds.of(0f, 0f, 10f, 10f));
        UINode childB = new UINode("childB", Bounds.of(0f, 0f, 10f, 10f));
        List<String> order = new ArrayList<>();

        childA.addListener(event -> {
            order.add("childA");
            return false;
        });
        childB.addListener(event -> {
            order.add("childB");
            return false;
        });
        parent.addChild(childA).addChild(childB);

        parent.dispatchEvent(new FocusEvent(1L, FocusEvent.FocusAction.GAINED));

        assertEquals(List.of("childB", "childA"), order);
    }

    @Test
    void eventConsumedByChildStopsPropagationToParentListener() {
        UINode parent = new UINode("parent", Bounds.of(0f, 0f, 100f, 100f));
        UINode child = new UINode("child", Bounds.of(0f, 0f, 10f, 10f));
        List<String> order = new ArrayList<>();

        child.addListener(event -> {
            order.add("child");
            return true;
        });
        parent.addListener(event -> {
            order.add("parent");
            return false;
        });
        parent.addChild(child);

        boolean consumed = parent.dispatchEvent(new FocusEvent(2L, FocusEvent.FocusAction.LOST));

        assertTrue(consumed);
        assertEquals(List.of("child"), order);
    }

    @Test
    void contentBoundsSubtractsPadding() {
        UINode node = new UINode("node", Bounds.of(10f, 20f, 100f, 80f));
        node.setPadding(Padding.of(5f, 10f, 15f, 20f));

        Bounds content = node.contentBounds();

        assertEquals(30f, content.x());
        assertEquals(25f, content.y());
        assertEquals(70f, content.width());
        assertEquals(60f, content.height());
    }

    @Test
    void blankIdThrowsDynamisException() {
        assertThrows(DynamisException.class, () -> new UINode("   ", Bounds.of(0f, 0f, 1f, 1f)));
    }
}
