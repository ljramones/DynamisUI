package org.dynamisengine.ui.core;

import org.dynamisengine.core.exception.DynamisException;
import org.dynamisengine.ui.api.event.FocusEvent;
import org.dynamisengine.ui.api.value.Bounds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIStageTest {

    @Test
    void addLayerAddsToLayerList() {
        UIStage stage = new UIStage(1920f, 1080f, DefaultDarkTheme.INSTANCE);
        UINode layer = new UINode("hud", Bounds.of(0f, 0f, 100f, 100f));

        stage.addLayer(layer);

        assertEquals(1, stage.layers().size());
        assertEquals(layer, stage.layers().getFirst());
    }

    @Test
    void removeLayerRemovesById() {
        UIStage stage = new UIStage(1920f, 1080f, DefaultDarkTheme.INSTANCE);
        UINode layer = new UINode("hud", Bounds.of(0f, 0f, 100f, 100f));
        stage.addLayer(layer);

        stage.removeLayer("hud");

        assertTrue(stage.layers().isEmpty());
    }

    @Test
    void dispatchEventRoutesTopToBottom() {
        UIStage stage = new UIStage(1920f, 1080f, DefaultDarkTheme.INSTANCE);
        List<String> order = new ArrayList<>();

        UINode bottom = new UINode("bottom", Bounds.of(0f, 0f, 100f, 100f));
        bottom.addListener(event -> {
            order.add("bottom");
            return false;
        });

        UINode top = new UINode("top", Bounds.of(0f, 0f, 100f, 100f));
        top.addListener(event -> {
            order.add("top");
            return false;
        });

        stage.addLayer(bottom);
        stage.addLayer(top);

        stage.dispatchEvent(new FocusEvent(1L, FocusEvent.FocusAction.GAINED));

        assertEquals(List.of("top", "bottom"), order);
    }

    @Test
    void renderCallsRenderSelfOnVisibleLayers() {
        UIStage stage = new UIStage(1920f, 1080f, DefaultDarkTheme.INSTANCE);
        RecordingNode visible = new RecordingNode("visible", Bounds.of(0f, 0f, 100f, 100f));
        RecordingNode hidden = new RecordingNode("hidden", Bounds.of(0f, 0f, 100f, 100f));
        hidden.setVisible(false);
        stage.addLayer(visible);
        stage.addLayer(hidden);

        StubUIRenderer renderer = new StubUIRenderer();
        stage.render(renderer);

        assertEquals(1, visible.renderSelfCalls);
        assertEquals(0, hidden.renderSelfCalls);
    }

    @Test
    void rebindCallsOnLocaleChangedOnLocalizedNodes() {
        UIStage stage = new UIStage(1920f, 1080f, DefaultDarkTheme.INSTANCE);
        LocalizedRecordingNode layer = new LocalizedRecordingNode("layer", Bounds.of(0f, 0f, 100f, 100f));
        LocalizedRecordingNode child = new LocalizedRecordingNode("child", Bounds.of(0f, 0f, 50f, 50f));
        layer.addChild(child);
        stage.addLayer(layer);

        stage.rebind();

        assertEquals(1, layer.localeChangeCalls);
        assertEquals(1, child.localeChangeCalls);
    }

    @Test
    void resizeUpdatesScreenDimensions() {
        UIStage stage = new UIStage(100f, 100f, DefaultDarkTheme.INSTANCE);
        UINode layer = new UINode("layer", Bounds.of(0f, 0f, 10f, 10f));
        stage.addLayer(layer);

        stage.resize(800f, 600f);

        assertEquals(800f, stage.screenWidth());
        assertEquals(600f, stage.screenHeight());
        assertEquals(Bounds.of(0f, 0f, 800f, 600f), layer.bounds());
    }

    @Test
    void zeroScreenDimensionsThrowDynamisException() {
        assertThrows(DynamisException.class, () -> new UIStage(0f, 100f, DefaultDarkTheme.INSTANCE));
        UIStage stage = new UIStage(100f, 100f, DefaultDarkTheme.INSTANCE);
        assertThrows(DynamisException.class, () -> stage.resize(0f, 10f));
        assertThrows(DynamisException.class, () -> stage.resize(10f, 0f));
    }

    private static final class RecordingNode extends UINode {
        int renderSelfCalls;

        private RecordingNode(String id, Bounds bounds) {
            super(id, bounds);
        }

        @Override
        protected void renderSelf(org.dynamisengine.ui.api.spi.UIRenderer renderer) {
            renderSelfCalls++;
        }
    }

    private static final class LocalizedRecordingNode extends UINode implements LocalizedNode {
        int localeChangeCalls;

        private LocalizedRecordingNode(String id, Bounds bounds) {
            super(id, bounds);
        }

        @Override
        public void onLocaleChanged() {
            localeChangeCalls++;
        }
    }
}
