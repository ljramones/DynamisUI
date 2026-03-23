package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Loads NDJSON debug snapshot files for replay.
 *
 * <p>Each line is deserialized into a {@link DebugViewSnapshot} that can be
 * fed into the existing replay system (DebugOverlayState + builder + renderer).
 */
public final class DebugSnapshotReplayLoader {

    private static final Logger LOG = Logger.getLogger(DebugSnapshotReplayLoader.class.getName());

    private DebugSnapshotReplayLoader() {}

    /**
     * Load all snapshots from an NDJSON file.
     *
     * @param path path to the NDJSON file
     * @return list of snapshots in chronological order
     */
    public static List<DebugViewSnapshot> load(Path path) throws IOException {
        var lines = Files.readAllLines(path);
        var snapshots = new ArrayList<DebugViewSnapshot>(lines.size());

        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            if (line.isBlank()) continue;
            try {
                snapshots.add(DebugSnapshotJson.fromJson(line));
            } catch (Exception e) {
                LOG.warning("Failed to parse snapshot at line " + lineNum + ": " + e.getMessage());
            }
        }

        LOG.info("Loaded " + snapshots.size() + " snapshots from " + path);
        return snapshots;
    }

    /**
     * Get a snapshot by frame index (0-based into the loaded list).
     * Returns EMPTY if index is out of range.
     */
    public static DebugViewSnapshot getFrame(List<DebugViewSnapshot> snapshots, int index) {
        if (index < 0 || index >= snapshots.size()) return DebugViewSnapshot.EMPTY;
        return snapshots.get(index);
    }
}
