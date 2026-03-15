package org.dynamisengine.ui.widgets.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayerTest {

    @Test
    void setHealthUpdatesHealthBarValue() {
        HudLayer hud = new HudLayer(1920f, 1080f);
        hud.setHealth(0.5f);
        assertEquals(0.5f, hud.health());
    }

    @Test
    void setStaminaUpdatesStaminaBarValue() {
        HudLayer hud = new HudLayer(1920f, 1080f);
        hud.setStamina(0.75f);
        assertEquals(0.75f, hud.stamina());
    }

    @Test
    void hasTwoChildrenHealthAndStamina() {
        HudLayer hud = new HudLayer(1920f, 1080f);
        assertEquals(2, hud.children().size());
    }

    @Test
    void visibleByDefault() {
        HudLayer hud = new HudLayer(1920f, 1080f);
        assertTrue(hud.visible());
    }
}
