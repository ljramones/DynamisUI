package org.dynamisui.widgets;

import org.dynamisui.api.value.Bounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressBarTest {

    @Test
    void initialValueClampedToRange() {
        ProgressBar over = new ProgressBar("pb1", Bounds.of(0f, 0f, 100f, 10f), 2f);
        ProgressBar under = new ProgressBar("pb2", Bounds.of(0f, 0f, 100f, 10f), -1f);

        assertEquals(1f, over.getValue());
        assertEquals(0f, under.getValue());
    }

    @Test
    void setValueAboveOneClampsToOne() {
        ProgressBar bar = new ProgressBar("pb", Bounds.of(0f, 0f, 100f, 10f), 0f);
        bar.setValue(1.5f);
        assertEquals(1f, bar.getValue());
    }

    @Test
    void setValueBelowZeroClampsToZero() {
        ProgressBar bar = new ProgressBar("pb", Bounds.of(0f, 0f, 100f, 10f), 1f);
        bar.setValue(-0.1f);
        assertEquals(0f, bar.getValue());
    }

    @Test
    void renderDrawsTrackAndFillWhenValuePositive() {
        ProgressBar bar = new ProgressBar("pb", Bounds.of(0f, 0f, 100f, 10f), 0.5f);
        StubUIRenderer renderer = new StubUIRenderer();

        bar.render(renderer);

        long roundRectCalls = renderer.calls.stream().filter("drawRoundRect"::equals).count();
        assertEquals(2L, roundRectCalls);
    }

    @Test
    void renderWithZeroValueDrawsOnlyTrack() {
        ProgressBar bar = new ProgressBar("pb", Bounds.of(0f, 0f, 100f, 10f), 0f);
        StubUIRenderer renderer = new StubUIRenderer();

        bar.render(renderer);

        long roundRectCalls = renderer.calls.stream().filter("drawRoundRect"::equals).count();
        assertEquals(1L, roundRectCalls);
    }
}
