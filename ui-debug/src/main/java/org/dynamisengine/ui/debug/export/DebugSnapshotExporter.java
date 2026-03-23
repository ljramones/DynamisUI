package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;

/**
 * Exports {@link DebugViewSnapshot} instances for external consumption.
 *
 * <p>Implementations may write to files, streams, or network sockets.
 * The exporter is stateless per call — each snapshot is self-contained.
 */
public interface DebugSnapshotExporter {

    /** Export a single snapshot. */
    void export(DebugViewSnapshot snapshot);

    /** Flush any buffered data. */
    default void flush() {}

    /** Close the exporter and release resources. */
    default void close() {}
}
