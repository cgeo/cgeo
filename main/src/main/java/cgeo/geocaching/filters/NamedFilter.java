package cgeo.geocaching.filters;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * A named filter: a user-assigned name paired with a GeocacheFilter, an optional emoji marker,
 * and a flag indicating whether the marker should be shown on matching caches.
 *
 * <p>Named filters replace both the old GeocacheFilter.Storage (saved named filter configurations)
 * and ConditionalCacheMarker (per-filter emoji markers). They are stored in the DB (cg_filters)
 * and kept in a singleton in-memory list ordered by priority (index 0 = highest).</p>
 */
public class NamedFilter {


    public enum MarkerPriority {
        HIGHEST(100, R.string.named_filter_priority_highest),
        HIGH(200, R.string.named_filter_priority_high),
        NORMAL(300, R.string.named_filter_priority_normal),
        LOW(400, R.string.named_filter_priority_low),
        LOWEST(500, R.string.named_filter_priority_lowest);

        private final int value;
        @StringRes private final int resId;

        MarkerPriority(final int value, final int resId) {
            this.resId = resId;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public String getL10n() {
            return LocalizationUtils.getString(this.resId);
        }

        public static MarkerPriority fromValue(final int value) {
            for (final MarkerPriority mp : values()) {
                if (mp.value == value) {
                    return mp;
                }
            }
            return NORMAL; // default
        }
    }

    // JSON keys
    private static final String JSON_KEY_ID = "id";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_FILTER = "filter";
    private static final String JSON_KEY_MARKER_ID = "markerId";
    private static final String JSON_KEY_CONDITIONAL_MARKER_ACTIVE = "conditionalMarkerActive";
    private static final String JSON_KEY_CONDITIONAL_MARKER_PRIORITY = "conditionalMarkerPriority";

    // --- instance fields ---
    private int id = -1;
    private String name;
    private GeocacheFilter filter;
    @Nullable private String markerId;
    private boolean conditionalMarkerActive;
    private MarkerPriority conditionalMarkerPriority = MarkerPriority.NORMAL;

    // --- static in-memory list and ID generator ---
    private static boolean cacheLoaded = false; // tracks whether we've loaded from DB yet
    private static final Map<Integer, NamedFilter> namedFilters = new HashMap<>(); // lazy init
    private static final List<NamedFilter> namedFiltersSortedByName = new ArrayList<>();
    private static final List<NamedFilter> namedFiltersSortedByPrioAndName = new ArrayList<>();

    // injectable functions for testability
    private static Supplier<List<NamedFilter>> loadFunction = DataStore.DBFilters::loadAll;
    private static Consumer<List<NamedFilter>> storeFunction = DataStore.DBFilters::storeAll;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public NamedFilter(@NonNull final String name, @Nullable final GeocacheFilter filter) {
        this(name, filter, EmojiUtils.NO_EMOJI, false, MarkerPriority.NORMAL);
    }

    public NamedFilter(@NonNull final String name, @Nullable final GeocacheFilter filter,
                       @Nullable final String markerId, final boolean conditionalMarkerActive, final MarkerPriority conditionalMarkerPriority) {

        this.name = name;
        this.filter = filter;
        this.markerId = markerId;
        this.conditionalMarkerActive = conditionalMarkerActive;
        this.conditionalMarkerPriority = conditionalMarkerPriority == null ? MarkerPriority.NORMAL : conditionalMarkerPriority;
    }

    // -------------------------------------------------------------------------
    // Instance accessors
    // -------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public NamedFilter setId(final int id) {
        this.id = id;
        return this;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public NamedFilter setName(@NonNull final String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public GeocacheFilter getFilter() {
        return filter;
    }

    public NamedFilter setFilter(@Nullable final GeocacheFilter filter) {
        this.filter = filter;
        return this;
    }

    @Nullable
    public String getMarkerId() {
        return markerId;
    }

    public NamedFilter setMarkerId(@Nullable final String markerId) {
        this.markerId = markerId;
        return this;
    }

    public boolean isConditionalMarkerActive() {
        return conditionalMarkerActive;
    }

    public NamedFilter setConditionalMarkerActive(final boolean conditionalMarkerActive) {
        this.conditionalMarkerActive = conditionalMarkerActive;
        return this;
    }

    public MarkerPriority getConditionalMarkerPriority() {
        return conditionalMarkerPriority;
    }

    public NamedFilter setConditionalMarkerPriority(final MarkerPriority conditionalMarkerPriority) {
        this.conditionalMarkerPriority = conditionalMarkerPriority == null ? MarkerPriority.NORMAL : conditionalMarkerPriority;
        return this;
    }

    // -------------------------------------------------------------------------
    // User-displayable name
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable label combining emoji, name, and filter description as appropriate.
     */
    @NonNull
    public String getNameAndMarker() {
        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(markerId)) {
            sb.append(markerId).append(" ");
        }
        sb.append(name);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // JSON serialization
    // -------------------------------------------------------------------------

    @NonNull
    public String toConfig() {
        return JsonUtils.nodeToString(toJsonNode());
    }

    private ObjectNode toJsonNode() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setInt(node, JSON_KEY_ID, id);
        JsonUtils.setText(node, JSON_KEY_NAME, name);
        JsonUtils.set(node, JSON_KEY_FILTER, filter != null ? filter.toJson() : null);
        JsonUtils.setText(node, JSON_KEY_MARKER_ID, markerId);
        JsonUtils.setBoolean(node, JSON_KEY_CONDITIONAL_MARKER_ACTIVE, conditionalMarkerActive);
        JsonUtils.setInt(node, JSON_KEY_CONDITIONAL_MARKER_PRIORITY, conditionalMarkerPriority.getValue());
        return node;
    }

    @NonNull
    public static NamedFilter createFromConfig(@NonNull final String config) {
        final JsonNode node = JsonUtils.stringToNode(config);
        return createFromJsonNode(node);
    }

    private static NamedFilter createFromJsonNode(@NonNull final JsonNode node) {
        final int id = JsonUtils.getInt(node, JSON_KEY_ID, 0);
        final String name = JsonUtils.getText(node, JSON_KEY_NAME, "");
        final GeocacheFilter filter = GeocacheFilter.createFromJson(JsonUtils.get(node, JSON_KEY_FILTER));
        final String markerId = JsonUtils.getText(node, JSON_KEY_MARKER_ID, EmojiUtils.NO_EMOJI);
        final boolean conditionalMarkerActive = JsonUtils.getBoolean(node, JSON_KEY_CONDITIONAL_MARKER_ACTIVE, false);
        final MarkerPriority conditionalMarkerPriority = MarkerPriority.fromValue(JsonUtils.getInt(node, JSON_KEY_CONDITIONAL_MARKER_PRIORITY, -1));
        return new NamedFilter(name != null ? name : "", filter, markerId, conditionalMarkerActive, conditionalMarkerPriority).setId(id);
    }

    public static List<NamedFilter> createFromListConfig(final String config) {
        final List<NamedFilter> result = new ArrayList<>();
        final JsonNode array = JsonUtils.stringToNode(config);
        for (final JsonNode node : array) {
            result.add(createFromJsonNode(node));
        }
        return result;
    }

    public static String toListConfig(final Collection<NamedFilter> filters) {
        final ArrayNode array = JsonUtils.createArrayNode();
        for (final NamedFilter nf : filters) {
            array.add(nf.toJsonNode());
        }
        return JsonUtils.nodeToString(array);
    }

    // -------------------------------------------------------------------------
    // Static management
    // -------------------------------------------------------------------------

    /** Lazily loads cache. */
    private static void ensureCache() {
        if (!cacheLoaded) {
            initializeCacheFrom(loadFunction.get());
            cacheLoaded = true;
        }
    }

    private static void initializeCacheFrom(final Collection<NamedFilter> filters) {
        namedFilters.clear();
        namedFiltersSortedByName.clear();
        namedFiltersSortedByPrioAndName.clear();
        for (final NamedFilter nf : filters) {
            namedFilters.put(nf.id, nf);
            namedFiltersSortedByName.add(nf);
            namedFiltersSortedByPrioAndName.add(nf);
        }
        namedFiltersSortedByName.sort(CommonUtils.getTextSortingComparator(f -> f.name)); // sort by name for display
        namedFiltersSortedByPrioAndName.sort(CommonUtils.getTextSortingComparator(f -> String.format(Locale.US, "%03d", f.conditionalMarkerPriority.value) + ":" + f.name)); // sort by name for display
    }

    /** Returns an unmodifiable view of all named filters in priority order. */
    @NonNull
    public static synchronized List<NamedFilter> getAll() {
        ensureCache();
        return Collections.unmodifiableList(namedFiltersSortedByName);
    }

    /** Returns an unmodifiable view of all named filters in priority order. */
    @NonNull
    public static synchronized List<NamedFilter> getAllDeepCopy() {
        ensureCache();
        final List<NamedFilter> copy = new ArrayList<>();
        for (final NamedFilter nf : namedFiltersSortedByName) {
            copy.add(NamedFilter.createFromConfig(nf.toConfig()));
        }
        return copy;
    }


    /** Find a filter by its integer id; returns null if not found. */
    @Nullable
    public static synchronized NamedFilter getById(final int id) {
        ensureCache();
        return namedFilters.get(id);
    }

    /** Find any filter by its name; returns null if not found; returns any filter if multiple exists with that name */
    @Nullable
    public static synchronized NamedFilter getFirstByName(final String name) {
        ensureCache();
        for (final NamedFilter nf : namedFiltersSortedByName) {
            if (Strings.CI.equals(name, nf.name)) {
                return nf;
            }
        }
        return null;
    }

    /** Case-insensitive check whether a filter with the given name exists. */
    public static synchronized boolean nameExists(@NonNull final String name) {
        ensureCache();
        for (final NamedFilter nf : namedFiltersSortedByName) {
            if (nf.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first stored filter whose underlying filter produces the same results as the given filter,
     * or null if none matches.
     */
    @Nullable
    public static synchronized NamedFilter filterConfigExists(@Nullable final GeocacheFilter filter) {
        ensureCache();
        for (final NamedFilter nf : namedFiltersSortedByName) {
            if (GeocacheFilter.filtersSame(nf.filter, filter)) {
                return nf;
            }
        }
        return null;
    }

    /**
     * Replaces the in-memory list with {@code newList}, persists via the store function, and broadcasts
     * {@link GeocacheChangedBroadcastReceiver#NAMED_FILTER_CHANGED}.
     */
    public static synchronized void storeAll(@NonNull final Collection<NamedFilter> newList) {
        //safe guard!
        if (newList == namedFiltersSortedByName || newList == namedFiltersSortedByPrioAndName) {
            throw new IllegalArgumentException("Cannot pass internal list directly; make a copy first");
        }

        initializeCacheFrom(newList);
        storeFunction.accept(namedFiltersSortedByName);
        GeocacheChangedBroadcastReceiver.sendBroadcast(GeocacheChangedBroadcastReceiver.NAMED_FILTER_CHANGED);
    }


    public static synchronized NamedFilter addNew(final String name, final GeocacheFilter filter) {
        final NamedFilter newFilter = new NamedFilter(name, filter);
        final List<NamedFilter> newList = getAllDeepCopy();
        newList.add(newFilter);
        storeAll(newList);
        return namedFilters.get(newFilter.getId());
    }

    public static synchronized void activateMarker(final Collection<NamedFilter> filters) {
        final Set<Integer> filterIds = filters.stream().map(NamedFilter::getId).collect(Collectors.toSet());
        final List<NamedFilter> newList = getAllDeepCopy();
        for (final NamedFilter nf : newList) {
            nf.setConditionalMarkerActive(filterIds.contains(nf.getId()));
        }
        storeAll(newList);
    }

    /**
     * Returns a pair of lists for the given cache:
     * <ul>
     *   <li>first = active matches: {@code conditionalMarkerActive == true} AND filter matches</li>
     *   <li>second = passive matches: {@code conditionalMarkerActive == false} AND filter matches</li>
     * </ul>
     * Returns an empty pair if {@code cache} is null.
     */
    @NonNull
    public static synchronized ImmutablePair<List<NamedFilter>, List<NamedFilter>> getFiltersMatchingCache(@Nullable final Geocache cache) {
        if (cache == null) {
            return new ImmutablePair<>(Collections.emptyList(), Collections.emptyList());
        }
        ensureCache();
        final List<NamedFilter> active = new ArrayList<>();
        final List<NamedFilter> passive = new ArrayList<>();
        for (final NamedFilter nf : namedFiltersSortedByPrioAndName) {
            final boolean matches = filterMatches(nf, cache);
            if (matches) {
                if (nf.conditionalMarkerActive) {
                    active.add(nf);
                } else {
                    passive.add(nf);
                }
            }
        }
        return new ImmutablePair<>(active, passive);
    }

    /**
     * Returns the marker ids (emoji code-points) of active matching filters for the given cache.
     * Uses the first list from {@link #getFiltersMatchingCache(Geocache)}.
     */
    @NonNull
    public static List<String> getMarkersForCache(@Nullable final Geocache cache) {
        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> pair = getFiltersMatchingCache(cache);
        final List<String> result = new ArrayList<>();
        for (final NamedFilter nf : pair.left) {
            if (StringUtils.isNotBlank(nf.markerId)) {
                result.add(nf.markerId);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Testability injection
    // -------------------------------------------------------------------------

    /** FOR UNIT/INSTR-TEST ONLY: inject a load function for unit testing (bypasses DataStore). Pass null to reset to default. */
    public static void resetStorageForTesting(@Nullable final List<NamedFilter> filterStorage) {
        loadFunction = filterStorage == null ? DataStore.DBFilters::loadAll : () -> filterStorage;
        storeFunction = filterStorage == null ? DataStore.DBFilters::storeAll : list -> {
            filterStorage.clear();
            filterStorage.addAll(list);
        };
        cacheLoaded = false; // reset lazy cache
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static boolean filterMatches(@NonNull final NamedFilter nf, @NonNull final Geocache cache) {
        if (nf.filter == null || !nf.filter.isFiltering()) {
            return true; // null/empty filter matches all caches
        }
        return nf.filter.filter(cache);
    }
}
