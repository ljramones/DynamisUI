package org.dynamisengine.ui.debug;

import org.dynamisengine.core.entity.EntityId;

import java.util.Map;

/**
 * Builds a DebugPanel showing AI state for a selected entity.
 *
 * Data is passed in per-frame from game code — this overlay
 * does not import DynamisAI directly (avoids module dependency).
 *
 * Usage:
 * <pre>
 *   AIInspectorOverlay inspector = new AIInspectorOverlay();
 *   inspector.update(agentId, lodTier, intentType, beliefCount, queueDepth);
 *   overlay.addPanel(inspector.build());
 * </pre>
 */
public final class AIInspectorOverlay {

    private EntityId agentId;
    private String lodTier    = "—";
    private String intentType = "—";
    private int beliefCount   = 0;
    private int queueDepth    = 0;
    private Map<String, String> extraRows = Map.of();

    /**
     * Updates the inspector state for the current frame.
     *
     * @param agentId     the agent being inspected
     * @param lodTier     LOD tier string (e.g. "TIER_0")
     * @param intentType  current intent type string
     * @param beliefCount number of active beliefs
     * @param queueDepth  cognition service queue depth
     */
    public void update(EntityId agentId,
                       String lodTier,
                       String intentType,
                       int beliefCount,
                       int queueDepth) {
        this.agentId     = agentId;
        this.lodTier     = lodTier != null ? lodTier : "—";
        this.intentType  = intentType != null ? intentType : "—";
        this.beliefCount = beliefCount;
        this.queueDepth  = queueDepth;
    }

    /** Optional extra rows for game-specific AI state. */
    public void setExtraRows(Map<String, String> rows) {
        this.extraRows = rows != null ? Map.copyOf(rows) : Map.of();
    }

    /** Builds the debug panel for this frame. */
    public DebugPanel build() {
        String agentStr = agentId != null ? String.valueOf(agentId.id()) : "none";
        DebugPanel.Builder b = DebugPanel.builder("AI Inspector")
            .row("Agent",   agentStr)
            .row("LOD",     lodTier)
            .row("Intent",  intentType)
            .row("Beliefs", beliefCount)
            .row("Queue",   queueDepth);
        extraRows.forEach(b::row);
        return b.build();
    }
}
