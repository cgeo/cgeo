package cgeo.geocaching.connector.internal;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class InternalConnector extends AbstractConnector implements ISearchByGeocode {

    // prefix - must not contain regexp characters
    public static final String PREFIX = "ZZ";

    // predefined id (without prefix) and geocode (with prefix) for certain special caches:
    public static final int ID_HISTORY_CACHE = 0;
    public static final String GEOCODE_HISTORY_CACHE = geocodeFromId(ID_HISTORY_CACHE);

    // pattern for internal caches id
    @NonNull private static final Pattern PATTERN_GEOCODE = Pattern.compile("(" + PREFIX + ")[0-9A-Z]{1,4}", Pattern.CASE_INSENSITIVE);

    public static String geocodeFromId (final long id) {
        return PREFIX + id;
    }

    @Override
    @NonNull
    public String getName() {
        return "Internal";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return PREFIX;
    }

    @Override
    @Nullable
    public String getCacheUrl(@NonNull final Geocache cache) {
        return null;
    }

    @Override
    @NonNull
    public String getHost() {
        return StringUtils.EMPTY; // we have no host for these caches
    }

    @Override
    public boolean getHttps() {
        return false;
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return StringUtils.EMPTY; // we have no URL for these caches
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches();
    }

    @NonNull
    public boolean supportsNamechange() {
        // this connector supports changing the name of a geocache
        return true;
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final SearchResult search = new SearchResult();
        if (DataStore.isThere(geocode, guid, false)) {
            if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                Log.i("Loading old cache from cache.");
                search.addGeocode(DataStore.getGeocodeForGuid(guid));
            } else {
                search.addGeocode(geocode);
            }
            search.setError(StatusCode.NO_ERROR);
            return search;
        }

        search.setError(StatusCode.CACHE_NOT_FOUND);
        return search;
    }

    /**
     * creates a new cache with the given id, if it does not exist yet
     * @param context       context in which this function gets called
     * @param id            internal (numeric) id of the cache
     * @param name          cache's name (or null for default name)
     * @param description   cache's description (or null for default description)
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id
     */
    protected static void assertCacheExists(final Context context, final long id, @Nullable final String name, @Nullable final String description, @Nullable final Geopoint geopoint, final int listId) {
        final String geocode = geocodeFromId(id);
        if (DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB) == null) {
            final Geocache cache = new Geocache();
            cache.setGeocode(geocode);
            cache.setName(name == null ? String.format(context.getString(R.string.internal_cache_default_name), id) : name);
            cache.setOwnerDisplayName(context.getString(R.string.internal_cache_default_owner));
            cache.setDescription(description == null ? context.getString(R.string.internal_cache_default_description) : description);
            cache.setDetailed(true);
            cache.setType(CacheType.USER_DEFINED);
            final Set<Integer> lists = new HashSet<>(1);
            lists.add(StoredList.getConcreteList(listId));
            cache.setLists(lists);
            if (geopoint != null) {
                cache.setCoords(geopoint);
            }
            // add more fields if needed

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
        }
    }

    /**
     * makes sure that the pseudo cache storing the "go to" targets exists
     * @param context   context in which this function gets called
     */
    public static void assertHistoryCacheExists(final Context context) {
        assertCacheExists(context, ID_HISTORY_CACHE, context.getString(R.string.internal_goto_targets_title), context.getString(R.string.internal_goto_targets_description), null, PseudoList.UDC_LIST.id);
    }

    /**
     * creates a new cache for the internal connector
     * @param context       context in which this function gets called
     * @param name          cache's name (or null for default name)
     * @param description   cache's description (or null for default description)
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id
     * @return geocode      geocode of the newly created cache
     */
    public static String createCache(final Context context, @Nullable final String name, @Nullable final String description, @Nullable final Geopoint geopoint, final int listId) {
        final long newId = DataStore.incSequenceInternalCache();
        assertCacheExists(context, newId, name, description, geopoint, listId);
        return geocodeFromId(newId);
    }

    /**
     * asks user for cache name and creates a new cache if name has been entered
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id
     */
    public static void interactiveCreateCache(final Context context, final Geopoint geopoint, final int listId) {
        final EditText editText = new EditText(context);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        editText.setText("");

        new AlertDialog.Builder(context)
            .setTitle(R.string.create_internal_cache)
            .setView(editText)
            .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                final String geocode = createCache(context, editText.getText().toString(), null, geopoint, listId);
                CacheDetailActivity.startActivity(context, geocode);
            })
            .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> { })
            .show()
        ;
    }
}
