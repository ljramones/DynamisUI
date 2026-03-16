package org.dynamisengine.ui.widgets;

import org.dynamisengine.localization.api.LocaleChangeListener;
import org.dynamisengine.localization.api.LocalizationService;
import org.dynamisengine.localization.api.StringTable;
import org.dynamisengine.localization.api.value.LocaleDescriptor;
import org.dynamisengine.localization.api.value.LocaleKey;
import org.dynamisengine.localization.api.value.MissingKeyBehavior;
import org.dynamisengine.ui.api.value.Bounds;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelTest {

    private static final Bounds BOUNDS = Bounds.of(0f, 0f, 100f, 20f);

    /** Minimal stub that maps LocaleKey -> String. */
    static final class StubLocalizationService implements LocalizationService {

        private final Map<String, String> table = new HashMap<>();

        void put(LocaleKey key, String value) {
            table.put(key.fullyQualified(), value);
        }

        @Override
        public LocaleDescriptor currentLocale() { return null; }

        @Override
        public void switchLocale(LocaleDescriptor locale) {}

        @Override
        public String get(LocaleKey key) {
            return table.getOrDefault(key.fullyQualified(), key.fullyQualified());
        }

        @Override
        public String get(LocaleKey key, MissingKeyBehavior onMissing) {
            String v = table.get(key.fullyQualified());
            if (v != null) return v;
            return switch (onMissing) {
                case RETURN_KEY -> key.fullyQualified();
                case RETURN_EMPTY -> "";
                case THROW -> throw new RuntimeException("Missing: " + key);
            };
        }

        @Override
        public String getPlural(LocaleKey key, long count) { return get(key); }

        @Override
        public String getPlural(LocaleKey key, long count, MissingKeyBehavior onMissing) { return get(key, onMissing); }

        @Override
        public Optional<StringTable> tableFor(String namespace) { return Optional.empty(); }

        @Override
        public List<LocaleDescriptor> supportedLocales() { return List.of(); }

        @Override
        public void addLocaleChangeListener(LocaleChangeListener listener) {}

        @Override
        public void removeLocaleChangeListener(LocaleChangeListener listener) {}
    }

    @Test
    void rawTextLabelReturnsProvidedText() {
        Label label = new Label("lbl", BOUNDS, "Hello");
        assertEquals("Hello", label.resolvedText());
    }

    @Test
    void localizedLabelResolvesTextFromService() {
        LocaleKey key = LocaleKey.of("ui:greeting");
        StubLocalizationService loc = new StubLocalizationService();
        loc.put(key, "Hola");

        Label label = new Label("lbl", BOUNDS, key, loc);

        assertEquals("Hola", label.resolvedText());
    }

    @Test
    void onLocaleChangedReResolvesText() {
        LocaleKey key = LocaleKey.of("ui:greeting");
        StubLocalizationService loc = new StubLocalizationService();
        loc.put(key, "Hello");

        Label label = new Label("lbl", BOUNDS, key, loc);
        assertEquals("Hello", label.resolvedText());

        loc.put(key, "Bonjour");
        label.onLocaleChanged();
        assertEquals("Bonjour", label.resolvedText());
    }

    @Test
    void setLocaleKeyUpdatesResolvedText() {
        LocaleKey key1 = LocaleKey.of("ui:a");
        LocaleKey key2 = LocaleKey.of("ui:b");
        StubLocalizationService loc = new StubLocalizationService();
        loc.put(key1, "A");
        loc.put(key2, "B");

        Label label = new Label("lbl", BOUNDS, key1, loc);
        assertEquals("A", label.resolvedText());

        label.setLocaleKey(key2);
        assertEquals("B", label.resolvedText());
    }

    @Test
    void setTextSwitchesToRawMode() {
        LocaleKey key = LocaleKey.of("ui:x");
        StubLocalizationService loc = new StubLocalizationService();
        loc.put(key, "Localized");

        Label label = new Label("lbl", BOUNDS, key, loc);
        label.setText("Raw");

        assertEquals("Raw", label.resolvedText());
    }

    @Test
    void onLocaleChangedOnRawLabelIsNoOp() {
        Label label = new Label("lbl", BOUNDS, "Static");
        label.onLocaleChanged();
        assertEquals("Static", label.resolvedText());
    }

    @Test
    void renderCallsDrawText() {
        Label label = new Label("lbl", BOUNDS, "Hi");
        StubUIRenderer renderer = new StubUIRenderer();

        label.render(renderer);

        assertTrue(renderer.calls.contains("drawText"));
    }
}
