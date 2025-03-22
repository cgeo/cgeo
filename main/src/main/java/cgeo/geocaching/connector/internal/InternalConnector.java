package cgeo.geocaching.connector.internal;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.databinding.DialogTitleButtonButtonBinding;
import cgeo.geocaching.databinding.UdcCreateBinding;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class InternalConnector extends AbstractConnector implements ISearchByGeocode {

    // prefix - must not contain regexp characters
    public static final String PREFIX = "ZZ";
    private static final String GEOCODE_CHARS = "0123456789ABCDEFGHJKLMNPQRTUVWXYZ";
    private static final int GEOCODE_LENGTH = 4;

    // predefined id (without prefix) and geocode (with prefix) for certain special caches:
    public static final String GEOCODE_HISTORY_CACHE = PREFIX + "0";

    // store into UDC list
    public static final int UDC_LIST = -1;

    // pattern for internal caches id
    @NonNull private static final Pattern PATTERN_GEOCODE = Pattern.compile("(" + PREFIX + ")[0-9A-Z]{1,10}", Pattern.CASE_INSENSITIVE);

    private InternalConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final InternalConnector INSTANCE = new InternalConnector();
    }

    @NonNull
    public static InternalConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    @NonNull
    public String getName() {
        return "Internal";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return LocalizationUtils.getStringWithFallback(R.string.internal_connector_name, "User Defined Caches");
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return PREFIX;
    }

    @Override
    public boolean isActive() {
        return true;
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
    public boolean isHttps() {
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

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{PREFIX + "%"};
    }


    @Override
    public int getCacheMapMarkerId() {
        return R.drawable.marker_other;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_other;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_other;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_other;
    }

    @Override
    public boolean supportsNamechange() {
        // this connector supports changing the name of a geocache
        return true;
    }

    @Override
    public boolean supportsDescriptionchange() {
        return true;
    }

    @Override
    public boolean supportsSettingFoundState() {
        return true;
    }

    @Override
    public boolean supportsDifficultyTerrain() {
        return false;
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
            search.setError(this, StatusCode.NO_ERROR);
            return search;
        }

        search.setError(this, StatusCode.CACHE_NOT_FOUND);
        return search;
    }

    /* creates user-defined cache list if requested */
    protected static int getUdcListId(final Context context) {
        for (StoredList list : DataStore.getLists()) {
            if (list.getTitle().trim().equals(context.getString(R.string.goto_targets_list).trim())) {
                return list.id;
            }
        }

        int newListId = DataStore.createList(context.getString(R.string.goto_targets_list));
        if (newListId == -1) {
            // fallback for errors during new list creation
            newListId = StoredList.STANDARD_LIST_ID;
        }
        return newListId;
    }

    /**
     * creates a new cache with the given id, if it does not exist yet
     *
     * @param context       context in which this function gets called
     * @param geocode       internal id of the cache
     * @param name          cache's name (or null for default name)
     * @param description   cache's description (or null for default description)
     * @param assignedEmoji cache's assigned emoji (or 0 for default cache type icon)
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id, may be NEW_LIST
     */
    private static void assertCacheExists(final Context context, final String geocode, @Nullable final String name, @Nullable final String description, final int assignedEmoji, @Nullable final Geopoint geopoint, final int listId) {
        if (DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB) == null) {

            int newListId = listId;
            if (listId == UDC_LIST) {
                newListId = getUdcListId(context);
            }

            // create new cache
            final Geocache cache = new Geocache();
            cache.setGeocode(geocode);
            cache.setName(StringUtils.isEmpty(name) ? String.format(context.getString(R.string.internal_cache_default_names), geocode) : name);
            cache.setOwnerDisplayName(context.getString(R.string.internal_cache_default_owner));
            cache.setDescription(StringUtils.isEmpty(description) ? context.getString(R.string.internal_cache_default_description) : description);
            cache.setAssignedEmoji(assignedEmoji);
            cache.setDetailed(true);
            cache.setType(CacheType.USER_DEFINED);
            final Set<Integer> lists = new HashSet<>(1);
            lists.add(StoredList.getConcreteList(newListId));
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
     *
     * @param context context in which this function gets called
     */
    public static void assertHistoryCacheExists(final Context context) {
        assertCacheExists(context, GEOCODE_HISTORY_CACHE, context.getString(R.string.internal_goto_targets_title), context.getString(R.string.internal_goto_targets_description), 0, null, UDC_LIST);
    }

    /**
     * creates a new cache for the internal connector
     *
     * @param context       context in which this function gets called
     * @param name          cache's name (or null for default name)
     * @param description   cache's description (or null for default description)
     * @param assignedEmoji cache's assigned emoji (or 0 for default cache type icon)
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id
     * @return geocode      geocode of the newly created cache
     */
    public static String createCache(final Context context, @Nullable final String name, @Nullable final String description, final int assignedEmoji, @Nullable final Geopoint geopoint, final int listId) {
        String geocode;
        do {
            geocode = PREFIX + generateRandomId();
        } while (DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB) != null);
        assertCacheExists(context, geocode, name, description, assignedEmoji, geopoint, listId);
        return geocode;
    }

    public static String generateRandomId() {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(GEOCODE_LENGTH);
        for (int i = 0; i < GEOCODE_LENGTH; i++) {
            sb.append(GEOCODE_CHARS.charAt(random.nextInt(GEOCODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * asks user for cache name and creates a new cache if name has been entered
     *
     * @param geopoint cache's current location (or null if none)
     * @param listId   cache list's id (either InternalConnector.UDC_LIST or interpreted as offline list id)
     * @param askUser  false: store in given list / true: ask for list & default list (if offline list given)
     *                 default list is InternalConnector.UDC_LIST
     */
    public static void interactiveCreateCache(final Context context, final Geopoint geopoint, final int listId, final boolean askUser) {
        final boolean showStoreInCurrentList = askUser && ((listId == StoredList.STANDARD_LIST_ID || listId >= DataStore.customListIdOffset));

        final Geocache temporaryCache = new Geocache();
        temporaryCache.setType(CacheType.USER_DEFINED);

        final UdcCreateBinding binding = UdcCreateBinding.inflate(LayoutInflater.from(context));
        binding.name.setText("");
        binding.givenList.setVisibility(showStoreInCurrentList ? View.VISIBLE : View.GONE);
        binding.givenList.setChecked(Settings.getCreateUDCuseGivenList());

        final DialogTitleButtonButtonBinding titleViewBinding = DialogTitleButtonButtonBinding.inflate(LayoutInflater.from(context));
        titleViewBinding.dialogTitleTitle.setText(R.string.create_internal_cache);
        final MaterialButton dialogButton = (MaterialButton) titleViewBinding.dialogButtonRight;
        dialogButton.setVisibility(View.VISIBLE);
        // This cross-converting solves a tinting issue described in #11715. Sorry, it is ugly but the only possibility we have found so far.
        dialogButton.setIcon(ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getCacheMarker(context.getResources(), temporaryCache, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable())));
        dialogButton.setIconTint(null);
        dialogButton.setHint(R.string.caches_set_cache_icon);
        dialogButton.setOnClickListener(v -> EmojiUtils.selectEmojiPopup(context, temporaryCache.getAssignedEmoji(), temporaryCache, assignedEmoji -> {
            temporaryCache.setAssignedEmoji(assignedEmoji);
            dialogButton.setIcon(MapMarkerUtils.getCacheMarker(context.getResources(), temporaryCache, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable());
        }));

        Dialogs.newBuilder(context)
                .setCustomTitle(titleViewBinding.getRoot())
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    final boolean useGivenList = binding.givenList.isChecked();
                    Settings.setCreateUDCuseGivenList(useGivenList);
                    final String geocode = createCache(context, ViewUtils.getEditableText(binding.name.getText()), null, temporaryCache.getAssignedEmoji(), geopoint, showStoreInCurrentList && !useGivenList ? InternalConnector.UDC_LIST : listId);
                    CacheDetailActivity.startActivity(context, geocode);
                    GeocacheChangedBroadcastReceiver.sendBroadcast(context, geocode);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel())
                .show();
        Keyboard.show(context, binding.name);
    }
}
