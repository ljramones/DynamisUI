package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * Exports {@link DebugViewSnapshot} as newline-delimited JSON (NDJSON) to a file.
 *
 * <p>Each snapshot is written as a single JSON line. The file can be replayed
 * by reading lines and deserializing each with {@link DebugSnapshotJson#fromJson}.
 *
 * <p>Thread-safe: writes are synchronized.
 */
public final class JsonFileExporter implements DebugSnapshotExporter {

    private static final Logger LOG = Logger.getLogger(JsonFileExporter.class.getName());

    private final Path outputPath;
    private BufferedWriter writer;
    private int snapshotCount;

    public JsonFileExporter(Path outputPath) {
        this.outputPath = outputPath;
    }

    /** Open the file for writing. Creates or truncates. */
    public void open() throws IOException {
        writer = Files.newBufferedWriter(outputPath,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        snapshotCount = 0;
        LOG.info("Exporting debug snapshots to: " + outputPath);
    }

    @Override
    public synchronized void export(DebugViewSnapshot snapshot) {
        if (writer == null) return;
        try {
            writer.write(DebugSnapshotJson.toJson(snapshot));
            writer.newLine();
            snapshotCount++;
        } catch (IOException e) {
            LOG.warning("Failed to export snapshot: " + e.getMessage());
        }
    }

    @Override
    public synchronized void flush() {
        if (writer == null) return;
        try { writer.flush(); } catch (IOException e) {
            LOG.warning("Failed to flush export: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        if (writer == null) return;
        try {
            writer.flush();
            writer.close();
            LOG.info("Export complete: " + snapshotCount + " snapshots to " + outputPath);
        } catch (IOException e) {
            LOG.warning("Failed to close export: " + e.getMessage());
        }
        writer = null;
    }

    public int snapshotCount() { return snapshotCount; }
    public Path outputPath() { return outputPath; }
}
