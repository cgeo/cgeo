package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    // JSON keys
    private static final String JSON_KEY_ID = "id";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_FILTER = "filter";
    private static final String JSON_KEY_MARKER_ID = "markerId";
    private static final String JSON_KEY_CONDITIONAL_MARKER_ACTIVE = "conditionalMarkerActive";

    // --- instance fields ---
    private final int id;
    private String name;
    private GeocacheFilter filter;
    private int markerId;
    private boolean conditionalMarkerActive;

    // --- static in-memory list and ID generator ---
    private static boolean namedFiltersLoaded = false; // tracks whether we've loaded from DB yet
    private static final List<NamedFilter> namedFilters = new ArrayList<>(); // lazy init
    private static final List<NamedFilter> namedFiltersUnmodifiable = Collections.unmodifiableList(namedFilters);
    private static final AtomicInteger nextId = new AtomicInteger(1);

    // injectable functions for testability
    private static Supplier<List<NamedFilter>> loadFunction = DataStore.DBFilters::loadAll;
    private static Consumer<List<NamedFilter>> storeFunction = DataStore.DBFilters::storeAll;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Public constructor for direct construction (deserialization, testing).
     * For user-facing creation of new filters use {@link #addNew(String, GeocacheFilter)}.
     */
    public NamedFilter(final int id, @NonNull final String name, @Nullable final GeocacheFilter filter,
                       final int markerId, final boolean conditionalMarkerActive) {
        this.id = id;
        this.name = name;
        this.filter = filter;
        this.markerId = markerId;
        this.conditionalMarkerActive = conditionalMarkerActive;
    }

    // -------------------------------------------------------------------------
    // Instance accessors
    // -------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull final String name) {
        this.name = name;
    }

    @Nullable
    public GeocacheFilter getFilter() {
        return filter;
    }

    public void setFilter(@Nullable final GeocacheFilter filter) {
        this.filter = filter;
    }

    public int getMarkerId() {
        return markerId;
    }

    public void setMarkerId(final int markerId) {
        this.markerId = markerId;
    }

    public boolean isConditionalMarkerActive() {
        return conditionalMarkerActive;
    }

    public void setConditionalMarkerActive(final boolean conditionalMarkerActive) {
        this.conditionalMarkerActive = conditionalMarkerActive;
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
        if (markerId != EmojiUtils.NO_EMOJI) {
            sb.append(EmojiUtils.getEmojiAsString(markerId)).append(" ");
        }
        sb.append(name);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // JSON serialization
    // -------------------------------------------------------------------------

    @NonNull
    public ObjectNode toJson() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setInt(node, JSON_KEY_ID, id);
        JsonUtils.setText(node, JSON_KEY_NAME, name);
        JsonUtils.setText(node, JSON_KEY_FILTER, filter != null ? filter.toConfig() : null);
        JsonUtils.setInt(node, JSON_KEY_MARKER_ID, markerId);
        JsonUtils.setBoolean(node, JSON_KEY_CONDITIONAL_MARKER_ACTIVE, conditionalMarkerActive);
        return node;
    }

    @NonNull
    public static NamedFilter fromJson(@NonNull final JsonNode node) {
        final int id = JsonUtils.getInt(node, JSON_KEY_ID, 0);
        final String name = JsonUtils.getText(node, JSON_KEY_NAME, "");
        final String filterConfig = JsonUtils.getText(node, JSON_KEY_FILTER, null);
        final GeocacheFilter filter = filterConfig != null ? GeocacheFilter.createFromConfig(filterConfig) : null;
        final int markerId = JsonUtils.getInt(node, JSON_KEY_MARKER_ID, EmojiUtils.NO_EMOJI);
        final boolean conditionalMarkerActive = JsonUtils.getBoolean(node, JSON_KEY_CONDITIONAL_MARKER_ACTIVE, false);
        return new NamedFilter(id, name != null ? name : "", filter, markerId, conditionalMarkerActive);
    }

    // -------------------------------------------------------------------------
    // Static management
    // -------------------------------------------------------------------------

    /** Lazily loads and returns the in-memory list. Must be called within synchronized block. */
    private static List<NamedFilter> ensureList() {
        if (!namedFiltersLoaded) {
            namedFilters.clear();
            namedFilters.addAll(loadFunction.get());
            namedFiltersLoaded = true;
            // Initialize nextId from max existing id
            int maxId = 0;
            for (final NamedFilter nf : namedFilters) {
                if (nf.id > maxId) {
                    maxId = nf.id;
                }
            }
            nextId.set(maxId + 1);
        }
        return namedFilters;
    }

    /** Returns an unmodifiable view of all named filters in priority order. */
    @NonNull
    public static synchronized List<NamedFilter> getAll() {
        ensureList();
        return namedFiltersUnmodifiable;
    }

    /** Returns a modifable deep copy of all named filters in priority order. */
    @NonNull
    public static synchronized List<NamedFilter> getAllDeepCopy() {
        final List<NamedFilter> result = new ArrayList<>();
        ensureList();
        for (final NamedFilter nf : namedFilters) {
            result.add(fromJson(nf.toJson()));
        }
        return result;
    }

    /** Find a filter by its integer id; returns null if not found. */
    @Nullable
    public static synchronized NamedFilter getById(final int id) {
        for (final NamedFilter nf : ensureList()) {
            if (nf.id == id) {
                return nf;
            }
        }
        return null;
    }

    /** Find any filter by its name; returns null if not found; returns any filter if multiple exists with that name */
    @Nullable
    public static synchronized NamedFilter getFirstByName(final String name) {
        for (final NamedFilter nf : ensureList()) {
            if (Strings.CI.equals(name, nf.name)) {
                return nf;
            }
        }
        return null;
    }

    /** Case-insensitive check whether a filter with the given name exists. */
    public static synchronized boolean nameExists(@NonNull final String name) {
        for (final NamedFilter nf : ensureList()) {
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
        for (final NamedFilter nf : ensureList()) {
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
    public static synchronized void storeAll(@NonNull final List<NamedFilter> newList) {
        if (namedFilters != newList) {
            namedFilters.clear();
            namedFilters.addAll(newList);
        }
        storeFunction.accept(namedFilters);
        GeocacheChangedBroadcastReceiver.sendBroadcast(GeocacheChangedBroadcastReceiver.NAMED_FILTER_CHANGED);
    }

    /**
     * Creates a new filter with an auto-generated id, inserts it at position 0 (highest priority),
     * persists, and broadcasts.
     */
    @NonNull
    public static synchronized NamedFilter addNew(@NonNull final String name, @Nullable final GeocacheFilter filter) {
        final List<NamedFilter> newList = ensureList();
        final int id = nextId.getAndIncrement();
        final NamedFilter nf = new NamedFilter(id, name, filter, EmojiUtils.NO_EMOJI, false);
        newList.add(0, nf);
        storeAll(newList);
        return nf;
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
        final List<NamedFilter> active = new ArrayList<>();
        final List<NamedFilter> passive = new ArrayList<>();
        for (final NamedFilter nf : ensureList()) {
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
    public static List<Integer> getMarkersForCache(@Nullable final Geocache cache) {
        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> pair = getFiltersMatchingCache(cache);
        final List<Integer> result = new ArrayList<>();
        for (final NamedFilter nf : pair.left) {
            if (nf.markerId != EmojiUtils.NO_EMOJI) {
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
        namedFiltersLoaded = false; // reset lazy cache
        nextId.set(1); // reset ids
    }

    /** FOR UNIT/INSTR-TEST ONLY: inject a load function for unit testing (bypasses DataStore). Pass null to reset to default. */
    private static void setLoadFunctionForTesting(@Nullable final Supplier<List<NamedFilter>> fn) {
        loadFunction = fn != null ? fn : DataStore.DBFilters::loadAll;
        namedFiltersLoaded = false; // reset lazy cache
    }

    /** FOR UNIT/INSTR-TEST ONLY: inject a store function for unit testing (bypasses DataStore). Pass null to reset to default. */
    private static void setStoreFunctionForTesting(@Nullable final Consumer<List<NamedFilter>> fn) {
        storeFunction = fn != null ? fn : DataStore.DBFilters::storeAll;
    }

    /** FOR UNIT/INSTR-TEST ONLY: reset the static state (for testing only). */
    private static void resetForTesting() {
        namedFiltersLoaded = false;
        nextId.set(1);
        loadFunction = DataStore.DBFilters::loadAll;
        storeFunction = DataStore.DBFilters::storeAll;
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
