package org.dynamisengine.ui.demo.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.dynamisengine.event.EventBusBuilder;
import org.dynamisengine.localization.api.value.LocaleDescriptor;
import org.dynamisengine.localization.api.value.LocaleKey;
import org.dynamisengine.localization.api.value.MissingKeyBehavior;
import org.dynamisengine.localization.core.JsonStringTableLoader;
import org.dynamisengine.localization.runtime.LocalizationRuntime;
import org.dynamisengine.ui.core.DefaultDarkTheme;
import org.dynamisengine.ui.debug.AIInspectorOverlay;
import org.dynamisengine.ui.debug.DebugPanel;
import org.dynamisengine.ui.runtime.UIRuntime;
import org.dynamisengine.ui.widgets.dialogue.DialoguePanel;
import org.dynamisengine.ui.widgets.hud.HudLayer;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JavaFX application demonstrating DynamisUI with a real renderer.
 *
 * Shows:
 * - HUD layer (health + stamina bars)
 * - Dialogue panel (NPC conversation with choices)
 * - Debug overlay (AI inspector, performance panel)
 * - Locale switching (EN/FR/RU cycle on L key)
 *
 * Controls:
 *   H — deal damage (reduce health by 15%)
 *   S — drain stamina (reduce stamina by 20%)
 *   D — toggle dialogue panel
 *   F — toggle debug overlay
 *   L — cycle locale (EN → FR → RU → EN)
 *   1/2/3 — select dialogue choice
 *   ESC — quit
 *
 * Run: cd demo && mvn exec:java -pl demo-ui
 */
public final class UiDemoApp extends Application {

    private static final float W = 1280f;
    private static final float H = 720f;

    private UIRuntime uiRuntime;
    private JavaFxUIRenderer renderer;
    private HudLayer hud;
    private DialoguePanel dialogue;
    private AIInspectorOverlay aiInspector;
    private LocalizationRuntime locRuntime;

    private final AtomicLong tick = new AtomicLong(0);
    private final LocaleDescriptor[] locales = {
        LocaleDescriptor.EN_US, LocaleDescriptor.FR_FR, LocaleDescriptor.RU_RU};
    private int localeIndex = 0;

    // Simulated AI state for inspector
    private String currentIntent = "locomotion.moveTo";
    private int beliefCount = 7;

    @Override
    public void start(Stage stage) throws Exception {
        // ── Localization ─────────────────────────────────────────────
        Path locPath = resolveResourcePath("localization");
        locRuntime = LocalizationRuntime.builder()
            .initialLocale(LocaleDescriptor.EN_US)
            .supportedLocales(List.of(locales))
            .loader(new JsonStringTableLoader(locPath))
            .namespace("dynamis.demo")
            .missingKeyBehavior(MissingKeyBehavior.RETURN_KEY)
            .eventBus(EventBusBuilder.create().synchronous().build())
            .build();

        // ── UIRuntime ────────────────────────────────────────────────
        uiRuntime = UIRuntime.builder()
            .screenSize(W, H)
            .theme(DefaultDarkTheme.INSTANCE)
            .localizationService(locRuntime.service())
            .eventBus(locRuntime.eventBus())
            .debugEnabled(false)
            .build();
        uiRuntime.wire();

        // ── Widgets ──────────────────────────────────────────────────
        hud = new HudLayer(W, H);
        uiRuntime.stage().addLayer(hud);

        dialogue = new DialoguePanel(W, H);
        dialogue.onChoiceSelected(i -> {
            System.out.println("Choice selected: " + i);
            dialogue.hide();
        });
        uiRuntime.stage().addLayer(dialogue);

        aiInspector = new AIInspectorOverlay();

        // ── Canvas + Renderer ────────────────────────────────────────
        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        renderer = new JavaFxUIRenderer(gc);

        // ── Scene + Input ────────────────────────────────────────────
        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #1A1A2E;");
        Scene scene = new Scene(root, W, H);

        scene.setOnKeyPressed(e -> handleKey(e.getCode()));

        stage.setTitle("DynamisUI Demo — JavaFX Renderer Bridge");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // ── Game Loop ────────────────────────────────────────────────
        long[] lastNanos = {System.nanoTime()};
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = (now - lastNanos[0]) / 1_000_000L;
                lastNanos[0] = now;
                long t = tick.incrementAndGet();

                // Update AI inspector state
                aiInspector.update(
                    org.dynamisengine.core.entity.EntityId.of(1L),
                    "TIER_0", currentIntent, beliefCount, 0);

                uiRuntime.beginFrame();
                uiRuntime.update(t, elapsed);

                if (uiRuntime.debugOverlay().isEnabled()) {
                    uiRuntime.debugOverlay().addPanel(aiInspector.build());
                }

                renderer.beginFrame(W, H);
                uiRuntime.render(renderer);
                renderer.endFrame();
            }
        }.start();

        printControls();
    }

    private void handleKey(KeyCode code) {
        switch (code) {
            case H -> {
                hud.setHealth(Math.max(0f, hud.health() - 0.15f));
                beliefCount++;
                System.out.printf("  Health: %.0f%%%n", hud.health() * 100);
            }
            case S -> {
                hud.setStamina(Math.max(0f, hud.stamina() - 0.20f));
                System.out.printf("  Stamina: %.0f%%%n", hud.stamina() * 100);
            }
            case D -> {
                if (dialogue.visible()) {
                    dialogue.hide();
                } else {
                    dialogue.show(
                        "Aria",
                        "I've discovered something troubling about Dorian. " +
                        "What should we do?",
                        List.of(
                            "Confront him directly",
                            "Gather more evidence first",
                            "Report to the authorities",
                            "Ignore it for now"));
                    currentIntent = "dialogue.initiate";
                }
            }
            case F -> {
                uiRuntime.toggleDebug();
                System.out.println("  Debug overlay: " +
                    (uiRuntime.debugOverlay().isEnabled() ? "ON" : "OFF"));
            }
            case L -> {
                localeIndex = (localeIndex + 1) % locales.length;
                locRuntime.switchLocale(locales[localeIndex]);
                System.out.println("  Locale: " + locales[localeIndex].displayName());
            }
            case DIGIT1 -> selectChoice(0);
            case DIGIT2 -> selectChoice(1);
            case DIGIT3 -> selectChoice(2);
            case ESCAPE -> System.exit(0);
        }
    }

    private void selectChoice(int index) {
        if (dialogue.visible()) {
            dialogue.hide();
            System.out.println("  Dialogue choice " + (index + 1) + " selected");
            currentIntent = "locomotion.patrol";
        }
    }

    private static Path resolveResourcePath(String name) {
        try {
            var url = UiDemoApp.class.getClassLoader().getResource(name);
            if (url == null) throw new IllegalStateException(
                name + " resource directory not found on classpath");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to resolve resource path", e);
        }
    }

    private static void printControls() {
        System.out.println("─".repeat(56));
        System.out.println("  DynamisUI Demo Controls");
        System.out.println("─".repeat(56));
        System.out.println("  H   — deal damage (-15% health)");
        System.out.println("  S   — drain stamina (-20%)");
        System.out.println("  D   — toggle dialogue panel");
        System.out.println("  F   — toggle debug overlay");
        System.out.println("  L   — cycle locale (EN→FR→RU→EN)");
        System.out.println("  ESC — quit");
        System.out.println("─".repeat(56));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
