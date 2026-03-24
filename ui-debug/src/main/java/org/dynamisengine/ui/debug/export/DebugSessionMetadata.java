package org.dynamisengine.ui.debug.export;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for a recorded debug session: context about the recording,
 * plus user bookmarks and annotations.
 *
 * <p>Serialized as a sidecar JSON file alongside the NDJSON recording:
 * <ul>
 *   <li>{@code debug-session.ndjson} — frame data</li>
 *   <li>{@code debug-session.meta.json} — this metadata</li>
 * </ul>
 */
public final class DebugSessionMetadata {

    // Session info
    private String engineVersion = "unknown";
    private String backend = "unknown";
    private String scenario = "";
    private String recordedAt = Instant.now().toString();
    private int frameCount;
    private long firstTick;
    private long lastTick;
    private float durationSeconds;

    // Bookmarks
    private final List<Bookmark> bookmarks = new ArrayList<>();

    // Investigation windows (saved named ranges)
    private final List<InvestigationWindow> windows = new ArrayList<>();

    // --- Accessors ---

    public String engineVersion() { return engineVersion; }
    public void setEngineVersion(String v) { this.engineVersion = v; }

    public String backend() { return backend; }
    public void setBackend(String b) { this.backend = b; }

    public String scenario() { return scenario; }
    public void setScenario(String s) { this.scenario = s; }

    public String recordedAt() { return recordedAt; }
    public void setRecordedAt(String t) { this.recordedAt = t; }

    public int frameCount() { return frameCount; }
    public void setFrameCount(int c) { this.frameCount = c; }

    public long firstTick() { return firstTick; }
    public void setFirstTick(long t) { this.firstTick = t; }

    public long lastTick() { return lastTick; }
    public void setLastTick(long t) { this.lastTick = t; }

    public float durationSeconds() { return durationSeconds; }
    public void setDurationSeconds(float d) { this.durationSeconds = d; }

    public List<Bookmark> bookmarks() { return bookmarks; }

    public List<InvestigationWindow> windows() { return windows; }

    public void addWindow(String name, int startFrame, int endFrame) {
        windows.add(new InvestigationWindow(name, startFrame, endFrame));
    }

    public void addWindow(String name, int startFrame, int endFrame, String note) {
        windows.add(new InvestigationWindow(name, startFrame, endFrame, note));
    }

    public void addBookmark(int frameIndex, String label) {
        bookmarks.add(new Bookmark(frameIndex, label));
    }

    public void addBookmark(int frameIndex, String label, String note) {
        bookmarks.add(new Bookmark(frameIndex, label, note));
    }

    /**
     * A bookmarked frame with label and optional free-text note.
     *
     * @param frameIndex frame index (0-based)
     * @param label      short label for display (e.g. "spike", "recovery")
     * @param note       longer free-text note explaining why this frame matters
     */
    public record Bookmark(int frameIndex, String label, String note) {
        /** Backwards-compatible constructor without note. */
        public Bookmark(int frameIndex, String label) {
            this(frameIndex, label, "");
        }
    }

    // --- Serialization ---

    public String toJson() {
        var sb = new StringBuilder(512);
        sb.append("{\n");
        appendKV(sb, "engineVersion", engineVersion); sb.append(",\n");
        appendKV(sb, "backend", backend); sb.append(",\n");
        appendKV(sb, "scenario", scenario); sb.append(",\n");
        appendKV(sb, "recordedAt", recordedAt); sb.append(",\n");
        sb.append("  \"frameCount\": ").append(frameCount).append(",\n");
        sb.append("  \"firstTick\": ").append(firstTick).append(",\n");
        sb.append("  \"lastTick\": ").append(lastTick).append(",\n");
        sb.append("  \"durationSeconds\": ").append(durationSeconds).append(",\n");

        sb.append("  \"bookmarks\": [");
        for (int i = 0; i < bookmarks.size(); i++) {
            if (i > 0) sb.append(",");
            var bm = bookmarks.get(i);
            sb.append("\n    {\"frameIndex\": ").append(bm.frameIndex())
              .append(", \"label\": \"").append(escape(bm.label()))
              .append("\", \"note\": \"").append(escape(bm.note())).append("\"}");
        }
        if (!bookmarks.isEmpty()) sb.append("\n  ");
        sb.append("],\n");

        sb.append("  \"windows\": [");
        for (int i = 0; i < windows.size(); i++) {
            if (i > 0) sb.append(",");
            var w = windows.get(i);
            sb.append("\n    {\"name\": \"").append(escape(w.name()))
              .append("\", \"startFrame\": ").append(w.startFrame())
              .append(", \"endFrame\": ").append(w.endFrame())
              .append(", \"note\": \"").append(escape(w.note())).append("\"}");
        }
        if (!windows.isEmpty()) sb.append("\n  ");
        sb.append("]\n}");
        return sb.toString();
    }

    public static DebugSessionMetadata fromJson(String json) {
        var meta = new DebugSessionMetadata();
        meta.setEngineVersion(extractString(json, "engineVersion"));
        meta.setBackend(extractString(json, "backend"));
        meta.setScenario(extractString(json, "scenario"));
        meta.setRecordedAt(extractString(json, "recordedAt"));
        meta.setFrameCount(extractInt(json, "frameCount"));
        meta.setFirstTick(extractLong(json, "firstTick"));
        meta.setLastTick(extractLong(json, "lastTick"));
        meta.setDurationSeconds(extractFloat(json, "durationSeconds"));

        // Parse bookmarks
        int bmStart = json.indexOf("\"bookmarks\":");
        if (bmStart >= 0) {
            String bmSection = json.substring(bmStart);
            int arrStart = bmSection.indexOf('[');
            int arrEnd = bmSection.indexOf(']');
            if (arrStart >= 0 && arrEnd > arrStart) {
                String arr = bmSection.substring(arrStart, arrEnd + 1);
                int pos = 0;
                while ((pos = arr.indexOf("{", pos)) >= 0) {
                    int end = arr.indexOf("}", pos);
                    if (end < 0) break;
                    String obj = arr.substring(pos, end + 1);
                    int fi = extractInt(obj, "frameIndex");
                    String label = extractString(obj, "label");
                    String note = extractString(obj, "note");
                    meta.addBookmark(fi, label, note);
                    pos = end + 1;
                }
            }
        }

        // Parse windows
        int wStart = json.indexOf("\"windows\":");
        if (wStart >= 0) {
            String wSection = json.substring(wStart);
            int arrStart = wSection.indexOf('[');
            int arrEnd = wSection.indexOf(']');
            if (arrStart >= 0 && arrEnd > arrStart) {
                String arr = wSection.substring(arrStart, arrEnd + 1);
                int pos = 0;
                while ((pos = arr.indexOf("{", pos)) >= 0) {
                    int end = arr.indexOf("}", pos);
                    if (end < 0) break;
                    String obj = arr.substring(pos, end + 1);
                    String name = extractString(obj, "name");
                    int sf = extractInt(obj, "startFrame");
                    int ef = extractInt(obj, "endFrame");
                    String note = extractString(obj, "note");
                    meta.addWindow(name, sf, ef, note);
                    pos = end + 1;
                }
            }
        }

        return meta;
    }

    // --- Helpers ---

    private static void appendKV(StringBuilder sb, String key, String value) {
        sb.append("  \"").append(key).append("\": \"").append(escape(value)).append("\"");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\": \"";
        int idx = json.indexOf(search);
        if (idx < 0) { search = "\"" + key + "\":\""; idx = json.indexOf(search); }
        if (idx < 0) return "";
        idx += search.length();
        int end = json.indexOf("\"", idx);
        return end > idx ? json.substring(idx, end) : "";
    }

    private static int extractInt(String json, String key) {
        return (int) extractLong(json, key);
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\": ";
        int idx = json.indexOf(search);
        if (idx < 0) { search = "\"" + key + "\":"; idx = json.indexOf(search); }
        if (idx < 0) return 0;
        idx += search.length();
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(idx, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static float extractFloat(String json, String key) {
        String search = "\"" + key + "\": ";
        int idx = json.indexOf(search);
        if (idx < 0) { search = "\"" + key + "\":"; idx = json.indexOf(search); }
        if (idx < 0) return 0f;
        idx += search.length();
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Float.parseFloat(json.substring(idx, end)); }
        catch (NumberFormatException e) { return 0f; }
    }
}
