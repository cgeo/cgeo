// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.internal

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.databinding.DialogTitleButtonButtonBinding
import cgeo.geocaching.databinding.UdcCreateBinding
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapMarkerUtils

import android.content.Context
import android.view.LayoutInflater
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.EnumSet
import java.util.HashSet
import java.util.Random
import java.util.Set
import java.util.regex.Pattern

import com.google.android.material.button.MaterialButton
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class InternalConnector : AbstractConnector() : ISearchByGeocode {

    // prefix - must not contain regexp characters
    public static val PREFIX: String = "ZZ"
    private static val GEOCODE_CHARS: String = "0123456789ABCDEFGHJKLMNPQRTUVWXYZ"
    private static val GEOCODE_LENGTH: Int = 4

    // predefined id (without prefix) and geocode (with prefix) for certain special caches:
    public static val GEOCODE_HISTORY_CACHE: String = PREFIX + "0"

    // store into UDC list
    public static val UDC_LIST: Int = -1

    // pattern for internal caches id
    private static val PATTERN_GEOCODE: Pattern = Pattern.compile("(" + PREFIX + ")[0-9A-Z]{1,10}", Pattern.CASE_INSENSITIVE)

    private InternalConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static val INSTANCE: InternalConnector = InternalConnector()
    }

    public static InternalConnector getInstance() {
        return Holder.INSTANCE
    }

    override     public String getName() {
        return "Internal"
    }

    override     public String getDisplayName() {
        return LocalizationUtils.getStringWithFallback(R.string.internal_connector_name, "User Defined Caches")
    }

    override     public String getNameAbbreviated() {
        return PREFIX
    }

    override     public Boolean isActive() {
        return true
    }

    override     public String getCacheUrl(final Geocache cache) {
        return null
    }

    override     public String getHost() {
        return StringUtils.EMPTY; // we have no host for these caches
    }

    override     public Boolean isHttps() {
        return false
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     protected String getCacheUrlPrefix() {
        return StringUtils.EMPTY; // we have no URL for these caches
    }

    override     public Boolean canHandle(final String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches()
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{PREFIX + "%"}
    }


    override     public Int getCacheMapMarkerId() {
        return R.drawable.marker_other
    }

    override     public Int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_other
    }

    override     public Int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_other
    }

    override     public Int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_other
    }

    override     public Boolean supportsNamechange() {
        // this connector supports changing the name of a geocache
        return true
    }

    override     public Boolean supportsDescriptionchange() {
        return true
    }

    override     public Boolean supportsSettingFoundState() {
        return true
    }

    override     public Boolean supportsDifficultyTerrain() {
        return false
    }

    override     public SearchResult searchByGeocode(final String geocode, final String guid, final DisposableHandler handler) {

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage)

        val search: SearchResult = SearchResult()
        if (DataStore.isThere(geocode, guid, false)) {
            if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                Log.i("Loading old cache from cache.")
                search.addGeocode(DataStore.getGeocodeForGuid(guid))
            } else {
                search.addGeocode(geocode)
            }
            search.setError(this, StatusCode.NO_ERROR)
            return search
        }

        search.setError(this, StatusCode.CACHE_NOT_FOUND)
        return search
    }

    /* creates user-defined cache list if requested */
    protected static Int getUdcListId(final Context context) {
        for (StoredList list : DataStore.getLists()) {
            if (list.getTitle().trim() == (context.getString(R.string.goto_targets_list).trim())) {
                return list.id
            }
        }

        Int newListId = DataStore.createList(context.getString(R.string.goto_targets_list))
        if (newListId == -1) {
            // fallback for errors during list creation
            newListId = StoredList.STANDARD_LIST_ID
        }
        return newListId
    }

    /**
     * creates a cache with the given id, if it does not exist yet
     *
     * @param context       context in which this function gets called
     * @param geocode       internal id of the cache
     * @param name          cache's name (or null for default name)
     * @param description   cache's description (or null for default description)
     * @param assignedEmoji cache's assigned emoji (or 0 for default cache type icon)
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id, may be NEW_LIST
     */
    private static Unit assertCacheExists(final Context context, final String geocode, final String name, final String description, final Int assignedEmoji, final Geopoint geopoint, final Int listId) {
        if (DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB) == null) {

            Int newListId = listId
            if (listId == UDC_LIST) {
                newListId = getUdcListId(context)
            }

            // create cache
            val cache: Geocache = Geocache()
            cache.setGeocode(geocode)
            cache.setName(StringUtils.isEmpty(name) ? String.format(context.getString(R.string.internal_cache_default_names), geocode) : name)
            cache.setOwnerDisplayName(context.getString(R.string.internal_cache_default_owner))
            cache.setDescription(StringUtils.isEmpty(description) ? context.getString(R.string.internal_cache_default_description) : description)
            cache.setAssignedEmoji(assignedEmoji)
            cache.setDetailed(true)
            cache.setType(CacheType.USER_DEFINED)
            val lists: Set<Integer> = HashSet<>(1)
            lists.add(StoredList.getConcreteList(newListId))
            cache.setLists(lists)
            if (geopoint != null) {
                cache.setCoords(geopoint)
            }
            // add more fields if needed

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB))
        }
    }

    /**
     * makes sure that the pseudo cache storing the "go to" targets exists
     *
     * @param context context in which this function gets called
     */
    public static Unit assertHistoryCacheExists(final Context context) {
        assertCacheExists(context, GEOCODE_HISTORY_CACHE, context.getString(R.string.internal_goto_targets_title), context.getString(R.string.internal_goto_targets_description), 0, null, UDC_LIST)
    }

    /**
     * creates a cache for the internal connector
     *
     * @param context       context in which this function gets called
     * @param name          cache's name (or null for default name)
     * @param description   cache's description (or null for default description)
     * @param assignedEmoji cache's assigned emoji (or 0 for default cache type icon)
     * @param geopoint      cache's current location (or null if none)
     * @param listId        cache list's id
     * @return geocode      geocode of the newly created cache
     */
    public static String createCache(final Context context, final String name, final String description, final Int assignedEmoji, final Geopoint geopoint, final Int listId) {
        String geocode
        do {
            geocode = PREFIX + generateRandomId()
        } while (DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB) != null)
        assertCacheExists(context, geocode, name, description, assignedEmoji, geopoint, listId)
        return geocode
    }

    public static String generateRandomId() {
        val random: Random = Random()
        val sb: StringBuilder = StringBuilder(GEOCODE_LENGTH)
        for (Int i = 0; i < GEOCODE_LENGTH; i++) {
            sb.append(GEOCODE_CHARS.charAt(random.nextInt(GEOCODE_CHARS.length())))
        }
        return sb.toString()
    }

    /**
     * asks user for cache name and creates a cache if name has been entered
     *
     * @param geopoint cache's current location (or null if none)
     * @param listId   cache list's id (either InternalConnector.UDC_LIST or interpreted as offline list id)
     * @param askUser  false: store in given list / true: ask for list & default list (if offline list given)
     *                 default list is InternalConnector.UDC_LIST
     */
    public static Unit interactiveCreateCache(final Context context, final Geopoint geopoint, final Int listId, final Boolean askUser) {
        val showStoreInCurrentList: Boolean = askUser && ((listId == StoredList.STANDARD_LIST_ID || listId >= DataStore.customListIdOffset))

        val temporaryCache: Geocache = Geocache()
        temporaryCache.setType(CacheType.USER_DEFINED)

        val binding: UdcCreateBinding = UdcCreateBinding.inflate(LayoutInflater.from(context))
        binding.name.setText("")
        binding.givenList.setVisibility(showStoreInCurrentList ? View.VISIBLE : View.GONE)
        binding.givenList.setChecked(Settings.getCreateUDCuseGivenList())

        val titleViewBinding: DialogTitleButtonButtonBinding = DialogTitleButtonButtonBinding.inflate(LayoutInflater.from(context))
        titleViewBinding.dialogTitleTitle.setText(R.string.create_internal_cache)
        val dialogButton: MaterialButton = (MaterialButton) titleViewBinding.dialogButtonRight
        dialogButton.setVisibility(View.VISIBLE)
        // This cross-converting solves a tinting issue described in #11715. Sorry, it is ugly but the only possibility we have found so far.
        dialogButton.setIcon(ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getCacheMarker(context.getResources(), temporaryCache, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable())))
        dialogButton.setIconTint(null)
        dialogButton.setHint(R.string.caches_set_cache_icon)
        dialogButton.setOnClickListener(v -> EmojiUtils.selectEmojiPopup(context, temporaryCache.getAssignedEmoji(), temporaryCache, assignedEmoji -> {
            temporaryCache.setAssignedEmoji(assignedEmoji)
            dialogButton.setIcon(MapMarkerUtils.getCacheMarker(context.getResources(), temporaryCache, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable())
        }))

        Dialogs.newBuilder(context)
                .setCustomTitle(titleViewBinding.getRoot())
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    val useGivenList: Boolean = binding.givenList.isChecked()
                    Settings.setCreateUDCuseGivenList(useGivenList)
                    val geocode: String = createCache(context, ViewUtils.getEditableText(binding.name.getText()), null, temporaryCache.getAssignedEmoji(), geopoint, showStoreInCurrentList && !useGivenList ? InternalConnector.UDC_LIST : listId)
                    CacheDetailActivity.startActivity(context, geocode)
                    GeocacheChangedBroadcastReceiver.sendBroadcast(context, geocode)
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel())
                .show()
        Keyboard.show(context, binding.name)
    }
}
