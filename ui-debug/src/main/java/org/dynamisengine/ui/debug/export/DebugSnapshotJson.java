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

    public static final int SCHEMA_VERSION = 1;

    /** Serialize a snapshot to a compact JSON string (single line). */
    public static String toJson(DebugViewSnapshot snapshot) {
        var sb = new StringBuilder(2048);
        sb.append('{');

        // version
        appendKey(sb, "v"); sb.append(SCHEMA_VERSION);

        // tick
        sb.append(',');
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

    /**
     * Deserialize a JSON string to a DebugViewSnapshot.
     * Full round-trip: all serialized fields are deserialized.
     *
     * @throws IllegalArgumentException if schema version is unsupported
     */
    public static DebugViewSnapshot fromJson(String json) {
        int version = extractInt(json, "v");
        if (version > SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                "Unsupported schema version " + version + " (max supported: " + SCHEMA_VERSION + ")");
        }

        long tick = extractLong(json, "tick");
        var summary = parseSummary(json);
        var alerts = parseAlerts(json);
        var timelineEvents = parseTimelineEvents(json);
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
        var alerts = new java.util.ArrayList<DebugViewSnapshot.DebugAlertView>();
        String search = "\"alerts\":[";
        int idx = json.indexOf(search);
        if (idx < 0) return alerts;
        String arr = extractArray(json, idx + search.length() - 1);
        for (String obj : splitArrayObjects(arr)) {
            alerts.add(new DebugViewSnapshot.DebugAlertView(
                extractString(obj, "ruleName"),
                extractString(obj, "severity"),
                extractString(obj, "message"),
                "", ""
            ));
        }
        return alerts;
    }

    private static List<DebugViewSnapshot.DebugTimelineEvent> parseTimelineEvents(String json) {
        var events = new java.util.ArrayList<DebugViewSnapshot.DebugTimelineEvent>();
        String search = "\"timelineEvents\":[";
        int idx = json.indexOf(search);
        if (idx < 0) return events;
        String arr = extractArray(json, idx + search.length() - 1);
        for (String obj : splitArrayObjects(arr)) {
            events.add(new DebugViewSnapshot.DebugTimelineEvent(
                extractLong(obj, "frame"),
                extractLong(obj, "ts"),
                extractString(obj, "severity"),
                extractString(obj, "source"),
                extractString(obj, "name"),
                extractString(obj, "message")
            ));
        }
        return events;
    }

    private static Map<String, DebugViewSnapshot.DebugCategoryView> parseCategories(String json) {
        var categories = new java.util.LinkedHashMap<String, DebugViewSnapshot.DebugCategoryView>();
        String search = "\"categories\":";
        int idx = json.indexOf(search);
        if (idx < 0) return categories;
        String catsObj = extractObject(json, idx + search.length());
        if (catsObj.length() <= 2) return categories; // "{}"

        // Parse each key-value pair in the categories object
        int pos = 1; // skip opening '{'
        while (pos < catsObj.length()) {
            // Find key
            int keyStart = catsObj.indexOf('"', pos);
            if (keyStart < 0) break;
            int keyEnd = catsObj.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = catsObj.substring(keyStart + 1, keyEnd);

            // Find value object
            int colonPos = catsObj.indexOf(':', keyEnd);
            if (colonPos < 0) break;
            String valueObj = extractObject(catsObj, colonPos + 1);

            categories.put(key, parseCategoryView(valueObj));
            pos = colonPos + 1 + valueObj.length();
            // Skip comma
            while (pos < catsObj.length() && catsObj.charAt(pos) == ',') pos++;
        }
        return categories;
    }

    private static DebugViewSnapshot.DebugCategoryView parseCategoryView(String obj) {
        String name = extractString(obj, "name");

        // Parse sources: {"source1":{"key":"val",...},...}
        var sources = new java.util.LinkedHashMap<String, Map<String, String>>();
        String srcSearch = "\"sources\":";
        int srcIdx = obj.indexOf(srcSearch);
        if (srcIdx >= 0) {
            String srcObj = extractObject(obj, srcIdx + srcSearch.length());
            sources.putAll(parseNestedStringMaps(srcObj));
        }

        // Parse flags: {"flag1":"val",...}
        var flags = new java.util.LinkedHashMap<String, String>();
        String flagSearch = "\"flags\":";
        int flagIdx = obj.indexOf(flagSearch);
        if (flagIdx >= 0) {
            String flagObj = extractObject(obj, flagIdx + flagSearch.length());
            flags.putAll(parseStringMap(flagObj));
        }

        // Parse trends
        var trends = new java.util.ArrayList<DebugMiniTrend>();
        String trendSearch = "\"trends\":[";
        int trendIdx = obj.indexOf(trendSearch);
        if (trendIdx >= 0) {
            String trendArr = extractArray(obj, trendIdx + trendSearch.length() - 1);
            for (String tObj : splitArrayObjects(trendArr)) {
                String metricName = extractString(tObj, "name");
                double min = extractFloat(tObj, "min");
                double max = extractFloat(tObj, "max");
                var values = parseDoubleArray(tObj);
                trends.add(new DebugMiniTrend(metricName, min, max, values));
            }
        }

        return new DebugViewSnapshot.DebugCategoryView(name, sources, flags, trends);
    }

    // --- Parsing helpers ---

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        idx += search.length();
        var sb = new StringBuilder();
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    default -> sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String extractObject(String json, int start) {
        // Skip whitespace
        while (start < json.length() && json.charAt(start) != '{') start++;
        if (start >= json.length()) return "{}";
        int depth = 0;
        boolean inString = false;
        int end = start;
        for (; end < json.length(); end++) {
            char c = json.charAt(end);
            if (c == '"' && (end == 0 || json.charAt(end - 1) != '\\')) inString = !inString;
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) { end++; break; } }
            }
        }
        return json.substring(start, end);
    }

    private static String extractArray(String json, int start) {
        while (start < json.length() && json.charAt(start) != '[') start++;
        if (start >= json.length()) return "[]";
        int depth = 0;
        boolean inString = false;
        int end = start;
        for (; end < json.length(); end++) {
            char c = json.charAt(end);
            if (c == '"' && (end == 0 || json.charAt(end - 1) != '\\')) inString = !inString;
            if (!inString) {
                if (c == '[') depth++;
                else if (c == ']') { depth--; if (depth == 0) { end++; break; } }
            }
        }
        return json.substring(start, end);
    }

    /** Split a JSON array of objects into individual object strings. */
    private static List<String> splitArrayObjects(String arr) {
        var result = new java.util.ArrayList<String>();
        int pos = 1; // skip '['
        while (pos < arr.length()) {
            while (pos < arr.length() && arr.charAt(pos) != '{') pos++;
            if (pos >= arr.length()) break;
            String obj = extractObject(arr, pos);
            result.add(obj);
            pos += obj.length();
            while (pos < arr.length() && arr.charAt(pos) == ',') pos++;
        }
        return result;
    }

    private static Map<String, String> parseStringMap(String obj) {
        var map = new java.util.LinkedHashMap<String, String>();
        if (obj.length() <= 2) return map;
        int pos = 1;
        while (pos < obj.length()) {
            int keyStart = obj.indexOf('"', pos);
            if (keyStart < 0) break;
            int keyEnd = obj.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = obj.substring(keyStart + 1, keyEnd);
            int colonPos = obj.indexOf(':', keyEnd);
            if (colonPos < 0) break;
            int valStart = obj.indexOf('"', colonPos);
            if (valStart < 0) break;
            String val = extractString(obj.substring(colonPos), key.isEmpty() ? "\"" : key);
            // Simpler: re-extract from the substring
            val = extractStringAt(obj, valStart);
            map.put(key, val);
            pos = valStart + val.length() + 2; // skip past value + quotes
        }
        return map;
    }

    private static String extractStringAt(String json, int quoteStart) {
        var sb = new StringBuilder();
        for (int i = quoteStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                sb.append(json.charAt(++i));
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Map<String, Map<String, String>> parseNestedStringMaps(String obj) {
        var result = new java.util.LinkedHashMap<String, Map<String, String>>();
        if (obj.length() <= 2) return result;
        int pos = 1;
        while (pos < obj.length()) {
            int keyStart = obj.indexOf('"', pos);
            if (keyStart < 0) break;
            int keyEnd = obj.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = obj.substring(keyStart + 1, keyEnd);
            int colonPos = obj.indexOf(':', keyEnd);
            if (colonPos < 0) break;
            String valueObj = extractObject(obj, colonPos + 1);
            result.put(key, parseStringMap(valueObj));
            pos = colonPos + 1 + valueObj.length();
            while (pos < obj.length() && obj.charAt(pos) == ',') pos++;
        }
        return result;
    }

    private static List<Double> parseDoubleArray(String obj) {
        var values = new java.util.ArrayList<Double>();
        String search = "\"values\":[";
        int idx = obj.indexOf(search);
        if (idx < 0) return values;
        idx += search.length();
        var sb = new StringBuilder();
        for (int i = idx; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == ']') break;
            if (c == ',') {
                if (!sb.isEmpty()) {
                    try { values.add(Double.parseDouble(sb.toString())); }
                    catch (NumberFormatException ignored) {}
                    sb.setLength(0);
                }
            } else if (c != ' ') {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            try { values.add(Double.parseDouble(sb.toString())); }
            catch (NumberFormatException ignored) {}
        }
        return values;
    }
}
