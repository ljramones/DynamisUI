package org.dynamisengine.ui.debug;

import org.dynamisengine.core.logging.DynamisLogger;

/**
 * Builds a DebugPanel with frame performance metrics.
 *
 * Call update() each frame with current timing data,
 * then build() to get the panel for DebugOverlay.addPanel().
 */
public final class PerformanceOverlay {

    private static final DynamisLogger log = DynamisLogger.get(PerformanceOverlay.class);

    private static final int SAMPLE_WINDOW = 60;

    private final long[] frameTimes = new long[SAMPLE_WINDOW];
    private int frameIndex = 0;
    private long lastFrameNanos = System.nanoTime();
    private float currentFps = 0f;
    private float avgFrameMs = 0f;

    /** Call once per frame with the frame elapsed time in milliseconds. */
    public void update(long frameElapsedMs) {
        long now = System.nanoTime();
        long delta = now - lastFrameNanos;
        lastFrameNanos = now;

        frameTimes[frameIndex % SAMPLE_WINDOW] = delta;
        frameIndex++;

        int samples = Math.min(frameIndex, SAMPLE_WINDOW);
        long total = 0;
        for (int i = 0; i < samples; i++) {
            total += frameTimes[i];
        }
        avgFrameMs = (total / (float) samples) / 1_000_000f;
        currentFps = avgFrameMs > 0 ? 1000f / avgFrameMs : 0f;
    }

    /** Builds the debug panel for this frame. */
    public DebugPanel build() {
        return DebugPanel.builder("Performance")
            .row("FPS", String.format("%.1f", currentFps))
            .row("Frame", String.format("%.2f ms", avgFrameMs))
            .build();
    }

    public float fps() { return currentFps; }
    public float avgFrameMs() { return avgFrameMs; }
}
