package org.dynamisui.widgets.dialogue;

import org.dynamisui.api.event.MouseEvent;
import org.dynamisui.core.UINode;
import org.dynamisui.widgets.Label;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialoguePanelTest {

    @Test
    void hiddenByDefault() {
        DialoguePanel panel = new DialoguePanel(1920f, 1080f);
        assertFalse(panel.visible());
    }

    @Test
    void showMakesPanelVisible() {
        DialoguePanel panel = new DialoguePanel(1920f, 1080f);
        panel.show("NPC", "Hello", List.of("One"));
        assertTrue(panel.visible());
    }

    @Test
    void hideMakesPanelInvisible() {
        DialoguePanel panel = new DialoguePanel(1920f, 1080f);
        panel.show("NPC", "Hello", List.of("One"));
        panel.hide();
        assertFalse(panel.visible());
    }

    @Test
    void showWithTwoChoicesShowsTwoAndHidesTwo() {
        DialoguePanel panel = new DialoguePanel(1920f, 1080f);
        panel.show("NPC", "Hello", List.of("A", "B"));

        List<UINode> choiceNodes = panel.children().stream()
            .filter(n -> n.id().startsWith("dialogue.choice."))
            .toList();

        long visibleCount = choiceNodes.stream().filter(UINode::visible).count();
        long hiddenCount = choiceNodes.stream().filter(n -> !n.visible()).count();

        assertEquals(2L, visibleCount);
        assertEquals(2L, hiddenCount);
    }

    @Test
    void mouseClickOnChoiceFiresHandlerWithCorrectIndex() {
        DialoguePanel panel = new DialoguePanel(1920f, 1080f);
        panel.show("NPC", "Hello", List.of("Choice A", "Choice B"));

        AtomicInteger selected = new AtomicInteger(-1);
        panel.onChoiceSelected(selected::set);

        Label firstChoice = (Label) panel.children().stream()
            .filter(n -> n.id().equals("dialogue.choice.0"))
            .findFirst()
            .orElseThrow();

        float clickX = firstChoice.bounds().x() + 1f;
        float clickY = firstChoice.bounds().y() + 1f;

        boolean consumed = panel.dispatchEvent(new MouseEvent(
            1L,
            clickX,
            clickY,
            MouseEvent.MouseButton.LEFT,
            MouseEvent.MouseAction.RELEASED));

        assertTrue(consumed);
        assertEquals(0, selected.get());
    }
}
