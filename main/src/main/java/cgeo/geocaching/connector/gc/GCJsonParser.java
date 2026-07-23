package cgeo.geocaching.connector.gc;

import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class for parsing JSON responses from GC.com APIs.
 * Provides shared parsing utilities to handle common patterns across different endpoints.
 */
public final class GCJsonParser {

    private static final SynchronizedDateFormat DATE_JSON = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"), Locale.US);
    private static final SynchronizedDateFormat DATE_JSON_SHORT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US);

    private GCJsonParser() {
        // Utility class
    }

    /**
     * Safely navigate through nested JSON object paths.
     *
     * @param node the starting JSON node
     * @param paths the path segments to navigate
     * @return the node at the specified path, or a missing node if path doesn't exist
     */
    public static JsonNode navigatePath(final JsonNode node, final String... paths) {
        JsonNode current = node;
        for (final String path : paths) {
            if (current == null || !current.isObject()) {
                return JsonUtils.reader.nullNode();
            }
            current = current.get(path);
        }
        return current;
    }

    /**
     * Extract a string value from a JSON node, with a safe fallback to empty string.
     *
     * @param node the JSON node
     * @param fieldName the field to extract
     * @return the string value, or empty string if not found or null
     */
    @Nullable
    public static String getStringOrNull(final JsonNode node, final String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }
        final JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText(null);
    }

    /**
     * Extract an integer value from a JSON node.
     *
     * @param node the JSON node
     * @param fieldName the field to extract
     * @param defaultValue default value if not found or invalid
     * @return the integer value, or defaultValue if not found
     */
    public static int getIntOrDefault(final JsonNode node, final String fieldName, final int defaultValue) {
        if (node == null || !node.isObject()) {
            return defaultValue;
        }
        final JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asInt(defaultValue);
    }

    /**
     * Extract a boolean value from a JSON node.
     *
     * @param node the JSON node
     * @param fieldName the field to extract
     * @param defaultValue default value if not found or invalid
     * @return the boolean value, or defaultValue if not found
     */
    public static boolean getBooleanOrDefault(final JsonNode node, final String fieldName, final boolean defaultValue) {
        if (node == null || !node.isObject()) {
            return defaultValue;
        }
        final JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asBoolean(defaultValue);
    }

    /**
     * Parse a timestamp string that may include fractional seconds.
     * Tries full format with milliseconds first, falls back to short format.
     *
     * @param timestamp the timestamp string (e.g., "2024-01-15T10:30:45.123Z" or "2024-01-15T10:30:45Z")
     * @return the parsed Date, or null if parsing fails
     */
    @Nullable
    public static Date parseTimestamp(final String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return null;
        }

        try {
            // Try parsing with fractional seconds
            return DATE_JSON.parse(timestamp);
        } catch (ParseException e1) {
            try {
                // Fallback to short format without fractional seconds
                return DATE_JSON_SHORT.parse(timestamp);
            } catch (ParseException e2) {
                Log.w("GCJsonParser.parseTimestamp: Failed to parse timestamp '" + timestamp + "'");
                return null;
            }
        }
    }
}
