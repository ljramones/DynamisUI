package org.dynamisui.core.layout;

import org.dynamisui.api.UIComponent;
import org.dynamisui.api.layout.UILayout;
import org.dynamisui.api.value.Bounds;

import java.util.List;

/**
 * No-op layout — children keep their own bounds unchanged.
 * Use when positions are set manually.
 */
public final class AbsoluteLayout implements UILayout {

    public static final AbsoluteLayout INSTANCE = new AbsoluteLayout();

    @Override
    public void layout(Bounds available, List<? extends UIComponent> children) {
        // Children manage their own positions.
    }
}
