package org.dynamisengine.ui.widgets.dialogue;

import org.dynamisengine.ui.api.event.MouseEvent;
import org.dynamisengine.ui.api.event.UIEvent;
import org.dynamisengine.ui.api.value.Bounds;
import org.dynamisengine.ui.api.value.Color;
import org.dynamisengine.ui.api.value.Padding;
import org.dynamisengine.ui.widgets.Label;
import org.dynamisengine.ui.widgets.Panel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * NPC conversation UI.
 *
 * Displays: speaker name, dialogue body text, and up to 4 response choices.
 * Response choices are selectable via mouse click.
 * Call show() / hide() to control visibility.
 */
public final class DialoguePanel extends Panel {

    private final Label speakerLabel;
    private final Label bodyLabel;
    private final List<Label> choiceLabels = new ArrayList<>();
    private IntConsumer onChoiceSelected;

    public DialoguePanel(float screenWidth, float screenHeight) {
        super("dialogue.panel", centeredBounds(screenWidth, screenHeight));
        backgroundColor(Color.ofHex(0x0F3460EE));
        borderColor(Color.ofHex(0xE94560FF));
        cornerRadius(8f);
        setPadding(Padding.of(16f));
        setVisible(false);

        Bounds b = bounds();
        float contentX = b.x() + 16f;
        float contentY = b.y() + 16f;

        speakerLabel = new Label("dialogue.speaker",
            Bounds.of(contentX, contentY, b.width() - 32f, 28f), "");
        bodyLabel = new Label("dialogue.body",
            Bounds.of(contentX, contentY + 36f, b.width() - 32f, 80f), "");

        addChild(speakerLabel);
        addChild(bodyLabel);

        // Pre-build 4 choice slots (hidden by default)
        for (int i = 0; i < 4; i++) {
            Label choice = new Label("dialogue.choice." + i,
                Bounds.of(contentX, contentY + 132f + i * 28f, b.width() - 32f, 24f), "");
            choice.setVisible(false);
            choiceLabels.add(choice);
            addChild(choice);
        }
    }

    public void show(String speaker, String body, List<String> choices) {
        speakerLabel.setText(speaker);
        bodyLabel.setText(body);

        for (int i = 0; i < choiceLabels.size(); i++) {
            Label lbl = choiceLabels.get(i);
            if (i < choices.size()) {
                lbl.setText((i + 1) + ". " + choices.get(i));
                lbl.setVisible(true);
            } else {
                lbl.setVisible(false);
            }
        }
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    public void onChoiceSelected(IntConsumer handler) {
        this.onChoiceSelected = handler;
    }

    @Override
    public boolean dispatchEvent(UIEvent event) {
        if (!visible() || !(event instanceof MouseEvent me)) return false;
        if (me.action() != MouseEvent.MouseAction.RELEASED) return false;

        for (int i = 0; i < choiceLabels.size(); i++) {
            Label lbl = choiceLabels.get(i);
            if (lbl.visible() && lbl.bounds().contains(me.x(), me.y())) {
                if (onChoiceSelected != null) onChoiceSelected.accept(i);
                return true;
            }
        }
        return super.dispatchEvent(event);
    }

    private static Bounds centeredBounds(float sw, float sh) {
        float w = Math.min(640f, sw - 80f);
        float h = 280f;
        return Bounds.of((sw - w) / 2f, sh - h - 40f, w, h);
    }
}
