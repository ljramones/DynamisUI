package org.dynamisui.runtime;

import org.dynamis.core.exception.DynamisException;
import org.dynamis.event.SynchronousEventBus;
import org.dynamislocalization.api.value.LocaleDescriptor;
import org.dynamislocalization.runtime.LocaleChangedEvent;
import org.dynamisui.api.value.Bounds;
import org.dynamisui.core.LocalizedNode;
import org.dynamisui.core.UINode;
import org.dynamisui.debug.DebugPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIRuntimeTest {

    @Test
    void wireCalledTwiceThrowsIllegalStateException() {
        UIRuntime runtime = UIRuntime.builder().build();
        runtime.wire();
        assertThrows(IllegalStateException.class, runtime::wire);
    }

    @Test
    void buildWithZeroScreenWidthThrowsDynamisException() {
        assertThrows(DynamisException.class, () -> UIRuntime.builder().screenSize(0f, 1080f).build());
    }

    @Test
    void stageIsNotNullAfterBuild() {
        UIRuntime runtime = UIRuntime.builder().build();
        assertNotNull(runtime.stage());
    }

    @Test
    void debugOverlayReflectsDebugEnabledSetting() {
        UIRuntime enabled = UIRuntime.builder().debugEnabled(true).build();
        UIRuntime disabled = UIRuntime.builder().debugEnabled(false).build();

        assertTrue(enabled.debugOverlay().isEnabled());
        assertTrue(!disabled.debugOverlay().isEnabled());
    }

    @Test
    void toggleDebugFlipsDebugOverlayState() {
        UIRuntime runtime = UIRuntime.builder().debugEnabled(false).build();
        runtime.toggleDebug();
        assertTrue(runtime.debugOverlay().isEnabled());
    }

    @Test
    void resizeUpdatesStageDimensions() {
        UIRuntime runtime = UIRuntime.builder().screenSize(100f, 100f).build();
        runtime.resize(800f, 600f);

        assertEquals(800f, runtime.stage().screenWidth());
        assertEquals(600f, runtime.stage().screenHeight());
    }

    @Test
    void localeChangedEventTriggersStageRebind() {
        SynchronousEventBus eventBus = new SynchronousEventBus();
        UIRuntime runtime = UIRuntime.builder().eventBus(eventBus).build();

        LocalizedTestNode localized = new LocalizedTestNode("localized", Bounds.of(0f, 0f, 10f, 10f));
        runtime.stage().addLayer(localized);

        runtime.wire();
        eventBus.publish(new LocaleChangedEvent(LocaleDescriptor.EN_US, LocaleDescriptor.FR_FR));

        assertEquals(1, localized.localeChangedCalls);
    }

    @Test
    void beginFrameClearsDebugPanels() {
        UIRuntime runtime = UIRuntime.builder().debugEnabled(true).build();
        runtime.debugOverlay().addPanel(DebugPanel.builder("Temp").row("A", "1").build());
        runtime.beginFrame();

        StubUIRenderer renderer = new StubUIRenderer();
        runtime.render(renderer);

        assertTrue(renderer.calls.isEmpty());
    }

    @Test
    void updateWithDebugEnabledAddsPerfPanelToOverlay() {
        UIRuntime runtime = UIRuntime.builder().debugEnabled(true).build();
        runtime.beginFrame();
        runtime.update(1L, 16L);

        StubUIRenderer renderer = new StubUIRenderer();
        runtime.render(renderer);

        assertTrue(renderer.calls.contains("drawRect"));
        assertTrue(renderer.calls.contains("drawText"));
    }

    private static final class LocalizedTestNode extends UINode implements LocalizedNode {
        int localeChangedCalls;

        private LocalizedTestNode(String id, Bounds bounds) {
            super(id, bounds);
        }

        @Override
        public void onLocaleChanged() {
            localeChangedCalls++;
        }
    }
}
