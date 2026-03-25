package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A session bundle is a directory containing all artifacts of a debug session:
 *
 * <pre>
 * my-session.bundle/
 *   session.ndjson          — frame data
 *   meta.json               — metadata, bookmarks, investigation windows
 *   compare-report.json     — optional compare report
 * </pre>
 *
 * <p>Provides static helpers for creating, loading, and saving bundles.
 */
public final class SessionBundle {

    private static final Logger LOG = Logger.getLogger(SessionBundle.class.getName());

    public static final String SESSION_FILE = "session.ndjson";
    public static final String META_FILE = "meta.json";
    public static final String COMPARE_REPORT_FILE = "compare-report.json";

    private final Path bundlePath;
    private List<DebugViewSnapshot> snapshots;
    private DebugSessionMetadata metadata;

    private SessionBundle(Path bundlePath) {
        this.bundlePath = bundlePath;
    }

    // --- Creation ---

    /** Create a new bundle directory. */
    public static SessionBundle create(Path bundlePath) throws IOException {
        Files.createDirectories(bundlePath);
        var bundle = new SessionBundle(bundlePath);
        bundle.metadata = new DebugSessionMetadata();
        return bundle;
    }

    /** Load an existing bundle from a directory. */
    public static SessionBundle load(Path bundlePath) throws IOException {
        var bundle = new SessionBundle(bundlePath);

        Path sessionFile = bundlePath.resolve(SESSION_FILE);
        if (Files.exists(sessionFile)) {
            bundle.snapshots = DebugSnapshotReplayLoader.load(sessionFile);
            LOG.info("Loaded " + bundle.snapshots.size() + " frames from " + sessionFile);
        } else {
            bundle.snapshots = List.of();
            LOG.warning("No session data in bundle: " + bundlePath);
        }

        Path metaFile = bundlePath.resolve(META_FILE);
        if (Files.exists(metaFile)) {
            bundle.metadata = DebugSessionMetadata.fromJson(Files.readString(metaFile));
            LOG.info("Loaded metadata: " + bundle.metadata.scenario() +
                " (" + bundle.metadata.bookmarks().size() + " bookmarks, " +
                bundle.metadata.windows().size() + " windows)");
        } else {
            bundle.metadata = new DebugSessionMetadata();
        }

        return bundle;
    }

    // --- Save ---

    /** Save metadata (bookmarks + windows) back to the bundle. */
    public void saveMetadata() throws IOException {
        Files.writeString(bundlePath.resolve(META_FILE), metadata.toJson());
    }

    /** Save a compare report to the bundle. */
    public void saveCompareReport(String reportJson) throws IOException {
        Files.writeString(bundlePath.resolve(COMPARE_REPORT_FILE), reportJson);
    }

    // --- Accessors ---

    public Path path() { return bundlePath; }
    public List<DebugViewSnapshot> snapshots() { return snapshots; }
    public DebugSessionMetadata metadata() { return metadata; }

    public boolean hasCompareReport() {
        return Files.exists(bundlePath.resolve(COMPARE_REPORT_FILE));
    }

    // --- Pack / Unpack (.dbgpack = zip) ---

    public static final String PACK_EXTENSION = ".dbgpack";

    /**
     * Pack this bundle directory into a single portable file.
     *
     * @param outputPath path for the .dbgpack file
     */
    public void pack(Path outputPath) throws IOException {
        try (var zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {
            packFile(zos, SESSION_FILE);
            packFile(zos, META_FILE);
            if (hasCompareReport()) {
                packFile(zos, COMPARE_REPORT_FILE);
            }
        }
        LOG.info("Packed bundle: " + outputPath + " (" + Files.size(outputPath) / 1024 + " KB)");
    }

    private void packFile(ZipOutputStream zos, String filename) throws IOException {
        Path file = bundlePath.resolve(filename);
        if (!Files.exists(file)) return;
        zos.putNextEntry(new ZipEntry(filename));
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /**
     * Unpack a .dbgpack file into a temporary directory and load it.
     */
    public static SessionBundle unpack(Path packFile) throws IOException {
        Path tempDir = Files.createTempDirectory("dbgpack-");
        try (var zis = new ZipInputStream(Files.newInputStream(packFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = tempDir.resolve(entry.getName());
                Files.copy(zis, target);
                zis.closeEntry();
            }
        }
        LOG.info("Unpacked bundle to: " + tempDir);
        return load(tempDir);
    }

    /**
     * Load from any path: .dbgpack file, bundle directory, or loose .ndjson.
     */
    public static SessionBundle loadAuto(Path path) throws IOException {
        if (path.toString().endsWith(PACK_EXTENSION)) {
            return unpack(path);
        }
        if (Files.isDirectory(path)) {
            return load(path);
        }
        // Loose file mode
        var bundle = new SessionBundle(path.getParent());
        bundle.snapshots = DebugSnapshotReplayLoader.load(path);

        Path metaPath = Path.of(path.toString().replace(".ndjson", ".meta.json"));
        if (Files.exists(metaPath)) {
            bundle.metadata = DebugSessionMetadata.fromJson(Files.readString(metaPath));
        } else {
            bundle.metadata = new DebugSessionMetadata();
        }
        return bundle;
    }
}
