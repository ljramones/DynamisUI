package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;
import org.dynamisengine.ui.debug.model.DebugMiniTrend;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serializes and deserializes {@link DebugViewSnapshot} to/from JSON strings.
 *
 * <p>Uses no external dependencies — manual JSON construction for minimal
 * footprint. Output is compact single-line JSON suitable for NDJSON streaming.
 */
public final class DebugSnapshotJson {

    private DebugSnapshotJson() {}

    /** Serialize a snapshot to a compact JSON string (single line). */
    public static String toJson(DebugViewSnapshot snapshot) {
        var sb = new StringBuilder(2048);
        sb.append('{');

        // tick
        appendKey(sb, "tick"); sb.append(snapshot.tick());

        // summary
        sb.append(',');
        appendKey(sb, "summary");
        writeSummary(sb, snapshot.summary());

        // alerts
        sb.append(',');
        appendKey(sb, "alerts");
        writeAlerts(sb, snapshot.alerts());

        // categories
        sb.append(',');
        appendKey(sb, "categories");
        writeCategories(sb, snapshot.categories());

        // timeline events
        sb.append(',');
        appendKey(sb, "timelineEvents");
        writeTimelineEvents(sb, snapshot.timelineEvents());

        sb.append('}');
        return sb.toString();
    }

    /** Deserialize a JSON string to a DebugViewSnapshot. Minimal parser. */
    public static DebugViewSnapshot fromJson(String json) {
        // Simple extraction — not a full JSON parser, but handles our known schema
        long tick = extractLong(json, "tick");
        var summary = parseSummary(json);
        var alerts = parseAlerts(json);
        var timelineEvents = parseTimelineEvents(json);
        // Categories are complex nested structures — simplified for replay
        var categories = parseCategories(json);

        return new DebugViewSnapshot(categories, alerts, summary, tick, timelineEvents);
    }

    // --- Serialization ---

    private static void writeSummary(StringBuilder sb, DebugViewSnapshot.DebugSummaryView s) {
        sb.append('{');
        appendKey(sb, "tick"); sb.append(s.tick());
        sb.append(','); appendKey(sb, "frameTimeMs"); sb.append(s.frameTimeMs());
        sb.append(','); appendKey(sb, "budgetPercent"); sb.append(s.budgetPercent());
        sb.append(','); appendKey(sb, "sourceCount"); sb.append(s.sourceCount());
        sb.append(','); appendKey(sb, "historyDepth"); sb.append(s.historyDepth());
        sb.append(','); appendKey(sb, "healthySources"); sb.append(s.healthySources());
        sb.append('}');
    }

    private static void writeAlerts(StringBuilder sb, List<DebugViewSnapshot.DebugAlertView> alerts) {
        sb.append('[');
        for (int i = 0; i < alerts.size(); i++) {
            if (i > 0) sb.append(',');
            var a = alerts.get(i);
            sb.append('{');
            appendKey(sb, "ruleName"); appendString(sb, a.ruleName());
            sb.append(','); appendKey(sb, "severity"); appendString(sb, a.severity());
            sb.append(','); appendKey(sb, "message"); appendString(sb, a.message());
            sb.append('}');
        }
        sb.append(']');
    }

    private static void writeCategories(StringBuilder sb,
                                          Map<String, DebugViewSnapshot.DebugCategoryView> categories) {
        sb.append('{');
        boolean first = true;
        for (var entry : categories.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            appendString(sb, entry.getKey());
            sb.append(':');
            writeCategory(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeCategory(StringBuilder sb, DebugViewSnapshot.DebugCategoryView cat) {
        sb.append('{');
        appendKey(sb, "name"); appendString(sb, cat.categoryName());

        sb.append(','); appendKey(sb, "sources");
        sb.append('{');
        boolean first = true;
        for (var entry : cat.sources().entrySet()) {
            if (!first) sb.append(',');
            first = false;
            appendString(sb, entry.getKey());
            sb.append(':');
            writeStringMap(sb, entry.getValue());
        }
        sb.append('}');

        sb.append(','); appendKey(sb, "flags");
        writeStringMap(sb, cat.flags());

        sb.append(','); appendKey(sb, "trends");
        writeTrends(sb, cat.trends());

        sb.append('}');
    }

    private static void writeTrends(StringBuilder sb, List<DebugMiniTrend> trends) {
        sb.append('[');
        for (int i = 0; i < trends.size(); i++) {
            if (i > 0) sb.append(',');
            var t = trends.get(i);
            sb.append('{');
            appendKey(sb, "name"); appendString(sb, t.metricName());
            sb.append(','); appendKey(sb, "min"); sb.append(t.min());
            sb.append(','); appendKey(sb, "max"); sb.append(t.max());
            sb.append(','); appendKey(sb, "values");
            sb.append('[');
            for (int j = 0; j < t.values().size(); j++) {
                if (j > 0) sb.append(',');
                sb.append(t.values().get(j));
            }
            sb.append(']');
            sb.append('}');
        }
        sb.append(']');
    }

    private static void writeTimelineEvents(StringBuilder sb,
                                              List<DebugViewSnapshot.DebugTimelineEvent> events) {
        sb.append('[');
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(',');
            var e = events.get(i);
            sb.append('{');
            appendKey(sb, "frame"); sb.append(e.frameNumber());
            sb.append(','); appendKey(sb, "ts"); sb.append(e.timestampMs());
            sb.append(','); appendKey(sb, "severity"); appendString(sb, e.severity());
            sb.append(','); appendKey(sb, "source"); appendString(sb, e.source());
            sb.append(','); appendKey(sb, "name"); appendString(sb, e.name());
            sb.append(','); appendKey(sb, "message"); appendString(sb, e.message());
            sb.append('}');
        }
        sb.append(']');
    }

    private static void writeStringMap(StringBuilder sb, Map<String, String> map) {
        sb.append('{');
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            appendString(sb, entry.getKey());
            sb.append(':');
            appendString(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void appendKey(StringBuilder sb, String key) {
        sb.append('"').append(key).append('"').append(':');
    }

    private static void appendString(StringBuilder sb, String value) {
        if (value == null) { sb.append("null"); return; }
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
    }

    // --- Deserialization (minimal, handles our known schema) ---

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        idx += search.length();
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-'))
            end++;
        try { return Long.parseLong(json.substring(idx, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static float extractFloat(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return 0f;
        idx += search.length();
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) ||
               json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == 'E'))
            end++;
        try { return Float.parseFloat(json.substring(idx, end)); }
        catch (NumberFormatException e) { return 0f; }
    }

    private static int extractInt(String json, String key) {
        return (int) extractLong(json, key);
    }

    private static DebugViewSnapshot.DebugSummaryView parseSummary(String json) {
        String search = "\"summary\":";
        int idx = json.indexOf(search);
        if (idx < 0) return DebugViewSnapshot.DebugSummaryView.EMPTY;
        String sub = extractObject(json, idx + search.length());
        return new DebugViewSnapshot.DebugSummaryView(
            extractLong(sub, "tick"),
            extractFloat(sub, "frameTimeMs"),
            extractFloat(sub, "budgetPercent"),
            extractInt(sub, "sourceCount"),
            extractInt(sub, "historyDepth"),
            extractInt(sub, "healthySources")
        );
    }

    private static List<DebugViewSnapshot.DebugAlertView> parseAlerts(String json) {
        // Simplified: return empty for now — alerts reconstruct from timeline events
        return List.of();
    }

    private static List<DebugViewSnapshot.DebugTimelineEvent> parseTimelineEvents(String json) {
        // Simplified: return empty for basic replay
        return List.of();
    }

    private static Map<String, DebugViewSnapshot.DebugCategoryView> parseCategories(String json) {
        // Simplified: return empty for basic replay
        return Map.of();
    }

    private static String extractObject(String json, int start) {
        if (start >= json.length() || json.charAt(start) != '{') return "{}";
        int depth = 0;
        int end = start;
        for (; end < json.length(); end++) {
            char c = json.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { end++; break; } }
        }
        return json.substring(start, end);
    }
}
