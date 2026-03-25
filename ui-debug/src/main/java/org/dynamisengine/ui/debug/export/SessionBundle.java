package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public static final String MANIFEST_FILE = "manifest.json";
    public static final int FORMAT_VERSION = 1;

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
     * Pack this bundle directory into a single portable .dbgpack file
     * with a manifest containing checksums and format version.
     */
    public void pack(Path outputPath) throws IOException {
        // Compute checksums for all files
        var checksums = new LinkedHashMap<String, String>();
        var contents = new java.util.ArrayList<String>();
        for (String file : List.of(SESSION_FILE, META_FILE, COMPARE_REPORT_FILE)) {
            Path p = bundlePath.resolve(file);
            if (Files.exists(p)) {
                contents.add(file);
                checksums.put(file, sha256(p));
            }
        }

        // Build manifest
        String engineVersion = metadata != null ? metadata.engineVersion() : "unknown";
        String manifest = buildManifest(engineVersion, contents, checksums);

        try (var zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {
            // Write manifest first
            zos.putNextEntry(new ZipEntry(MANIFEST_FILE));
            zos.write(manifest.getBytes());
            zos.closeEntry();

            // Write data files
            for (String file : contents) {
                zos.putNextEntry(new ZipEntry(file));
                Files.copy(bundlePath.resolve(file), zos);
                zos.closeEntry();
            }
        }
        LOG.info("Packed bundle: " + outputPath + " (" + Files.size(outputPath) / 1024 + " KB, " + contents.size() + " files)");
    }

    /**
     * Unpack a .dbgpack file, validate manifest, and load.
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

        // Validate manifest if present
        Path manifestPath = tempDir.resolve(MANIFEST_FILE);
        if (Files.exists(manifestPath)) {
            validateManifest(tempDir, Files.readString(manifestPath));
        } else {
            LOG.warning("No manifest in bundle — skipping integrity check");
        }

        LOG.info("Unpacked bundle from: " + packFile);
        return load(tempDir);
    }

    // --- Manifest ---

    private static String buildManifest(String engineVersion,
                                         java.util.List<String> contents,
                                         Map<String, String> checksums) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format\": \"dbgpack\",\n");
        sb.append("  \"version\": ").append(FORMAT_VERSION).append(",\n");
        sb.append("  \"engineVersion\": \"").append(engineVersion).append("\",\n");
        sb.append("  \"createdAt\": \"").append(Instant.now()).append("\",\n");
        sb.append("  \"contents\": [");
        for (int i = 0; i < contents.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(contents.get(i)).append("\"");
        }
        sb.append("],\n");
        sb.append("  \"checksums\": {\n");
        int ci = 0;
        for (var entry : checksums.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\": \"sha256:")
              .append(entry.getValue()).append("\"");
            if (++ci < checksums.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n}");
        return sb.toString();
    }

    private static void validateManifest(Path dir, String manifestJson) {
        // Check version
        if (manifestJson.contains("\"version\":")) {
            int vIdx = manifestJson.indexOf("\"version\":") + 10;
            while (vIdx < manifestJson.length() && !Character.isDigit(manifestJson.charAt(vIdx))) vIdx++;
            int vEnd = vIdx;
            while (vEnd < manifestJson.length() && Character.isDigit(manifestJson.charAt(vEnd))) vEnd++;
            try {
                int version = Integer.parseInt(manifestJson.substring(vIdx, vEnd));
                if (version > FORMAT_VERSION) {
                    LOG.warning("Bundle format version " + version + " is newer than supported " + FORMAT_VERSION);
                }
            } catch (NumberFormatException ignored) {}
        }

        // Validate checksums
        int csStart = manifestJson.indexOf("\"checksums\":");
        if (csStart >= 0) {
            String csSection = manifestJson.substring(csStart);
            for (String file : List.of(SESSION_FILE, META_FILE, COMPARE_REPORT_FILE)) {
                String key = "\"" + file + "\": \"sha256:";
                int idx = csSection.indexOf(key);
                if (idx < 0) continue;
                idx += key.length();
                int end = csSection.indexOf("\"", idx);
                if (end < 0) continue;
                String expected = csSection.substring(idx, end);

                Path filePath = dir.resolve(file);
                if (Files.exists(filePath)) {
                    try {
                        String actual = sha256(filePath);
                        if (!expected.equals(actual)) {
                            LOG.warning("Checksum mismatch for " + file + ": expected " +
                                expected.substring(0, 12) + "... got " + actual.substring(0, 12) + "...");
                        }
                    } catch (IOException e) {
                        LOG.warning("Cannot verify checksum for " + file + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(file));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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
