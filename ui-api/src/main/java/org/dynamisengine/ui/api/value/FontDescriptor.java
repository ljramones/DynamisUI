package org.dynamisengine.ui.api.value;

import org.dynamisengine.core.exception.DynamisException;

/**
 * Describes a font for use by UIRenderer.
 */
public record FontDescriptor(String family, float size, FontWeight weight) {

    public static final FontDescriptor DEFAULT =
        new FontDescriptor("sans-serif", 14f, FontWeight.NORMAL);
    public static final FontDescriptor HEADING =
        new FontDescriptor("sans-serif", 20f, FontWeight.BOLD);
    public static final FontDescriptor MONOSPACE =
        new FontDescriptor("monospace", 12f, FontWeight.NORMAL);

    public FontDescriptor {
        if (family == null || family.isBlank()) {
            throw new DynamisException("FontDescriptor family must not be null or blank");
        }
        if (size <= 0) {
            throw new DynamisException("FontDescriptor size must be > 0, got: " + size);
        }
        if (weight == null) {
            throw new DynamisException("FontDescriptor weight must not be null");
        }
    }

    public enum FontWeight { LIGHT, NORMAL, BOLD }
}
