package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void exportWritesNdjson() throws IOException {
        Path file = tempDir.resolve("test.ndjson");
        var exporter = new JsonFileExporter(file);
        exporter.open();

        exporter.export(new DebugViewSnapshot(Map.of(), List.of(),
            new DebugViewSnapshot.DebugSummaryView(1, 3f, 18f, 3, 10, 3), 1));
        exporter.export(new DebugViewSnapshot(Map.of(), List.of(),
            new DebugViewSnapshot.DebugSummaryView(2, 4f, 24f, 3, 11, 3), 2));

        exporter.close();

        var lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"tick\":1"));
        assertTrue(lines.get(1).contains("\"tick\":2"));
    }

    @Test
    void snapshotCountTracked() throws IOException {
        Path file = tempDir.resolve("count.ndjson");
        var exporter = new JsonFileExporter(file);
        exporter.open();

        for (int i = 0; i < 5; i++) {
            exporter.export(DebugViewSnapshot.EMPTY);
        }
        exporter.close();

        assertEquals(5, exporter.snapshotCount());
    }

    @Test
    void replayLoadRoundtrip() throws IOException {
        Path file = tempDir.resolve("replay.ndjson");
        var exporter = new JsonFileExporter(file);
        exporter.open();

        var summary = new DebugViewSnapshot.DebugSummaryView(42, 10f, 60f, 5, 100, 4);
        exporter.export(new DebugViewSnapshot(Map.of(), List.of(), summary, 42));
        exporter.close();

        var loaded = DebugSnapshotReplayLoader.load(file);
        assertEquals(1, loaded.size());
        assertEquals(42, loaded.getFirst().tick());
        assertEquals(10f, loaded.getFirst().summary().frameTimeMs(), 0.1f);
    }
}
