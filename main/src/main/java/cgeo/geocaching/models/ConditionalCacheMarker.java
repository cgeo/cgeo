package cgeo.geocaching.models;

import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents a single conditional cache marker rule: a filter paired with an emoji marker.
 * When the filter matches a cache, the marker is shown on that cache's icon.
 * This class is immutable — create a new instance to change values.
 */
public class ConditionalCacheMarker {

    private static final String JSON_KEY_MARKER = "marker";
    private static final String JSON_KEY_FILTER = "filter";

    private final int markerId;
    @Nullable private final GeocacheFilter filter;

    public ConditionalCacheMarker(final int markerId, @Nullable final GeocacheFilter filter) {
        this.markerId = markerId;
        this.filter = filter;
    }

    public int getMarkerId() {
        return markerId;
    }

    @Nullable
    public GeocacheFilter getFilter() {
        return filter;
    }

    /** Serialises this rule to a JSON object node. A null filter is stored as an absent field */
    @NonNull
    public ObjectNode toJson() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setInt(node, JSON_KEY_MARKER, markerId);
        // null filter stored as absent field via JsonUtils.setText null-handling
        JsonUtils.setText(node, JSON_KEY_FILTER, filter != null ? filter.toConfig() : null);
        return node;
    }

    /** Deserialises a rule from a JSON node */
    @NonNull
    public static ConditionalCacheMarker fromJson(@NonNull final JsonNode node) {
        final int markerId = JsonUtils.getInt(node, JSON_KEY_MARKER, EmojiUtils.NO_EMOJI);
        final String filterConfigStr = JsonUtils.getText(node, JSON_KEY_FILTER, null);
        final GeocacheFilter filter = filterConfigStr != null
                ? GeocacheFilter.createFromConfig(filterConfigStr)
                : null;
        return new ConditionalCacheMarker(markerId, filter);
    }

    /**
     * Returns the list of conditional marker IDs (emoji code-points) that apply to the given cache.
     * Returns an empty list if cache is null.
     */
    @NonNull
    public static List<Integer> getMarkersForCache(@Nullable final Geocache cache) {
        return getMarkersForRules(cache, Settings.getConditionalCacheMarkers());
    }

    /**
     * Package-private helper for testing: evaluates rules against a cache without reading from Settings.
     * Rules are evaluated top-to-bottom. Rules with markerId == EmojiUtils.NO_EMOJI are skipped.
     * A null or empty (non-filtering) filter matches all caches. Returns an empty list if cache is null.
     */
    @NonNull
    static List<Integer> getMarkersForRules(@Nullable final Geocache cache, @NonNull final List<ConditionalCacheMarker> rules) {
        if (cache == null) {
            return Collections.emptyList();
        }
        final List<Integer> result = new ArrayList<>();
        for (final ConditionalCacheMarker rule : rules) {
            // null or empty (non-filtering) filter means "match all caches"
            final boolean matches = rule.markerId != EmojiUtils.NO_EMOJI &&
                (rule.filter == null || !rule.filter.isFiltering() || rule.filter.filter(cache));
            if (matches) {
                result.add(rule.markerId);
            }
        }
        return result;
    }
}

