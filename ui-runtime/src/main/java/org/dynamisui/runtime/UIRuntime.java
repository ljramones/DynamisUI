package org.dynamisui.runtime;

import org.dynamis.core.exception.DynamisException;
import org.dynamis.core.logging.DynamisLogger;
import org.dynamis.event.EventBus;
import org.dynamislocalization.api.LocalizationService;
import org.dynamislocalization.runtime.LocaleChangedEvent;
import org.dynamisui.api.UITheme;
import org.dynamisui.api.spi.UIRenderer;
import org.dynamisui.core.DefaultDarkTheme;
import org.dynamisui.core.UIStage;
import org.dynamisui.debug.DebugOverlay;
import org.dynamisui.debug.PerformanceOverlay;

/**
 * Top-level assembly for DynamisUI.
 *
 * Wires together: UIStage, DebugOverlay, LocalizationService,
 * PerformanceOverlay, and DynamisEvent EventBus.
 *
 * Game loop integration:
 * <pre>
 *   // Each frame:
 *   runtime.beginFrame();
 *   runtime.update(tick);
 *   renderer.beginFrame(width, height);
 *   runtime.render(renderer);
 *   renderer.endFrame();
 * </pre>
 */
public final class UIRuntime {

    private static final DynamisLogger log = DynamisLogger.get(UIRuntime.class);

    private final UIStage stage;
    private final DebugOverlay debugOverlay;
    private final PerformanceOverlay perfOverlay;
    private final LocalizationService localizationService;
    private final EventBus eventBus;

    private boolean wired = false;

    private UIRuntime(Builder builder) {
        UITheme theme = builder.theme != null ? builder.theme : DefaultDarkTheme.INSTANCE;

        this.stage = new UIStage(builder.screenWidth, builder.screenHeight, theme);
        this.debugOverlay = new DebugOverlay(builder.debugEnabled);
        this.perfOverlay = new PerformanceOverlay();
        this.localizationService = builder.localizationService;
        this.eventBus = builder.eventBus;
    }

    /**
     * Wires ecosystem subscriptions. Call once after construction.
     */
    public void wire() {
        if (wired) throw new IllegalStateException("UIRuntime.wire() called more than once");
        wired = true;

        // Subscribe to locale changes — rebuild all localized text
        if (eventBus != null) {
            eventBus.subscribe(LocaleChangedEvent.class, event -> {
                log.info(String.format("Locale changed to %s — rebinding UI",
                    event.current().bcp47Tag()));
                stage.rebind();
            });
            log.info("UIRuntime subscribed to LocaleChangedEvent");
        }

        log.info("UIRuntime wiring complete");
    }

    /**
     * Called at the start of each frame before update/render.
     * Clears the debug overlay panel list.
     */
    public void beginFrame() {
        debugOverlay.beginFrame();
    }

    /**
     * Updates all scene graph nodes for the current tick.
     * Also updates performance metrics.
     *
     * @param tick          current simulation tick
     * @param frameElapsedMs elapsed frame time in milliseconds
     */
    public void update(long tick, long frameElapsedMs) {
        stage.update(tick);
        perfOverlay.update(frameElapsedMs);

        if (debugOverlay.isEnabled()) {
            debugOverlay.addPanel(perfOverlay.build());
        }
    }

    /**
     * Renders the full UI: scene graph then debug overlay.
     * Caller must surround with renderer.beginFrame() / renderer.endFrame().
     */
    public void render(UIRenderer renderer) {
        stage.render(renderer);
        debugOverlay.render(renderer);
    }

    /** Resizes the stage. Call when the game window is resized. */
    public void resize(float width, float height) {
        stage.resize(width, height);
    }

    /** Toggles the debug overlay on/off. */
    public void toggleDebug() {
        debugOverlay.toggle();
        log.debug(String.format("Debug overlay: %s",
            debugOverlay.isEnabled() ? "enabled" : "disabled"));
    }

    // ── Accessors ────────────────────────────────────────────────────

    public UIStage stage()                         { return stage; }
    public DebugOverlay debugOverlay()             { return debugOverlay; }
    public PerformanceOverlay perfOverlay()        { return perfOverlay; }
    public LocalizationService localizationService() { return localizationService; }
    public EventBus eventBus()                     { return eventBus; }

    public static Builder builder() { return new Builder(); }

    // ── Builder ──────────────────────────────────────────────────────

    public static final class Builder {
        private float screenWidth  = 1920f;
        private float screenHeight = 1080f;
        private UITheme theme;
        private LocalizationService localizationService;
        private EventBus eventBus;
        private boolean debugEnabled = false;

        public Builder screenSize(float width, float height) {
            this.screenWidth  = width;
            this.screenHeight = height;
            return this;
        }

        public Builder theme(UITheme theme) {
            this.theme = theme;
            return this;
        }

        public Builder localizationService(LocalizationService svc) {
            this.localizationService = svc;
            return this;
        }

        public Builder eventBus(EventBus bus) {
            this.eventBus = bus;
            return this;
        }

        public Builder debugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
            return this;
        }

        public UIRuntime build() {
            if (screenWidth <= 0 || screenHeight <= 0) {
                throw new DynamisException("Screen dimensions must be > 0");
            }
            return new UIRuntime(this);
        }
    }
}
