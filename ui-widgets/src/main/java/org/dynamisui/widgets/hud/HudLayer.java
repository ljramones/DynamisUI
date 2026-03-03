package org.dynamisui.widgets.hud;

import org.dynamisui.api.value.Bounds;
import org.dynamisui.api.value.Color;
import org.dynamisui.widgets.Panel;
import org.dynamisui.widgets.ProgressBar;

/**
 * In-game HUD layer.
 *
 * Default layout: health and stamina bars in the bottom-left corner.
 * Add to UIStage as the base game layer.
 */
public final class HudLayer extends Panel {

    private final ProgressBar healthBar;
    private final ProgressBar staminaBar;

    public HudLayer(float screenWidth, float screenHeight) {
        super("hud.layer", Bounds.of(0, 0, screenWidth, screenHeight));
        backgroundColor(Color.TRANSPARENT);

        float barWidth  = 200f;
        float barHeight = 16f;
        float margin    = 24f;
        float gap       = 8f;

        float baseY = screenHeight - margin - barHeight * 2 - gap;

        healthBar = new ProgressBar("hud.health",
            Bounds.of(margin, baseY, barWidth, barHeight), 1.0f);
        healthBar.setFillColor(Color.ofHex(0xFF4444FF));

        staminaBar = new ProgressBar("hud.stamina",
            Bounds.of(margin, baseY + barHeight + gap, barWidth, barHeight), 1.0f);
        staminaBar.setFillColor(Color.ofHex(0x44AA44FF));

        addChild(healthBar);
        addChild(staminaBar);
    }

    public void setHealth(float value)  { healthBar.setValue(value); }
    public void setStamina(float value) { staminaBar.setValue(value); }

    public float health()  { return healthBar.getValue(); }
    public float stamina() { return staminaBar.getValue(); }
}
