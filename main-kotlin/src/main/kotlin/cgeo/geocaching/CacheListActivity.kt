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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.AbstractListActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.FilteredActivity
import cgeo.geocaching.activity.Progress
import cgeo.geocaching.apps.cachelist.CacheListAppUtils
import cgeo.geocaching.apps.cachelist.CacheListApps
import cgeo.geocaching.apps.cachelist.ListNavigationSelectionActionProvider
import cgeo.geocaching.apps.navi.NavigationAppFactory
import cgeo.geocaching.command.AbstractCachesCommand
import cgeo.geocaching.command.CopyToListCommand
import cgeo.geocaching.command.DeleteListCommand
import cgeo.geocaching.command.MakeListUniqueCommand
import cgeo.geocaching.command.MoveToListAndRemoveFromOthersCommand
import cgeo.geocaching.command.MoveToListCommand
import cgeo.geocaching.command.RenameListCommand
import cgeo.geocaching.command.SetCacheIconCommand
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.gc.BookmarkListActivity
import cgeo.geocaching.connector.gc.BookmarkUtils
import cgeo.geocaching.connector.gc.PocketQueryListActivity
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.export.BatchUploadModifiedCoordinates
import cgeo.geocaching.export.FieldNoteExport
import cgeo.geocaching.export.GpxExport
import cgeo.geocaching.export.PersonalNoteExport
import cgeo.geocaching.files.GPXImporter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.OriginGeocacheFilter
import cgeo.geocaching.filters.gui.GeocacheFilterActivity
import cgeo.geocaching.list.ListNameMemento
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.loaders.AbstractSearchLoader
import cgeo.geocaching.loaders.CoordsGeocacheListLoader
import cgeo.geocaching.loaders.FinderGeocacheListLoader
import cgeo.geocaching.loaders.GCListLoader
import cgeo.geocaching.loaders.KeywordGeocacheListLoader
import cgeo.geocaching.loaders.LiveFilterGeocacheListLoader
import cgeo.geocaching.loaders.NextPageGeocacheListLoader
import cgeo.geocaching.loaders.NullGeocacheListLoader
import cgeo.geocaching.loaders.OfflineGeocacheListLoader
import cgeo.geocaching.loaders.OwnerGeocacheListLoader
import cgeo.geocaching.loaders.SearchFilterGeocacheListLoader
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LoggingUI
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.models.GCList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.DownloadProgress
import cgeo.geocaching.network.Send2CgeoDownloader
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.service.CacheDownloaderService
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.sorting.GeocacheSortContext
import cgeo.geocaching.sorting.VisitComparator
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.ContentStorageActivityHelper
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.CacheListActionBarChooser
import cgeo.geocaching.ui.CacheListAdapter
import cgeo.geocaching.ui.FastScrollListener
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ToggleItemType
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.WeakReferenceHandler
import cgeo.geocaching.ui.dialog.CheckboxDialogConfig
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.FilterUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.MenuUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.WatchListUtils
import cgeo.geocaching.utils.functions.Action1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Pair
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.util.Consumer
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Set
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.collections4.IterableUtils
import org.apache.commons.lang3.StringUtils


class CacheListActivity : AbstractListActivity() : FilteredActivity, LoaderManager.LoaderCallbacks<SearchResult> {

    private static val CACHE_LOADER_ID: Int = 5; //arbitrary number, but must be fixed
    private static val EXTRAS_NEXTPAGE: String = "extras_nextpage"

    private static val REFRESH_WARNING_THRESHOLD: Int = 100

    private static val REQUEST_CODE_LOG: Int = 1001
    private static val REQUEST_CODE_IMPORT_PQ: Int = 10003
    private static val REQUEST_CODE_IMPORT_BOOKMARK: Int = 10004

    private static val STATE_GEOCACHE_FILTER: String = "currentGeocacheFilter"
    private static val STATE_SORT_CONTEXT: String = "currentSortContext"
    private static val STATE_LIST_TYPE: String = "currentListType"
    private static val STATE_TYPE_PARAMETERS: String = "currentTypeParameters"
    private static val STATE_LIST_ID: String = "currentListId"
    private static val STATE_MARKER_ID: String = "currentMarkerId"
    private static val STATE_PREVENTASKFORDELETION: String = "preventAskForDeletion"
    private static val STATE_CONTENT_STORAGE_ACTIVITY_HELPER: String = "contentStorageActivityHelper"
    private static val STATE_OFFLINELISTLOADLIMIT_ID: String = "offlineListLoadLimit"
    private static val STATE_CHECKFOREMPTYLIST: String = "checkForEmptyList"

    private static val BUNDLE_ACTION_KEY: String = "afterLoadAction"

    private var type: CacheListType = null
    private val typeParameters: Bundle = Bundle()
    private var coords: Geopoint = null
    private GeocacheSortContext sortContext
    private var search: SearchResult = null
    private var checkForEmtpyList: Boolean = true

    private val actionBarChooser: CacheListActionBarChooser =
        CacheListActionBarChooser(this, this::getSupportActionBar, this::switchListById)
    private var adapter: CacheListAdapter = null
    private var listFooter: View = null
    private var listFooterLine1: TextView = null
    private var listFooterLine2: TextView = null
    private val progress: Progress = Progress()
    private var title: String = ""
    private val detailProgress: AtomicInteger = AtomicInteger(0)
    private var listId: Int = StoredList.TEMPORARY_LIST.id; // Only meaningful for the OFFLINE type
    private var markerId: Int = EmojiUtils.NO_EMOJI
    private var preventAskForDeletion: Boolean = false
    private var offlineListLoadLimit: Int = getOfflineListInitialLoadLimit()

    /**
     * remember current filter when switching between lists, so it can be re-applied afterwards
     */
    private var currentCacheFilter: GeocacheFilterContext = null
    private var currentAddFilterCriteria: IGeocacheFilter = null

    private val geoDirHandler: GeoDirHandler = GeoDirHandler() {

        override         public Unit updateDirection(final Float direction) {
            if (Settings.isLiveList()) {
                adapter.setActualHeading(AngleUtils.getDirectionNow(direction))
            }
        }

        override         public Unit updateGeoData(final GeoData geoData) {
            adapter.setActualCoordinates(geoData.getCoords())
        }

    }

    private ContextMenuInfo lastMenuInfo
    private var contextMenuGeocode: String = ""
    private val resumeDisposables: CompositeDisposable = CompositeDisposable()
    private val listNameMemento: ListNameMemento = ListNameMemento()

    private val clearOfflineLogsHandler: DisposableHandler = ClearOfflineLogsHandler(this)
    private val importGpxAttachementFinishedHandler: Handler = ImportGpxAttachementFinishedHandler(this)

    private var contentStorageActivityHelper: ContentStorageActivityHelper = null

    private AbstractSearchLoader currentLoader

    override     public Int getSelectedBottomItemId() {
        return type.navigationMenuItem
    }

    override     public Unit onNavigationItemReselected(final MenuItem item) {
        if (item.getItemId() == MENU_SEARCH || item.getItemId() == MENU_MAP) {
            ActivityMixin.finishWithFadeTransition(this)
        }
    }

    /**
     * Loads the caches and fills the adapter according to {@link #search} content.
     * <br>
     * If {@link #search} is {@code null}, this does nothing.
     */

    private Unit replaceCacheListFromSearch() {
        if (search != null) {
            runOnUiThread(() -> {


                // The database search was moved into the UI call intentionally. If this is done before the runOnUIThread,
                // then we have 2 sets of caches in memory. This can lead to OOM for huge cache lists.
                val cachesFromSearchResult: Set<Geocache> = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)
                val lph: LastPositionHelper = LastPositionHelper(this)
                adapter.setList(cachesFromSearchResult)
                updateGui()
                lph.setLastListPosition()
            })
        }
    }

    private static String getCacheNumberString(final Resources res, final Int count) {
        return res.getQuantityString(R.plurals.cache_counts, count, count)
    }

    protected Unit updateTitle() {
        setTitle(title)
        adapter.setCurrentListTitle(title)
        ActionBarUtils.setSubtitle(this, getCurrentSubtitle())
        refreshActionBarTitle()
    }

    /**
     * TODO Possibly parts should be a Thread not a Handler
     */
    private static class DownloadFromWebHandler : DisposableHandler() {
        private final WeakReference<CacheListActivity> activityRef

        DownloadFromWebHandler(final CacheListActivity activity) {
            activityRef = WeakReference<>(activity)
        }

        override         public Unit handleRegularMessage(final Message msg) {
            val activity: CacheListActivity = activityRef.get()
            if (activity != null) {
                activity.updateAdapter()

                val adapter: CacheListAdapter = activity.adapter
                adapter.notifyDataSetChanged()

                val progress: Progress = activity.progress
                switch (msg.what) {
                    case DownloadProgress.MSG_WAITING:  //no caches
                        progress.setMessage(activity.res.getString(R.string.web_import_waiting))
                        break
                    case DownloadProgress.MSG_LOADING: {  //cache downloading
                        val res: Resources = activity.res
                        progress.setMessage(res.getString(R.string.web_downloading) + ' ' + msg.obj + res.getString(R.string.ellipsis))
                        break
                    }
                    case DownloadProgress.MSG_LOADED: {  //Cache downloaded
                        val res: Resources = activity.res
                        progress.setMessage(res.getString(R.string.web_downloaded) + ' ' + msg.obj + res.getString(R.string.ellipsis))
                        activity.refreshCurrentList()
                        break
                    }
                    case DownloadProgress.MSG_SERVER_FAIL:
                        progress.dismiss()
                        activity.showToast(activity.res.getString(R.string.sendToCgeo_download_fail))
                        activity.finish()
                        break
                    case DownloadProgress.MSG_NO_REGISTRATION:
                        progress.dismiss()
                        activity.showToast(activity.res.getString(R.string.sendToCgeo_no_registration))
                        activity.finish()
                        break
                    default:  // MSG_DONE
                        adapter.setSelectMode(false)
                        activity.replaceCacheListFromSearch()
                        progress.dismiss()
                        break
                }
            }
        }
    }

    private static class ClearOfflineLogsHandler : DisposableHandler() {
        private final WeakReference<CacheListActivity> activityRef

        ClearOfflineLogsHandler(final CacheListActivity activity) {
            activityRef = WeakReference<>(activity)
        }

        override         public Unit handleRegularMessage(final Message msg) {
            val activity: CacheListActivity = activityRef.get()
            if (activity != null) {
                activity.adapter.setSelectMode(false)

                activity.refreshCurrentList()

                activity.replaceCacheListFromSearch()

                activity.progress.dismiss()
            }
        }
    }

    private static class ImportGpxAttachementFinishedHandler : WeakReferenceHandler()<CacheListActivity> {

        ImportGpxAttachementFinishedHandler(final CacheListActivity activity) {
            super(activity)
        }

        override         public Unit handleMessage(final Message msg) {
            val activity: CacheListActivity = getReference()
            if (activity != null) {
                activity.refreshCurrentList()
            }
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        setTheme()

        this.contentStorageActivityHelper = ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CONTENT_STORAGE_ACTIVITY_HELPER))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_MULTIPLE, List.class, this::importGpx)

        // get parameters
        val extras: Bundle = getIntent().getExtras()
        typeParameters.clear()
        Geopoint extraTargetCoords = null
        if (extras != null) {
            typeParameters.putAll(extras)
            type = Intents.getListType(getIntent())
            coords = extras.getParcelable(Intents.EXTRA_COORDS)
            extraTargetCoords = extras.getParcelable(Intents.EXTRA_COORDS)
        }
        if (isInvokedFromAttachment()) {
            type = CacheListType.OFFLINE
            if (coords == null) {
                coords = Geopoint.ZERO
            }
        }
        if (type == CacheListType.NEAREST) {
            coords = LocationDataProvider.getInstance().currentGeo().getCoords()
        }

        setTitle(title)
        setContentView(R.layout.cacheslist_activity)

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            currentCacheFilter = savedInstanceState.getParcelable(STATE_GEOCACHE_FILTER)
            sortContext = savedInstanceState.getParcelable(STATE_SORT_CONTEXT)
            type = CacheListType.values()[savedInstanceState.getInt(STATE_LIST_TYPE, type.ordinal())]
            typeParameters.clear()
            typeParameters.putAll(savedInstanceState.getBundle(STATE_TYPE_PARAMETERS))
            listId = savedInstanceState.getInt(STATE_LIST_ID)
            markerId = savedInstanceState.getInt(STATE_MARKER_ID)
            preventAskForDeletion = savedInstanceState.getBoolean(STATE_PREVENTASKFORDELETION)
            offlineListLoadLimit = savedInstanceState.getInt(STATE_OFFLINELISTLOADLIMIT_ID)
            checkForEmtpyList = savedInstanceState.getBoolean(STATE_CHECKFOREMPTYLIST)
        } else {
            sortContext = GeocacheSortContext.getFor(type, "" + listId)
            sortContext.getSort().setTargetCoords(extraTargetCoords)
            offlineListLoadLimit = getOfflineListInitialLoadLimit()
            currentCacheFilter = GeocacheFilterContext(type.filterContextType)
            checkForEmtpyList = true
        }

        initAdapter()

        FilterUtils.initializeFilterBar(this, this)
        updateFilterBar()

        restartCacheLoader(false, null)
        refreshListFooter()

        if (isInvokedFromAttachment()) {
            if (extras != null && !StringUtils.isBlank(extras.getString(Intents.EXTRA_NAME))) {
                listNameMemento.rememberTerm(extras.getString(Intents.EXTRA_NAME))
            } else {
                val data: String = ContentStorage.get().getName(getIntent().getData())
                listNameMemento.rememberTerm(StringUtils.isNotBlank(data) && StringUtils.endsWith(data.toLowerCase(Locale.ROOT), ".gpx") ? StringUtils.substring(data, 0, -4) : data)
            }
            importGpxAttachement()
        }

        getLifecycle().addObserver(GeocacheChangedBroadcastReceiver(this) {
            override             protected Unit onReceive(final Context context, final String geocode) {
                if (IterableUtils.matchesAny(adapter.getFilteredList(), geocache -> geocache.getGeocode() == (geocode))) {
                    val geocache: Geocache = DataStore.loadCache(geocode, EnumSet.of(LoadFlags.LoadFlag.DB_MINIMAL))
                    if (geocache != null) {
                        adapter.setElement(geocache)
                    }
                }
            }
        })
    }

    override     public Unit onSaveInstanceState(final Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState)

        // Save the current Filter
        savedInstanceState.putParcelable(STATE_GEOCACHE_FILTER, currentCacheFilter)
        savedInstanceState.putParcelable(STATE_SORT_CONTEXT, sortContext)
        savedInstanceState.putInt(STATE_LIST_TYPE, type.ordinal())
        savedInstanceState.putBundle(STATE_TYPE_PARAMETERS, typeParameters)
        savedInstanceState.putInt(STATE_LIST_ID, listId)
        savedInstanceState.putInt(STATE_MARKER_ID, markerId)
        savedInstanceState.putBoolean(STATE_PREVENTASKFORDELETION, preventAskForDeletion)
        savedInstanceState.putInt(STATE_OFFLINELISTLOADLIMIT_ID, offlineListLoadLimit)
        savedInstanceState.putBundle(STATE_CONTENT_STORAGE_ACTIVITY_HELPER, contentStorageActivityHelper.getState())
        savedInstanceState.putBoolean(STATE_CHECKFOREMPTYLIST, checkForEmtpyList)
    }

    private Unit refreshActionBarTitle() {
        if (type.canSwitch) {
            actionBarChooser.setList(listId, adapter.getCount(), resultIsOfflineAndLimited())
        }
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)
        if (currentLoader != null && currentLoader.isLoading()) {
            refreshListFooter()
        }
    }

    private Boolean isConcreteList() {
        return type == CacheListType.OFFLINE &&
                (listId == StoredList.STANDARD_LIST_ID || listId >= DataStore.customListIdOffset)
    }

    private Boolean isInvokedFromAttachment() {
        val intent: Intent = getIntent()
        val actionType: String = intent.getAction()
        return Intent.ACTION_SEND_MULTIPLE == (actionType)
                || (Intent.ACTION_VIEW == (actionType) && intent.getData() != null)
    }

    private Unit importGpxAttachement() {
        StoredList.UserInterface(this).promptForListSelection(R.string.gpx_import_select_list_title, listId -> {
            GPXImporter(CacheListActivity.this, listId, importGpxAttachementFinishedHandler).importGPX()
            switchListById(listId)
        }, true, Collections.singleton(0), -1, listNameMemento)
    }

    override     public Unit onResume() {
        super.onResume()

        // save current position
        val lastPosition: LastPositionHelper = LastPositionHelper(this)

        // resume location access
        resumeDisposables.add(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA | GeoDirHandler.UPDATE_DIRECTION | GeoDirHandler.LOW_POWER, 250, TimeUnit.MILLISECONDS))

        adapter.setSelectMode(false)
        setAdapterCurrentCoordinates(true)
        lastPosition.refreshListAtLastPosition(checkForEmtpyList)
        checkForEmtpyList = true

        if (search != null) {
            updateAdapter()
           // loadCachesHandler.sendEmptyMessage(0)
        }
    }

    private Unit setAdapterCurrentCoordinates(final Boolean forceSort) {
        adapter.setActualCoordinates(LocationDataProvider.getInstance().currentGeo().getCoords())
        if (forceSort) {
            adapter.forceSort()
        }
    }

    override     public Unit onPause() {
        resumeDisposables.clear()
        super.onPause()
    }


    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.cache_list_options, menu)

        ViewUtils.setForParentAndChildren(this.findViewById(R.id.sort_bar),
                v -> openSortDialog(),
                v -> refreshWithSortType(sortContext.getSort().getType()))

        ListNavigationSelectionActionProvider.initialize(menu.findItem(R.id.menu_cache_list_app_provider), app -> app.invoke(CacheListAppUtils.filterCoords(adapter.getList()), CacheListActivity.this, getFilteredSearch()))
        FilterUtils.initializeFilterMenu(this, this)
        MenuUtils.enableIconsInOverflowMenu(menu)
        MenuUtils.tintToolbarAndOverflowIcons(menu)

        return true
    }

    public Unit updateSelectSwitchMenuItem(final MenuItem item) {
        ToggleItemType.SELECT_MODE.toggleMenuItem(item, adapter.isSelectMode())
    }

    public Geopoint getReferencePoint() {
        return this.coords
    }


    /**
     * Menu items which are not at all usable with the current list type should be hidden.
     * Menu items which are usable with the current list type but not in the current situation should be disabled.
     */
    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu)

        val isSelectMode: Boolean = adapter.isSelectMode()
        val isHistory: Boolean = type == CacheListType.HISTORY
        val isOffline: Boolean = type == CacheListType.OFFLINE
        val isConcrete: Boolean = isConcreteList()
        val isNonDefaultList: Boolean = isConcrete && listId != StoredList.STANDARD_LIST_ID

        val isEmpty: Boolean = adapter.isEmpty()
        val caches: Collection<Geocache> = adapter.getCheckedOrAllCaches()
        val containsOfflineLogs: Boolean = containsOfflineLogs(caches)
        val containsStoredPastEvents: Boolean = containsStoredPastEvents(caches)
        val containsStoredCaches: Boolean = containsStoredCaches(caches)

        val isGcConnectorActive: Boolean = Settings.isGCConnectorActive()
        val isGcPremiumMember: Boolean = isGcConnectorActive && Settings.isGCPremiumMember()

        val hasListNavigationApps: Boolean = CacheListApps.getActiveApps().size() > 1
        val checkedCount: Int = adapter.getCheckedCount()


        try {
            // toplevel menu items
            MenuUtils.setEnabled(menu, R.id.menu_show_on_map, !isEmpty)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_sort, !isHistory, !isEmpty)

            MenuUtils.setEnabled(menu, R.id.menu_switch_select_mode, !isEmpty)
            updateSelectSwitchMenuItem(menu.findItem(R.id.menu_switch_select_mode))
            MenuUtils.setVisible(menu, R.id.menu_invert_selection, isSelectMode); // exception to the general rule: only show in select mode
            MenuUtils.setVisible(menu, R.id.menu_select_next20, isSelectMode); // same here
            MenuUtils.setVisible(menu, R.id.menu_select_next100, isSelectMode); // same here

            MenuUtils.setVisibleEnabled(menu, R.id.menu_cache_list_app_provider, hasListNavigationApps, !isEmpty)

            // Manage Caches submenu
            MenuUtils.setEnabled(menu, R.id.menu_refresh_stored, !isEmpty)
            if (isOffline || isHistory) { // only offline list
                setMenuItemLabel(menu, R.id.menu_refresh_stored, R.string.caches_refresh_selected, R.string.caches_refresh_all, checkedCount)
            } else { // search and global list (all other than offline and history)
                setMenuItemLabel(menu, R.id.menu_refresh_stored, R.string.caches_store_selected, R.string.caches_store_offline, checkedCount)
            }

            MenuUtils.setVisible(menu, R.id.menu_move_to_list, containsStoredCaches)
            setMenuItemLabel(menu, R.id.menu_move_to_list, R.string.caches_move_selected, R.string.caches_move_all, checkedCount)
            MenuUtils.setVisible(menu, R.id.menu_copy_to_list, containsStoredCaches)
            setMenuItemLabel(menu, R.id.menu_copy_to_list, R.string.caches_copy_selected, R.string.caches_copy_all, checkedCount)

            MenuUtils.setEnabled(menu, R.id.menu_add_to_route, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_add_to_route, R.string.caches_append_to_route_selected, R.string.caches_append_to_route_all, checkedCount)
            MenuUtils.setVisible(menu, R.id.menu_delete_events, containsStoredPastEvents)
            MenuUtils.setVisible(menu, R.id.menu_clear_offline_logs, containsOfflineLogs)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_remove_from_history, isHistory, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_remove_from_history, R.string.cache_remove_from_history, R.string.cache_clear_history, checkedCount)

            val removeFromDevice: Boolean = removeWillDeleteFromDevice(listId)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_drop_caches, (isHistory || containsStoredCaches) && !removeFromDevice, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_drop_caches, R.string.caches_remove_selected, R.string.caches_remove_all, checkedCount)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_drop_caches_all_lists, isHistory || containsStoredCaches, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_drop_caches_all_lists, R.string.caches_remove_selected_completely, R.string.caches_remove_all_completely, checkedCount)

            //MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_bookmarklist, isGcPremiumMember, !isEmpty)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_bookmarklist, true, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_upload_bookmarklist, R.string.caches_upload_bookmarklist_selected, R.string.caches_upload_bookmarklist_all, checkedCount)

            MenuUtils.setVisible(menu, R.id.menu_watch_management, WatchListUtils.anySupportsWatchlist(adapter.getCheckedOrAllCaches()))
            MenuUtils.setVisibleEnabled(menu, R.id.menu_watch_all, WatchListUtils.anySupportsWatchlist(adapter.getCheckedOrAllCaches()), WatchListUtils.anySupportsWatching(adapter.getCheckedOrAllCaches()))
            setMenuItemLabel(menu, R.id.menu_watch_all, R.string.caches_watch_selected, R.string.caches_watch_all, checkedCount)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_unwatch_all, WatchListUtils.anySupportsWatchlist(adapter.getCheckedOrAllCaches()), WatchListUtils.anySupportsUnwatching(adapter.getCheckedOrAllCaches()))
            setMenuItemLabel(menu, R.id.menu_unwatch_all, R.string.caches_unwatch_selected, R.string.caches_unwatch_all, checkedCount)

            MenuUtils.setEnabled(menu, R.id.menu_show_attributes, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_show_attributes, R.string.caches_show_attributes_selected, R.string.caches_show_attributes_all, checkedCount)
            MenuUtils.setEnabled(menu, R.id.menu_set_cache_icon, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_set_cache_icon, R.string.caches_set_cache_icon_selected, R.string.caches_set_cache_icon_all, checkedCount)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_remove_from_other_lists, isOffline && listId != PseudoList.ALL_LIST.id, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_remove_from_other_lists, R.string.caches_remove_from_other_lists_selected, R.string.caches_remove_from_other_lists_all, checkedCount)

            // Manage Lists submenu
            MenuUtils.setVisibleEnabled(menu, R.id.menu_lists, isOffline, !isSelectMode)
            MenuUtils.setVisible(menu, R.id.menu_drop_list, isNonDefaultList)
            MenuUtils.setVisible(menu, R.id.menu_rename_list, isNonDefaultList)
            MenuUtils.setVisible(menu, R.id.menu_rename_list_prefix, isNonDefaultList && DataStore.getListHierarchy().size() > 1)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_make_list_unique, listId != PseudoList.ALL_LIST.id, !isEmpty)
            MenuUtils.setVisible(menu, R.id.menu_set_listmarker, isNonDefaultList)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_set_askfordeletion, isNonDefaultList, preventAskForDeletion)

            // Import submenu
            MenuUtils.setVisibleEnabled(menu, R.id.menu_import, isOffline && listId != PseudoList.ALL_LIST.id, !isSelectMode)
            MenuUtils.setEnabled(menu, R.id.menu_import_pq, isGcPremiumMember)
            MenuUtils.setEnabled(menu, R.id.menu_bookmarklists, isGcPremiumMember)

            // Export
            MenuUtils.setVisibleEnabled(menu, R.id.menu_export, isHistory || isOffline, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_export_gpx, R.string.export_gpx, R.string.export_gpx, checkedCount)
            setMenuItemLabel(menu, R.id.menu_export_fieldnotes, R.string.export_fieldnotes, R.string.export_fieldnotes, checkedCount)
            setMenuItemLabel(menu, R.id.menu_export_persnotes, R.string.export_persnotes, R.string.export_persnotes, checkedCount)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_modifiedcoords, isGcConnectorActive, !isEmpty)
            MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_allcoords, isGcConnectorActive, !isEmpty)
            setMenuItemLabel(menu, R.id.menu_upload_allcoords, R.string.caches_upload_allcoords, R.string.caches_upload_allcoords, checkedCount)

        } catch (final RuntimeException e) {
            Log.e("CacheListActivity.onPrepareOptionsMenu", e)
        }
        MenuUtils.tintToolbarAndOverflowIcons(menu)

        return true
    }

    private Boolean containsStoredCaches(final Collection<Geocache> caches) {
        for (final Geocache cache : caches) {
            if (cache.isOffline()) {
                return true
            }
        }
        return false
    }

    private Boolean containsStoredPastEvents(final Collection<Geocache> caches) {
        for (final Geocache cache : caches) {
            if (CalendarUtils.isPastEvent(cache)) {
                return true
            }
        }
        return false
    }

    private Boolean containsOfflineLogs(final Collection<Geocache> caches) {
        for (final Geocache cache : caches) {
            if (cache.hasLogOffline()) {
                return true
            }
        }
        return false
    }

    private Unit setMenuItemLabel(final Menu menu, final Int menuId, @StringRes final Int resIdSelection, @StringRes final Int resId, final Int checkedCount) {
        val menuItem: MenuItem = menu.findItem(menuId)
        if (menuItem == null) {
            return
        }

        val hasSelection: Boolean = checkedCount > 0
        if (hasSelection) {
            menuItem.setTitle(res.getString(resIdSelection) + " (" + checkedCount + ")")
        } else {
            menuItem.setTitle(res.getString(resId))
        }
    }

    private Unit setListMarker(final Int newListMarker) {
        DataStore.setListEmoji(listId, newListMarker)
        markerId = newListMarker
        MapMarkerUtils.resetLists()
        adapter.notifyDataSetChanged()
        refreshActionBarTitle()
        refreshCurrentList()
    }

    private Unit setPreventAskForDeletion(final Boolean prevent) {
        DataStore.setListPreventAskForDeletion(listId, prevent)
        preventAskForDeletion = prevent
        invalidateOptionsMenuCompatible()
    }

    private Unit setCacheIcons(final Int newCacheIcon) {
        if (newCacheIcon == 0) {
            SimpleDialog.of(this).setTitle(R.string.caches_reset_cache_icons_title).setMessage(R.string.caches_reset_cache_icons_title).confirm(() -> setCacheIconsHelper(0))
        } else {
            setCacheIconsHelper(newCacheIcon)
        }
    }

    private Unit setCacheIconsHelper(final Int newCacheIcon) {
        SetCacheIconCommand(this, adapter.getCheckedOrAllCaches(), newCacheIcon) {
            override             protected Unit onFinished() {
                adapter.setSelectMode(false)
                refreshCurrentList(AfterLoadAction.CHECK_IF_EMPTY)
            }
        }.execute()
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val menuItem: Int = item.getItemId()
        if (menuItem == R.id.menu_show_on_map) {
            goMap()
        } else if (menuItem == R.id.menu_switch_select_mode) {
            adapter.switchSelectMode()
            updateSelectSwitchMenuItem(item)
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_refresh_stored) {
            refreshInBackground(adapter.getCheckedOrAllCaches())
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_add_to_route) {
            appendInBackground(adapter.getCheckedOrAllCaches())
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_drop_caches) {
            deleteCaches(adapter.getCheckedOrAllCaches(), false)
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_drop_caches_all_lists) {
            deleteCaches(adapter.getCheckedOrAllCaches(), true)
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_import_pq) {
            checkForEmtpyList = false
            startListSelection(REQUEST_CODE_IMPORT_PQ)
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_bookmarklists) {
            checkForEmtpyList = false
            startListSelection(REQUEST_CODE_IMPORT_BOOKMARK)
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_import_gpx) {
            importGpxSelectFiles()
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_create_list) {
            StoredList.UserInterface(this).promptForListCreation(getListSwitchingRunnable(), StringUtils.EMPTY)
            refreshActionBarTitle()
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_drop_list) {
            removeList()
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_rename_list) {
            renameList()
        } else if (menuItem == R.id.menu_rename_list_prefix) {
            StoredList.UserInterface(this).promptForListPrefixRename(() -> {
                refreshCurrentList()
                invalidateOptionsMenuCompatible()
            })
        } else if (menuItem == R.id.menu_invert_selection) {
            adapter.invertSelection()
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_select_next20) {
            adapter.selectNextCaches(20)
        } else if (menuItem == R.id.menu_select_next100) {
            adapter.selectNextCaches(100)
        } else if (menuItem == R.id.menu_filter) {
            showFilterMenu()
        } else if (menuItem == R.id.menu_sort) {
            openSortDialog()
        } else if (menuItem == R.id.menu_import_web) {
            importWeb()
        } else if (menuItem == R.id.menu_export_gpx) {
            GpxExport().export(adapter.getCheckedOrAllCaches(), this, title)
        } else if (menuItem == R.id.menu_export_fieldnotes) {
            FieldNoteExport().export(adapter.getCheckedOrAllCaches(), this)
        } else if (menuItem == R.id.menu_export_persnotes) {
            PersonalNoteExport().export(adapter.getCheckedOrAllCaches(), this)
        } else if (menuItem == R.id.menu_upload_modifiedcoords) {
            val that: Activity = this
            SimpleDialog.of(this).setTitle(R.string.caches_upload_modifiedcoords).setMessage(R.string.caches_upload_modifiedcoords_warning).confirm(() -> BatchUploadModifiedCoordinates(true).export(adapter.getCheckedOrAllCaches(), that))
        } else if (menuItem == R.id.menu_upload_allcoords) {
            val that2: Activity = this
            SimpleDialog.of(this).setTitle(R.string.caches_upload_allcoords_dialogtitle).setMessage(R.string.caches_upload_allcoords_warning).confirm(() -> BatchUploadModifiedCoordinates(false).export(adapter.getCheckedOrAllCaches(), that2))
        } else if (menuItem == R.id.menu_share_geocodes) {
            shareGeocodes(adapter.getCheckedOrAllCaches())
        } else if (menuItem == R.id.menu_remove_from_history) {
            removeFromHistoryCheck()
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_move_to_list) {
            moveCachesToOtherList(adapter.getCheckedOrAllCaches())
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_copy_to_list) {
            copyCachesToOtherList(adapter.getCheckedOrAllCaches())
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_delete_events) {
            deletePastEvents(adapter.getCheckedOrAllCaches())
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_create_internal_cache) {
            InternalConnector.interactiveCreateCache(this, coords, StoredList.getConcreteList(listId), false)
        } else if (menuItem == R.id.menu_clear_offline_logs) {
            clearOfflineLogs(adapter.getCheckedOrAllCaches())
            invalidateOptionsMenuCompatible()
        } else if (menuItem == R.id.menu_show_attributes) {
            adapter.showAttributes(adapter.getCheckedOrAllCaches())
        } else if (menuItem == R.id.menu_make_list_unique || menuItem == R.id.menu_remove_from_other_lists) {
            MakeListUniqueCommand(this, listId, Geocache.getGeocodes(menuItem == R.id.menu_remove_from_other_lists ? adapter.getCheckedOrAllCaches() : ArrayList<>())) {

                override                 protected Unit onFinished() {
                    refreshActionBarTitle()
                }

                override                 protected Unit onFinishedUndo() {
                    refreshActionBarTitle()
                }

            }.execute()
        } else if (menuItem == R.id.menu_upload_bookmarklist) {
            BookmarkUtils.askAndUploadCachesToBookmarkList(this, adapter.getCheckedOrAllCaches())
        } else if (menuItem == R.id.menu_watch_all) {
            WatchListUtils.watchAll(this, adapter.getCheckedOrAllCaches())
        } else if (menuItem == R.id.menu_unwatch_all) {
            WatchListUtils.unwatchAll(this, adapter.getCheckedOrAllCaches())
        } else if (menuItem == R.id.menu_set_listmarker) {
            EmojiUtils.selectEmojiPopup(this, markerId, null, this::setListMarker)
        } else if (menuItem == R.id.menu_set_cache_icon) {
            EmojiUtils.selectEmojiPopup(this, -1, null, this::setCacheIcons)
        } else if (menuItem == R.id.menu_set_askfordeletion) {
            setPreventAskForDeletion(false)
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    public Unit startListSelection(final Int requestCode) {
        val intent: Intent = Intent(this, requestCode == REQUEST_CODE_IMPORT_PQ ? PocketQueryListActivity.class : BookmarkListActivity.class)
        intent.putExtra(Intents.EXTRA_PQ_LIST_IMPORT, true)
        startActivityForResult(intent, requestCode)
    }

    private Unit checkIfEmptyAndRemoveAfterConfirm() {
        val isNonDefaultList: Boolean = isConcreteList() && listId != StoredList.STANDARD_LIST_ID
        // Check local cacheList first, and Datastore only if needed (because of filtered lists)
        // Checking is done in this order for performance reasons
        if (isNonDefaultList && !preventAskForDeletion && adapter.isEmpty()
                && DataStore.getAllStoredCachesCount(listId) == 0) {
            // ask user, if he wants to delete the now empty list
            Dialogs.confirmWithCheckbox(this, getString(R.string.list_dialog_remove), getString(R.string.list_dialog_remove_nowempty),
                    CheckboxDialogConfig.newCheckbox(R.string.list_dialog_do_not_ask_me_again)
                            .setActionButtonLabel(CheckboxDialogConfig.ActionButtonLabel.YES_NO)
                            .setPositiveButtonCheckCondition(CheckboxDialogConfig.CheckCondition.UNCHECKED),
                    preventAskForDeletion -> removeListInternal(), this::setPreventAskForDeletion)
        }
    }

    private Boolean cacheToShow() {
        if (search == null || adapter.isEmpty()) {
            showToast(res.getString(R.string.warn_no_cache_coord))
            return false
        }
        return true
    }

    private SearchResult getFilteredSearch() {
        return SearchResult(Geocache.getGeocodes(adapter.getFilteredList()))
    }

    private Unit deletePastEvents(final Collection<Geocache> caches) {
        val deletion: List<Geocache> = ArrayList<>()
        for (final Geocache cache : caches) {
            if (CalendarUtils.isPastEvent(cache)) {
                deletion.add(cache)
            }
        }
        deleteCaches(deletion, false)
    }

    private Unit clearOfflineLogs(final Collection<Geocache> caches) {
        SimpleDialog.of(this).setTitle(R.string.caches_clear_offlinelogs).setMessage(R.string.caches_clear_offlinelogs_message).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> {
            progress.show(CacheListActivity.this, null, res.getString(R.string.caches_clear_offlinelogs_progress), true, clearOfflineLogsHandler.disposeMessage())
            clearOfflineLogs(clearOfflineLogsHandler, caches)
        })
    }

    /**
     * called from the filter bar view
     */
    override     public Unit showFilterMenu() {
        GeocacheFilterActivity.selectFilter(this, currentCacheFilter, adapter.getFilteredList(), !resultIsOfflineAndLimited())
    }

    /**
     * called from the filter bar view
     */
    override     public Boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, currentCacheFilter)
    }

    override     public Unit onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info)

        AdapterContextMenuInfo adapterInfo = null
        try {
            adapterInfo = (AdapterContextMenuInfo) info
        } catch (final Exception e) {
            Log.w("CacheListActivity.onCreateContextMenu", e)
        }

        if (adapterInfo == null || adapterInfo.position >= adapter.getCount()) {
            return
        }
        val cache: Geocache = adapter.getItem(adapterInfo.position)
        assert cache != null

        menu.setHeaderTitle(StringUtils.defaultIfBlank(cache.getName(), cache.getShortGeocode()))

        contextMenuGeocode = cache.getGeocode()

        getMenuInflater().inflate(R.menu.cache_list_context, menu)

        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName())
        val hasCoords: Boolean = cache.getCoords() != null
        MenuUtils.setVisible(menu.findItem(R.id.menu_default_navigation), hasCoords)
        MenuUtils.setVisible(menu.findItem(R.id.menu_navigate), hasCoords)
        MenuUtils.setVisible(menu.findItem(R.id.menu_cache_details), hasCoords)
        val isOffline: Boolean = cache.isOffline()
        MenuUtils.setVisible(menu.findItem(R.id.menu_drop_cache), isOffline)
        MenuUtils.setVisible(menu.findItem(R.id.menu_move_to_list), isOffline)
        MenuUtils.setVisible(menu.findItem(R.id.menu_copy_to_list), isOffline)
        MenuUtils.setVisible(menu.findItem(R.id.menu_refresh), isOffline)
        MenuUtils.setVisible(menu.findItem(R.id.menu_store_cache), !isOffline)

        LoggingUI.onPrepareOptionsMenu(menu, cache)
    }

    private Unit moveCachesToOtherList(final Collection<Geocache> caches) {
        if (isConcreteList()) {
            MoveToListCommand(this, caches, listId) {
                private LastPositionHelper lastPositionHelper

                override                 protected Unit doCommand() {
                    lastPositionHelper = LastPositionHelper(CacheListActivity.this)
                    super.doCommand()
                }

                override                 protected Unit onFinished() {
                    lastPositionHelper.refreshListAtLastPosition(true)
                }

            }.execute()
        } else {
            MoveToListAndRemoveFromOthersCommand(this, caches) {
                private LastPositionHelper lastPositionHelper

                override                 protected Unit doCommand() {
                    lastPositionHelper = LastPositionHelper(CacheListActivity.this)
                    super.doCommand()
                }

                override                 protected Unit onFinished() {
                    lastPositionHelper.refreshListAtLastPosition(true)
                }

            }.execute()
        }
    }

    private Unit copyCachesToOtherList(final Collection<Geocache> caches) {
        CopyToListCommand(this, caches, listId) {

            override             protected Unit onFinished() {
                adapter.setSelectMode(false)
                refreshCurrentList(AfterLoadAction.CHECK_IF_EMPTY)
            }

        }.execute()
    }

    override     public Boolean onContextItemSelected(final MenuItem item) {
        ContextMenu.ContextMenuInfo info = item.getMenuInfo()

        // restore menu info for sub menu items, see
        // https://code.google.com/p/android/issues/detail?id=7139
        if (info == null) {
            info = lastMenuInfo
            lastMenuInfo = null
        }

        AdapterContextMenuInfo adapterInfo = null
        try {
            adapterInfo = (AdapterContextMenuInfo) info
        } catch (final Exception e) {
            Log.w("CacheListActivity.onContextItemSelected", e)
        }

        val cache: Geocache = adapterInfo != null ? getCacheFromAdapter(adapterInfo) : null

        // just in case the list got resorted while we are executing this code
        if (cache == null || adapterInfo == null) {
            return true
        }

        val itemId: Int = item.getItemId()
        if (itemId == R.id.menu_default_navigation) {
            NavigationAppFactory.startDefaultNavigationApplication(1, this, cache)
        } else if (itemId == R.id.menu_navigate) {
            NavigationAppFactory.showNavigationMenu(this, cache, null, null)
        } else if (itemId == R.id.menu_cache_details) {
            CacheDetailActivity.startActivity(this, cache.getGeocode(), cache.getName())
        } else if (itemId == R.id.menu_drop_cache) {
            deleteCaches(Collections.singletonList(cache), false)
        } else if (itemId == R.id.menu_move_to_list) {
            moveCachesToOtherList(Collections.singletonList(cache))
        } else if (itemId == R.id.menu_copy_to_list) {
            copyCachesToOtherList(Collections.singletonList(cache))
        } else if (itemId == R.id.menu_store_cache || itemId == R.id.menu_refresh) {
            CacheDownloaderService.refreshCache(this, cache.getGeocode(), itemId == R.id.menu_refresh, this::refreshCurrentList)
        } else {
            // we must remember the menu info for the sub menu, there is a bug
            // in Android:
            // https://code.google.com/p/android/issues/detail?id=7139
            lastMenuInfo = info
            val selectedView: View = adapterInfo.targetView
            LoggingUI.onMenuItemSelected(item, this, cache, dialog -> {
                if (selectedView != null) {
                    final CacheListAdapter.ViewHolder holder = (CacheListAdapter.ViewHolder) selectedView.getTag()
                    if (holder != null) {
                        CacheListAdapter.updateViewHolder(holder, cache, res)
                    }
                }
            })
        }
        return true
    }

    /**
     * Extract a cache from adapter data.
     *
     * @param adapterInfo an adapterInfo
     * @return the pointed cache
     */
    private Geocache getCacheFromAdapter(final AdapterContextMenuInfo adapterInfo) {
        val cache: Geocache = adapter.getItem(adapterInfo.position)
        assert cache != null
        if (cache.getGeocode().equalsIgnoreCase(contextMenuGeocode)) {
            return cache
        }

        return adapter.findCacheByGeocode(contextMenuGeocode)
    }

    private Unit setFilter() {
        applyAdapterFilter()
        updateFilterBar()
        updateTitle()
        invalidateOptionsMenuCompatible()
    }

    private Unit applyAdapterFilter() {
        val filter: GeocacheFilter = currentAddFilterCriteria == null ?
                currentCacheFilter.get() : currentCacheFilter.get().clone().and(currentAddFilterCriteria)
        adapter.setFilter(filter)
    }

    override     public Unit onBackPressed() {
        if (adapter.isSelectMode()) {
            adapter.setSelectMode(false)
            invalidateOptionsMenu(); // update select mode icon
        } else {
            super.onBackPressed()
        }
    }

    private Unit initAdapter() {
        val listView: ListView = getListView()
        registerForContextMenu(listView)

        adapter = CacheListAdapter(this, adapter == null ? ArrayList<>() : adapter.getList(), type, sortContext)
        adapter.setStoredLists(StoredList.UserInterface.getMenuLists(true, PseudoList.NEW_LIST.id))
        applyAdapterFilter()

        if (listFooter == null) {
            listFooter = getLayoutInflater().inflate(R.layout.cacheslist_footer, listView, false)
            listFooterLine1 = listFooter.findViewById(R.id.more_caches_1)
            listFooterLine2 = listFooter.findViewById(R.id.more_caches_2)
            listView.addFooterView(listFooter)
        }
        setListAdapter(adapter)

        adapter.forceSort()
        updateSortBar()

        listView.setOnScrollListener(FastScrollListener(listView))
    }

    private Unit updateAdapter() {
        val lph: LastPositionHelper = LastPositionHelper(this)
        setAdapterCurrentCoordinates(false)
        adapter.notifyDataSetChanged()
        adapter.forceFilter()
        adapter.checkSpecialSortOrder()
        adapter.forceSort()
        updateGui()
        lph.setLastListPosition()
    }

    private Unit updateGui() {
        updateSortBar()
        updateTitle()
        refreshListFooter()
    }

    private Unit refreshListFooter() {
        if (listFooter == null) {
            return
        }

        if (type.isOnline) {
            val unfilteredListSize: Int = search == null ? adapter.getOriginalListCount() : search.getCount()
            val totalAchievableListSize: Int = search == null ? unfilteredListSize : Math.max(0, search.getTotalCount())
            val moreToShow: Boolean = unfilteredListSize > 0 && totalAchievableListSize > unfilteredListSize

            if (moreToShow) {
                setViewGone(listFooterLine2)
                setView(listFooterLine1, res.getString(R.string.caches_more_caches) + " (" + res.getString(R.string.caches_more_caches_currently) + ": " + unfilteredListSize + ")", v -> {
                    showProgress(true)
                    restartCacheLoader(true, null)
                })
            } else {
                setViewGone(listFooterLine2)
                setView(listFooterLine1, res.getString(adapter.isEmpty() ? R.string.caches_no_cache : R.string.caches_more_caches_no), null)
            }
        } else if (resultIsOfflineAndLimited()) {
            val missingCaches: Int = search.getTotalCount() - search.getCount()

            if (missingCaches > getOfflineListLimitIncrease()) {
                val info: String = res.getQuantityString(R.plurals.caches_more_caches_next_x, getOfflineListLimitIncrease(), getOfflineListLimitIncrease())
                setView(listFooterLine1, info, v -> {
                    if (offlineListLoadLimit >= 0) {
                        offlineListLoadLimit += getOfflineListLimitIncrease()
                        refreshCurrentList()
                    }
                })
            } else {
                setViewGone(listFooterLine1)
            }
            setView(listFooterLine2, res.getString(R.string.caches_more_caches_all_remaining), v -> {
                offlineListLoadLimit = 0
                refreshCurrentList()
            })
        } else {
            setViewGone(listFooterLine1)
            setViewGone(listFooterLine2)
        }
    }

    private Unit setViewGone(final View view) {
        view.setVisibility(View.GONE)
        view.setOnClickListener(null)
        view.setClickable(false)
    }

    private Unit setView(final TextView view, final String text, final View.OnClickListener clickListener) {
        view.setVisibility(View.VISIBLE)
        view.setText(text)
        view.setClickable(clickListener != null)
        view.setOnClickListener(clickListener)
    }

    private Unit setView(final TextView view, final String text, final View.OnClickListener clickListener, final View.OnLongClickListener longClickListener) {
        setView(view, text, clickListener)
    }

    private Unit updateSortBar() {
        val sortView: View = this.findViewById(R.id.sort_bar)
        final GeocacheSort.SortType st = sortContext.getSort().getType()
        if (st == null || GeocacheSort.SortType.AUTO == (st) || CacheListType.HISTORY == (type)) {
            sortView.setVisibility(View.GONE)
        } else {
            sortView.setVisibility(View.VISIBLE)
            val sortTextView: TextView = findViewById(R.id.sort_text)
            sortTextView.setText(sortContext.getSort().getDisplayName())
        }
    }

    private Unit importGpxSelectFiles() {
        contentStorageActivityHelper.selectMultipleFiles(null, PersistableFolder.GPX.getUri())
    }

    private Unit importGpx(final List<Uri> uris) {
        val importer: GPXImporter = GPXImporter(this, listId, importGpxAttachementFinishedHandler)
        for (Uri uri : uris) {
            importer.importGPX(uri, null, ContentStorage.get().getName(uri))
        }
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)

        if (contentStorageActivityHelper.onActivityResult(requestCode, resultCode, data)) {
            return
        }

        if ((requestCode == REQUEST_CODE_IMPORT_PQ || requestCode == REQUEST_CODE_IMPORT_BOOKMARK) && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri: Uri = data.getData()
                GPXImporter(this, listId, importGpxAttachementFinishedHandler).importGPX(uri, data.getType(), null)
            }
        } else if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            setAndRefreshFilterForOnlineSearch(data.getParcelableExtra(GeocacheFilterActivity.EXTRA_FILTER_CONTEXT))
        } else if (requestCode == REQUEST_CODE_LOG && resultCode == Activity.RESULT_OK && data != null) {
            val navBar: View = findViewById(R.id.activity_navigationBar)
            val isNavBarVisible: Boolean = navBar != null && navBar.getVisibility() == View.VISIBLE && navBar.getHeight() > 0

            ShareUtils.showLogPostedSnackbar(this, data, isNavBarVisible ? navBar : null)
        }
    }

    override     public Unit refreshWithFilter(final GeocacheFilter filter) {
        currentCacheFilter.set(filter)
        setFilter()
        refreshFilterForOnlineSearch()

        refreshCurrentList()
    }

    private Unit setAndRefreshFilterForOnlineSearch(final GeocacheFilterContext filterContext) {
        currentCacheFilter = filterContext
        setFilter()
        refreshFilterForOnlineSearch()
    }

    private Unit refreshFilterForOnlineSearch() {
        //not supported yet for all online searches
        if (type.isOnline && type != CacheListType.POCKET) {
            restartCacheLoader(false, null)
        }
    }

    private Unit refreshInBackground(final List<Geocache> caches) {
        if (type.isStoredInDatabase && caches.size() > REFRESH_WARNING_THRESHOLD) {
            SimpleDialog.of(this).setTitle(R.string.caches_refresh_all).setMessage(R.string.caches_refresh_all_warning).confirm(() ->
                    CacheDownloaderService.downloadCaches(this, Geocache.getGeocodes(caches), true, type.isStoredInDatabase, this::refreshCurrentList))
        } else {
            CacheDownloaderService.downloadCaches(this, Geocache.getGeocodes(caches), true, type.isStoredInDatabase, this::refreshCurrentList)
        }
    }

    private Unit appendInBackground(final Collection<Geocache> caches) {
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.appendToIndividualRoute(caches), () -> ActivityMixin.showShortToast(this, R.string.caches_appended_to_route))
        adapter.setSelectMode(false)
    }

    public Unit removeFromHistoryCheck() {
        val message: Int = (adapter != null && adapter.getCheckedCount() > 0) ? R.string.cache_remove_from_history
                : R.string.cache_clear_history
        SimpleDialog.of(this).setTitle(R.string.caches_removing_from_history).setMessage(message).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> removeFromHistory(adapter.getCheckedOrAllCaches()))
    }

    private Unit removeFromHistory(final Collection<Geocache> caches) {
        val geocodes: Collection<String> = ArrayList<>(caches.size())
        for (final Geocache cache : caches) {
            geocodes.add(cache.getGeocode())
        }
        DataStore.clearVisitDate(geocodes)
        DataStore.clearLogsOffline(caches)
        refreshCurrentList()
    }

    private Unit importWeb() {
        // menu is also shown with no device connected
        if (!Settings.isRegisteredForSend2cgeo()) {
            SimpleDialog.of(this).setTitle(R.string.web_import_title).setMessage(R.string.init_sendToCgeo_description).confirm(() -> SettingsActivity.openForScreen(R.string.preference_screen_sendtocgeo, CacheListActivity.this))
            return
        }

        detailProgress.set(0)
        showProgress(false)
        val downloadFromWebHandler: DownloadFromWebHandler = DownloadFromWebHandler(this)
        progress.show(this, null, res.getString(R.string.web_import_waiting), true, downloadFromWebHandler.disposeMessage())
        Send2CgeoDownloader.loadFromWeb(downloadFromWebHandler, listId)
    }

    private Unit deleteCaches(final Collection<Geocache> caches, final Boolean removeFromAllLists) {
        DeleteCachesFromListCommand(this, caches, listId, removeFromAllLists).execute()
    }

    private Unit shareGeocodes(final Collection<Geocache> caches) {
        val sb: StringBuilder = StringBuilder()
        Boolean first = true
        for (Geocache cache : caches) {
            sb.append(first ? "" : ',').append(cache.getGeocode())
            first = false
        }
        ShareUtils.sharePlainText(this, sb.toString())
    }

    private static class LastPositionHelper {
        private final WeakReference<CacheListActivity> activityRef
        private final Int lastListPosition
        private final Int onTopSpace

        LastPositionHelper(final CacheListActivity context) {
            super()
            this.lastListPosition = context.getListView().getFirstVisiblePosition()
            this.activityRef = WeakReference<>(context)

            val firstChild: View = context.getListView().getChildAt(0)
            onTopSpace = (firstChild == null) ? 0 : (firstChild.getTop() - context.getListView().getPaddingTop())

        }

        public Unit refreshListAtLastPosition(final Boolean triggerEmptyCheck) {
            val activity: CacheListActivity = activityRef.get()
            if (activity != null) {
                activity.adapter.setSelectMode(false)
                if (activity.type.isStoredInDatabase) {
                    activity.refreshCurrentList(triggerEmptyCheck ? AfterLoadAction.CHECK_IF_EMPTY : AfterLoadAction.NO_ACTION)
                } else {
                    activity.replaceCacheListFromSearch()
                }
            }
            setLastListPosition()
        }

        public Unit setLastListPosition() {
            val activity: CacheListActivity = activityRef.get()
            if (activity != null) {
                if (lastListPosition > 0 && lastListPosition < activity.adapter.getCount()) {
                    activity.getListView().setSelectionFromTop(lastListPosition, onTopSpace)
                }
            }
        }
    }

    public static Boolean removeWillDeleteFromDevice(final Int listId) {
        return listId == PseudoList.ALL_LIST.id || listId == PseudoList.HISTORY_LIST.id || listId == StoredList.TEMPORARY_LIST.id
    }

    private static class DeleteCachesFromListCommand : AbstractCachesCommand() {

        private final LastPositionHelper lastPositionHelper
        private final Int listId
        private final Map<String, Set<Integer>> oldCachesLists = HashMap<>()
        private final Boolean removeFromAllLists

        DeleteCachesFromListCommand(final CacheListActivity context, final Collection<Geocache> caches, final Int listId, final Boolean removeFromAllLists) {
            super(context, caches, R.string.command_delete_caches_progress)
            this.lastPositionHelper = LastPositionHelper(context)
            this.listId = listId
            this.removeFromAllLists = removeFromAllLists
        }

        override         public Unit onFinished() {
            lastPositionHelper.refreshListAtLastPosition(true)
        }

        override         protected Unit doCommand() {
            if (appliesToAllLists()) {
                oldCachesLists.putAll(DataStore.markDropped(getCaches()))
            } else {
                DataStore.removeFromList(getCaches(), listId)
            }
        }

        public Boolean appliesToAllLists() {
            return removeFromAllLists || removeWillDeleteFromDevice(listId)
        }

        override         protected Unit undoCommand() {
            if (appliesToAllLists()) {
                DataStore.addToLists(getCaches(), oldCachesLists)
            } else {
                DataStore.addToList(getCaches(), listId)
            }
        }

        override         protected String getResultMessage() {
            val size: Int = getCaches().size()
            return getContext().getResources().getQuantityString(R.plurals.command_delete_caches_result, size, size)
        }
    }

    private static Unit clearOfflineLogs(final Handler handler, final Collection<Geocache> selectedCaches) {
        Schedulers.io().scheduleDirect(() -> {
            DataStore.clearLogsOffline(selectedCaches)
            handler.sendEmptyMessage(DownloadProgress.MSG_DONE)
        })
    }

    private Unit restartCacheLoader(final Boolean nextPage, final Consumer<Bundle> extrasModifier) {
        val extras: Bundle = Bundle()
        extras.putAll(typeParameters)
        if (extrasModifier != null) {
            extrasModifier.accept(extras)
        }
        extras.putBoolean(EXTRAS_NEXTPAGE, nextPage)
        LoaderManager.getInstance(CacheListActivity.this).restartLoader(CACHE_LOADER_ID, extras, CacheListActivity.this)
    }

    private Action1<Integer> getListSwitchingRunnable() {
        return this::switchListById
    }

    private Unit switchListById(final Int id) {
        switchListById(id, AfterLoadAction.NO_ACTION)
    }

    private Unit switchListById(final Int id, final AfterLoadAction action) {
        if (id < 0) {
            return
        }

        val previousListType: CacheListType = type

        if (id != listId) {
            //reset load limit
            offlineListLoadLimit = getOfflineListInitialLoadLimit()
        }

        if (id == PseudoList.HISTORY_LIST.id) {
            type = CacheListType.HISTORY
            if (previousListType != type) {
                currentCacheFilter = GeocacheFilterContext(type.filterContextType)
            }
            restartCacheLoader(false, e -> e.putSerializable(BUNDLE_ACTION_KEY, action))
        } else {
            if (id == PseudoList.ALL_LIST.id) {
                listId = id
                title = res.getString(R.string.list_all_lists)
                markerId = EmojiUtils.NO_EMOJI
                preventAskForDeletion = true
            } else {
                val list: StoredList = DataStore.getList(id)
                listId = list.id
                title = list.title
                markerId = list.markerId
                preventAskForDeletion = list.preventAskForDeletion
            }
            type = CacheListType.OFFLINE

            if (previousListType != type) {
                currentCacheFilter = GeocacheFilterContext(type.filterContextType)
            }
            restartCacheLoader(false, e -> {
                e.putSerializable(BUNDLE_ACTION_KEY, action)
                e.putAll(OfflineGeocacheListLoader.getBundleForList(listId))
            })

            Settings.setLastDisplayedList(listId)
        }

        sortContext = GeocacheSortContext.getFor(type, "" + listId)

        initAdapter()
        setFilter()
        showProgress(true)
        refreshListFooter()
        adapter.setSelectMode(false)
        invalidateOptionsMenuCompatible()
    }

    private Unit renameList() {
        (RenameListCommand(this, listId) {

            override             protected Unit onFinished() {
                refreshCurrentList()
            }
        }).execute()
    }

    private Unit removeListInternal() {
        DeleteListCommand(this, listId) {

            private String oldListName

            override             protected Boolean canExecute() {
                oldListName = DataStore.getList(listId).getTitle()
                return super.canExecute()
            }

            override             protected Unit onFinished() {
                refreshActionBarTitle()
                switchListById(StoredList.STANDARD_LIST_ID)
            }

            override             protected Unit onFinishedUndo() {
                refreshActionBarTitle()
                for (final StoredList list : DataStore.getLists()) {
                    if (oldListName == (list.getTitle())) {
                        switchListById(list.id)
                    }
                }
            }

        }.execute()
    }

    private Unit removeList() {
        // if there are no caches on this list, don't bother the user with questions.
        // there is no harm in deleting the list, he could recreate it easily
        if (adapter.isEmpty()) {
            removeListInternal()
            return
        }

        // ask him, if there are caches on the list
        SimpleDialog.of(this).setTitle(R.string.list_dialog_remove_title).setMessage(R.string.list_dialog_remove_description)
                .setPositiveButton(TextParam.id(R.string.list_dialog_remove)).confirm(this::removeListInternal)
    }

    public Unit goMap() {
        if (!cacheToShow()) {
            return
        }

        // apply filter settings (if there's a filter)
        val searchToUse: SearchResult = getFilteredSearch()
        if (Settings.useLegacyMaps() || listId == 0) {
            DefaultMap.startActivitySearch(this, searchToUse, title, listId)
        } else {
            DefaultMap.startActivityList(this, listId, currentCacheFilter)
        }
        ActivityMixin.overrideTransitionToFade(this)
    }

    private Unit refreshCurrentList() {
        refreshCurrentList(AfterLoadAction.NO_ACTION)
    }

    private Unit refreshCurrentList(final AfterLoadAction action) {
        // do not refresh any of the dynamic search result lists but history, which might have been cleared
        if (!type.isStoredInDatabase) {
            return
        }

        val lph: LastPositionHelper = LastPositionHelper(this)
        refreshActionBarTitle()
        switchListById(listId, action)
        lph.setLastListPosition()
    }

    public static Intent getActivityOfflineIntent(final Context context) {
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.OFFLINE)
        return cachesIntent
    }

    public static Unit startActivityOffline(final Context context) {
        context.startActivity(getActivityOfflineIntent(context))
    }

    public static Unit startActivityOwner(final Context context, final String userName) {
        if (!checkNonBlankUsername(context, userName)) {
            return
        }
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.OWNER)
        cachesIntent.putExtra(Intents.EXTRA_USERNAME, userName)
        context.startActivity(cachesIntent)
    }

    public static Unit startActivityFilter(final Context context) {
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.SEARCH_FILTER)
        context.startActivity(cachesIntent)
    }

    /**
     * Check if a given username is valid (non blank), and show a toast if it isn't.
     *
     * @param context  an activity
     * @param username the username to check
     * @return <tt>true</tt> if the username is not blank, <tt>false</tt> otherwise
     */
    private static Boolean checkNonBlankUsername(final Context context, final String username) {
        if (StringUtils.isBlank(username)) {
            ActivityMixin.showToast(context, R.string.warn_no_username)
            return false
        }
        return true
    }

    public static Unit startActivityFinder(final Context context, final String userName) {
        if (!checkNonBlankUsername(context, userName)) {
            return
        }
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.FINDER)
        cachesIntent.putExtra(Intents.EXTRA_USERNAME, userName)
        context.startActivity(cachesIntent)
    }

    private Unit updateFilterBar() {
        FilterUtils.updateFilterBar(this, getActiveFilterName(), getActiveFilterSavedDifferently())
    }

    private String getActiveFilterName() {
        if (currentCacheFilter.get().isFiltering()) {
            return currentCacheFilter.get().toUserDisplayableString()
        }
        return null
    }

    private Boolean getActiveFilterSavedDifferently() {
        if (currentCacheFilter.get().isFiltering()) {
            return currentCacheFilter.get().isSavedDifferently()
        }
        return null
    }

    public static Intent getNearestIntent(final Activity context) {
        return Intents.putListType(Intent(context, CacheListActivity.class), CacheListType.NEAREST)
    }

    public static Intent getHistoryIntent(final Context context) {
        return Intents.putListType(Intent(context, CacheListActivity.class), CacheListType.HISTORY)
    }

    public static Intent getHistoryIntent(final Context context, final IConnector connector) {
        val historyIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(historyIntent, CacheListType.HISTORY)
        historyIntent.putExtra(Intents.EXTRA_CONNECTOR, connector.getName())
        return historyIntent
    }

    public static Unit startActivityAddress(final Context context, final Geopoint coords, final String address) {
        val addressIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(addressIntent, CacheListType.ADDRESS)
        addressIntent.putExtra(Intents.EXTRA_COORDS, coords)
        addressIntent.putExtra(Intents.EXTRA_ADDRESS, address)
        context.startActivity(addressIntent)
    }

    /**
     * start list activity, by searching around the given point.
     *
     * @param name name of coordinates, will lead to a title like "Around ..." instead of directly showing the
     *             coordinates as title
     */
    public static Unit startActivityCoordinates(final AbstractActivity context, final Geopoint coords, final String name) {
        if (!isValidCoords(context, coords)) {
            return
        }
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.COORDINATE)
        cachesIntent.putExtra(Intents.EXTRA_COORDS, coords)
        if (StringUtils.isNotEmpty(name)) {
            cachesIntent.putExtra(Intents.EXTRA_TITLE, context.getString(R.string.around, name))
        }
        context.startActivity(cachesIntent)
    }

    private static Boolean isValidCoords(final AbstractActivity context, final Geopoint coords) {
        if (coords == null) {
            context.showToast(CgeoApplication.getInstance().getString(R.string.warn_no_coordinates))
            return false
        }
        return true
    }

    public static Unit startActivityKeyword(final AbstractActivity context, final String keyword) {
        if (keyword == null) {
            context.showToast(CgeoApplication.getInstance().getString(R.string.warn_no_keyword))
            return
        }
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.KEYWORD)
        cachesIntent.putExtra(Intents.EXTRA_KEYWORD, keyword)
        context.startActivity(cachesIntent)
    }

    public static Unit startActivityMap(final Context context, final SearchResult search) {
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        cachesIntent.putExtra(Intents.EXTRA_SEARCH, search)
        Intents.putListType(cachesIntent, CacheListType.MAP)
        context.startActivity(cachesIntent)
    }

    public static Unit startActivityLastViewed(final Context context, final SearchResult search) {
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        cachesIntent.putExtra(Intents.EXTRA_SEARCH, search)
        Intents.putListType(cachesIntent, CacheListType.LAST_VIEWED)
        context.startActivity(cachesIntent)
    }

    public static Unit startActivityPocketDownload(final Context context, final List<GCList> pocketQueries) {
        if (pocketQueries.isEmpty()) {
            return
        }

        val intent: Intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        val pqName: String = (1 == pocketQueries.size()) ? pocketQueries.get(0).getName() : ""
        intent.putExtra(Intents.EXTRA_NAME, pqName)
        intent.putParcelableArrayListExtra(Intents.EXTRA_POCKET_LIST, ArrayList<>(pocketQueries))
        intent.setClass(context, CacheListActivity.class)
        context.startActivity(intent)
    }

    public static Unit startActivityPocket(final Context context, final List<GCList> pocketQueries) {
        if (pocketQueries.isEmpty()) {
            return
        }

        val pqName: String = (1 == pocketQueries.size()) ? pocketQueries.get(0).getName() : ""
        val cachesIntent: Intent = Intent(context, CacheListActivity.class)
        Intents.putListType(cachesIntent, CacheListType.POCKET)
        cachesIntent.putExtra(Intents.EXTRA_NAME, pqName)
        cachesIntent.putParcelableArrayListExtra(Intents.EXTRA_POCKET_LIST, ArrayList<>(pocketQueries))
        context.startActivity(cachesIntent)
    }

    // Loaders

    override     public Loader<SearchResult> onCreateLoader(final Int type, final Bundle extras) {
        if (type != CACHE_LOADER_ID || extras == null) {
            throw IllegalArgumentException("invalid loader type " + type + " or empty extras: " + extras)
        }
        val isNextPage: Boolean = extras.getBoolean(EXTRAS_NEXTPAGE, false)
        AbstractSearchLoader loader = null
        preventAskForDeletion = true

        if (isNextPage) {
            loader = NextPageGeocacheListLoader(this, search)
        } else {
            switch (this.type) {
                case OFFLINE:
                    // open either the requested or the last list
                    if (extras.containsKey(Intents.EXTRA_LIST_ID)) {
                        listId = extras.getInt(Intents.EXTRA_LIST_ID)
                    } else {
                        listId = Settings.getLastDisplayedList()
                    }
                    if (listId == PseudoList.ALL_LIST.id) {
                        title = res.getString(R.string.list_all_lists)
                        markerId = EmojiUtils.NO_EMOJI
                    } else if (listId <= StoredList.TEMPORARY_LIST.id) {
                        listId = StoredList.STANDARD_LIST_ID
                        title = res.getString(R.string.stored_caches_button)
                        markerId = EmojiUtils.NO_EMOJI
                    } else {
                        val list: StoredList = DataStore.getList(listId)
                        // list.id may be different if listId was not valid
                        if (list.id != listId) {
                            showToast(getString(R.string.list_not_available))
                        }
                        listId = list.id
                        title = list.title
                        markerId = list.markerId
                        preventAskForDeletion = list.preventAskForDeletion
                    }

                    loader = OfflineGeocacheListLoader(this, coords, listId, currentCacheFilter.get(), sortContext.getSort().getComparator(), false, offlineListLoadLimit)

                    break
                case HISTORY:
                    title = res.getString(R.string.caches_history)
                    listId = PseudoList.HISTORY_LIST.id
                    markerId = EmojiUtils.NO_EMOJI

                    final GeocacheFilter offlineFilter
                    val connectorName: String = extras.getString(Intents.EXTRA_CONNECTOR)
                    if (null == connectorName) {
                        offlineFilter = currentCacheFilter.get()
                    } else {
                        val connector: IConnector = ConnectorFactory.getConnectorByName(connectorName)
                        val connectorAddFilter: OriginGeocacheFilter = GeocacheFilterType.ORIGIN.create()
                        connectorAddFilter.setValues(Collections.singletonList(connector))
                        offlineFilter = GeocacheFilter.createEmpty().and(connectorAddFilter)
                    }
                    loader = OfflineGeocacheListLoader(this, coords, PseudoList.HISTORY_LIST.id, offlineFilter, VisitComparator.singleton, sortContext.getSort().isInverse(), offlineListLoadLimit)

                    break
                case NEAREST:
                    title = res.getString(R.string.caches_nearby)
                    markerId = EmojiUtils.NO_EMOJI
                    loader = CoordsGeocacheListLoader(this, sortContext.getSort(), coords, true)
                    break
                case COORDINATE:
                    title = coords.toString()
                    markerId = EmojiUtils.NO_EMOJI
                    loader = CoordsGeocacheListLoader(this, sortContext.getSort(), coords, false)
                    break
                case KEYWORD:
                    val keyword: String = extras.getString(Intents.EXTRA_KEYWORD)
                    markerId = EmojiUtils.NO_EMOJI
                    title = listNameMemento.rememberTerm(keyword)
                    if (keyword != null) {
                        loader = KeywordGeocacheListLoader(this, sortContext.getSort(), keyword)
                    }
                    break
                case ADDRESS:
                    val address: String = extras.getString(Intents.EXTRA_ADDRESS)
                    if (StringUtils.isNotBlank(address)) {
                        title = listNameMemento.rememberTerm(address)
                    } else {
                        title = coords.toString()
                    }
                    markerId = EmojiUtils.NO_EMOJI
                    loader = CoordsGeocacheListLoader(this, sortContext.getSort(), coords, false)
                    break
                case FINDER:
                    val username: String = extras.getString(Intents.EXTRA_USERNAME)
                    title = listNameMemento.rememberTerm(username)
                    markerId = EmojiUtils.NO_EMOJI
                    if (username != null) {
                        loader = FinderGeocacheListLoader(this, sortContext.getSort(), username)
                    }
                    break
                case SEARCH_FILTER:
                    markerId = EmojiUtils.NO_EMOJI
                    loader = SearchFilterGeocacheListLoader(this, currentCacheFilter.get(), sortContext.getSort())
                    break
                case OWNER:
                    val ownerName: String = extras.getString(Intents.EXTRA_USERNAME)
                    title = listNameMemento.rememberTerm(ownerName)
                    markerId = EmojiUtils.NO_EMOJI
                    if (ownerName != null) {
                        loader = OwnerGeocacheListLoader(this, sortContext.getSort(), ownerName)
                    }
                    break
                case MAP:
                    title = res.getString(R.string.map_map)
                    markerId = EmojiUtils.NO_EMOJI
                    search = (SearchResult) extras.get(Intents.EXTRA_SEARCH)
                    replaceCacheListFromSearch()
                    //loadCachesHandler.sendMessage(Message.obtain())
                    loader = NullGeocacheListLoader(this, search)
                    break
                case LAST_VIEWED:
                    title = res.getString(R.string.cache_recently_viewed)
                    markerId = EmojiUtils.NO_EMOJI
                    search = (SearchResult) extras.get(Intents.EXTRA_SEARCH)
                    replaceCacheListFromSearch()
                    //loadCachesHandler.sendMessage(Message.obtain())
                    loader = NullGeocacheListLoader(this, search)
                    break
                case POCKET:
                    val cachesLists: List<GCList> = extras.getParcelableArrayList(Intents.EXTRA_POCKET_LIST)
                    title = listNameMemento.rememberTerm(extras.getString(Intents.EXTRA_NAME))
                    markerId = EmojiUtils.NO_EMOJI
                    loader = GCListLoader(this, cachesLists)
                    break
                default:
                    //can never happen, makes Codacy happy
                    break
            }
        }
        // if there is a title given in the activity start request, use this one instead of the default
        if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_TITLE))) {
            title = extras.getString(Intents.EXTRA_TITLE)
        }
        if (loader != null && extras.getSerializable(BUNDLE_ACTION_KEY) != null) {
            val action: AfterLoadAction = (AfterLoadAction) extras.getSerializable(BUNDLE_ACTION_KEY)
            loader.setAfterLoadAction(action)
        }
        if (loader != null) {
            if (loader is LiveFilterGeocacheListLoader) {
                currentAddFilterCriteria = ((LiveFilterGeocacheListLoader) loader).getAdditionalFilterParameter()
            } else {
                currentAddFilterCriteria = null
            }
        }
        updateTitle()
        showProgress(true)
        refreshListFooter()

        if (loader == null) {
            Log.w("LOADER IS NULL!!!")
        }

        currentLoader = loader
        return loader
    }

    override     public Unit onLoadFinished(final Loader<SearchResult> arg0, final SearchResult searchIn) {
        // The database search was moved into the UI call intentionally. If this is done before the runOnUIThread,
        // then we have 2 sets of caches in memory. This can lead to OOM for huge cache lists.
        if (searchIn != null) {
            val cachesFromSearchResult: Set<Geocache> = searchIn.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)
            val lph: LastPositionHelper = LastPositionHelper(this)
            adapter.setList(cachesFromSearchResult)
            search = searchIn
            updateGui()
            lph.setLastListPosition()

            if (search.getError() != StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.err_download_fail) + ' ' + search.getError().getErrorString() + '.')
            }
        }
        showProgress(false)
        invalidateOptionsMenuCompatible()
        if (arg0 is AbstractSearchLoader) {
            switch (((AbstractSearchLoader) arg0).getAfterLoadAction()) {
                case CHECK_IF_EMPTY:
                    checkIfEmptyAndRemoveAfterConfirm()
                    break
                case NO_ACTION:
                    break
            }
        }
    }

    override     public Unit onLoaderReset(final Loader<SearchResult> arg0) {
        //Not interesting
    }

    /**
     * Calculate the subtitle of the current list depending on (optional) filters.
     */
    private String getCurrentSubtitle() {
        if (search == null) {
            return getCacheNumberString(getResources(), 0)
        }

        val totalCount: Int = type.isStoredInDatabase ? search.getTotalCount() : search.getCount()

        val result: StringBuilder = StringBuilder()
        val isFiltered: Boolean = adapter.hasActiveFilter() && adapter.getCount() != totalCount
        if (isFiltered || resultIsOfflineAndLimited()) {
            result.append(adapter.getCount())
            if (resultIsOfflineAndLimited()) {
                result.append("+")
            }
            result.append('/')
        }
        result.append(getCacheNumberString(getResources(), totalCount))

        return result.toString()
    }

    private Boolean resultIsOfflineAndLimited() {
        return type.isStoredInDatabase && offlineListLoadLimit > 0 && search != null && search.getTotalCount() > offlineListLoadLimit && search.getCount() == offlineListLoadLimit
    }

    /**
     * Used to indicate if an action should be taken after the AbstractSearchLoader has finished
     */
    enum class class AfterLoadAction {
        /**
         * Take no action
         */
        NO_ACTION,
        /**
         * Check if the list is empty and prompt for deletion
         */
        CHECK_IF_EMPTY
    }

    public static Int getOfflineListInitialLoadLimit() {
        return Settings.getListInitialLoadLimit()
    }

    public static Int getOfflineListLimitIncrease() {
        return 100
    }

    private Unit showProgress(final Boolean loading) {
        val progressBar: View = findViewById(R.id.loading)
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE)
        val list: ListView = getListView()
        list.setVisibility(loading ? View.GONE : View.VISIBLE)
    }

    private Boolean openSortDialog() {
        final List<Pair<GeocacheSort.SortType, String>> availableTypes = sortContext.getSort().getAvailableTypes()
        val typeList: List<GeocacheSort.SortType> = ArrayList<>()
        val typeToString: HashMap<GeocacheSort.SortType, String> = HashMap<>()
        for (Pair<GeocacheSort.SortType, String> entry : availableTypes) {
            typeToString.put(entry.first, entry.second)
            typeList.add(entry.first)
        }

        final SimpleDialog.ItemSelectModel<GeocacheSort.SortType> model = SimpleDialog.ItemSelectModel<>()
        model
                .setScrollAnchor(sortContext.getSort().getType())
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO)
                .setItems(typeList)
                .setSelectedItems(Collections.singleton(sortContext.getSort().getType()))
                .setDisplayMapper((f) -> TextParam.text(typeToString.get(f)))

        SimpleDialog.of(this).setTitle(R.string.caches_sort)
                .selectSingle(model, this::refreshWithSortType)

        return true
    }

    private Boolean refreshWithSortType(final GeocacheSort.SortType sortType) {
        sortContext.getSort().setAndToggle(sortType)
        sortContext.save()
        adapter.forceSort()
        updateSortBar()
        refreshCurrentList()
        //for online searches, restart search with sort argument
        if (type.isOnline && type != CacheListType.POCKET) {
            restartCacheLoader(false, null)
        }

        return true
    }
}
