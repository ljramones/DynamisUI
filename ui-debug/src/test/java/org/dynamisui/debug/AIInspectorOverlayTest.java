package org.dynamisui.debug;

import org.dynamis.core.entity.EntityId;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIInspectorOverlayTest {

    @Test
    void defaultBuildProducesAiInspectorPanel() {
        AIInspectorOverlay overlay = new AIInspectorOverlay();
        DebugPanel panel = overlay.build();

        assertEquals("AI Inspector", panel.title());
    }

    @Test
    void updateProducesExpectedRows() {
        AIInspectorOverlay overlay = new AIInspectorOverlay();
        overlay.update(EntityId.of(42L), "TIER_1", "Patrol", 7, 3);

        DebugPanel panel = overlay.build();

        assertEquals("42", panel.rows().get(0).value());
        assertEquals("TIER_1", panel.rows().get(1).value());
        assertEquals("Patrol", panel.rows().get(2).value());
        assertEquals("7", panel.rows().get(3).value());
        assertEquals("3", panel.rows().get(4).value());
    }

    @Test
    void nullLodTierRendersDash() {
        AIInspectorOverlay overlay = new AIInspectorOverlay();
        overlay.update(EntityId.of(1L), null, "Search", 1, 1);

        DebugPanel panel = overlay.build();

        assertEquals("—", panel.rows().get(1).value());
    }

    @Test
    void extraRowsAppearInBuiltPanel() {
        AIInspectorOverlay overlay = new AIInspectorOverlay();
        overlay.update(EntityId.of(1L), "TIER_0", "Idle", 2, 1);
        overlay.setExtraRows(Map.of("Target", "Player", "State", "Alert"));

        DebugPanel panel = overlay.build();

        assertTrue(panel.rows().stream().anyMatch(r -> r.label().equals("Target") && r.value().equals("Player")));
        assertTrue(panel.rows().stream().anyMatch(r -> r.label().equals("State") && r.value().equals("Alert")));
    }
}
