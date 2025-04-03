package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cachelist.CacheListApp;
import cgeo.geocaching.apps.cachelist.CacheListAppUtils;
import cgeo.geocaching.apps.cachelist.CacheListApps;
import cgeo.geocaching.apps.cachelist.ListNavigationSelectionActionProvider;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.command.AbstractCachesCommand;
import cgeo.geocaching.command.CopyToListCommand;
import cgeo.geocaching.command.DeleteListCommand;
import cgeo.geocaching.command.MakeListUniqueCommand;
import cgeo.geocaching.command.MoveToListAndRemoveFromOthersCommand;
import cgeo.geocaching.command.MoveToListCommand;
import cgeo.geocaching.command.RenameListCommand;
import cgeo.geocaching.command.SetCacheIconCommand;
import cgeo.geocaching.connector.gc.BookmarkListActivity;
import cgeo.geocaching.connector.gc.BookmarkUtils;
import cgeo.geocaching.connector.gc.PocketQueryListActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.export.BatchUploadModifiedCoordinates;
import cgeo.geocaching.export.FieldNoteExport;
import cgeo.geocaching.export.GpxExport;
import cgeo.geocaching.export.PersonalNoteExport;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.list.ListNameMemento;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.loaders.AbstractSearchLoader;
import cgeo.geocaching.loaders.CoordsGeocacheListLoader;
import cgeo.geocaching.loaders.FinderGeocacheListLoader;
import cgeo.geocaching.loaders.KeywordGeocacheListLoader;
import cgeo.geocaching.loaders.LiveFilterGeocacheListLoader;
import cgeo.geocaching.loaders.NextPageGeocacheListLoader;
import cgeo.geocaching.loaders.NullGeocacheListLoader;
import cgeo.geocaching.loaders.OfflineGeocacheListLoader;
import cgeo.geocaching.loaders.OwnerGeocacheListLoader;
import cgeo.geocaching.loaders.PocketGeocacheListLoader;
import cgeo.geocaching.loaders.SearchFilterGeocacheListLoader;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LoggingUI;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.DownloadProgress;
import cgeo.geocaching.network.Send2CgeoDownloader;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.sorting.GeocacheSort;
import cgeo.geocaching.sorting.GeocacheSortContext;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.CacheListActionBarChooser;
import cgeo.geocaching.ui.CacheListAdapter;
import cgeo.geocaching.ui.FastScrollListener;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ToggleItemType;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.CheckboxDialogConfig;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.HideActionBarUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.MenuUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.util.Consumer;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

public class CacheListActivity extends AbstractListActivity implements FilteredActivity, LoaderManager.LoaderCallbacks<SearchResult> {

    private static final int CACHE_LOADER_ID = 5; //arbitrary number, but must be fixed
    private static final String EXTRAS_NEXTPAGE = "extras_nextpage";

    private static final int REFRESH_WARNING_THRESHOLD = 100;

    private static final int REQUEST_CODE_IMPORT_PQ = 10003;
    private static final int REQUEST_CODE_IMPORT_BOOKMARK = 10004;

    private static final String STATE_GEOCACHE_FILTER = "currentGeocacheFilter";
    private static final String STATE_SORT_CONTEXT = "currentSortContext";
    private static final String STATE_LIST_TYPE = "currentListType";
    private static final String STATE_TYPE_PARAMETERS = "currentTypeParameters";
    private static final String STATE_LIST_ID = "currentListId";
    private static final String STATE_MARKER_ID = "currentMarkerId";
    private static final String STATE_PREVENTASKFORDELETION = "preventAskForDeletion";
    private static final String STATE_CONTENT_STORAGE_ACTIVITY_HELPER = "contentStorageActivityHelper";
    private static final String STATE_OFFLINELISTLOADLIMIT_ID = "offlineListLoadLimit";
    private static final String STATE_CHECKFOREMPTYLIST = "checkForEmptyList";

    private static final String BUNDLE_ACTION_KEY = "afterLoadAction";

    private CacheListType type = null;
    private final Bundle typeParameters = new Bundle();
    private Geopoint coords = null;
    private GeocacheSortContext sortContext;
    private SearchResult search = null;
    private boolean checkForEmtpyList = true;

    private final CacheListActionBarChooser actionBarChooser =
        new CacheListActionBarChooser(this, this::getSupportActionBar, this::switchListById);
    private CacheListAdapter adapter = null;
    private View listFooter = null;
    private TextView listFooterLine1 = null;
    private TextView listFooterLine2 = null;
    private final Progress progress = new Progress();
    private String title = "";
    private final AtomicInteger detailProgress = new AtomicInteger(0);
    private int listId = StoredList.TEMPORARY_LIST.id; // Only meaningful for the OFFLINE type
    private int markerId = EmojiUtils.NO_EMOJI;
    private boolean preventAskForDeletion = false;
    private int offlineListLoadLimit = getOfflineListInitialLoadLimit();

    /**
     * remember current filter when switching between lists, so it can be re-applied afterwards
     */
    private GeocacheFilterContext currentCacheFilter = null;
    private IGeocacheFilter currentAddFilterCriteria = null;

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {

        @Override
        public void updateDirection(final float direction) {
            if (Settings.isLiveList()) {
                adapter.setActualHeading(AngleUtils.getDirectionNow(direction));
            }
        }

        @Override
        public void updateGeoData(final GeoData geoData) {
            adapter.setActualCoordinates(geoData.getCoords());
        }

    };

    private ContextMenuInfo lastMenuInfo;
    private String contextMenuGeocode = "";
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private final ListNameMemento listNameMemento = new ListNameMemento();

    private final DisposableHandler clearOfflineLogsHandler = new ClearOfflineLogsHandler(this);
    private final Handler importGpxAttachementFinishedHandler = new ImportGpxAttachementFinishedHandler(this);

    private ContentStorageActivityHelper contentStorageActivityHelper = null;

    private AbstractSearchLoader currentLoader;

    @Override
    public int getSelectedBottomItemId() {
        return type.navigationMenuItem;
    }

    @Override
    public void onNavigationItemReselected(@NonNull final MenuItem item) {
        if (item.getItemId() == MENU_SEARCH || item.getItemId() == MENU_MAP) {
            ActivityMixin.finishWithFadeTransition(this);
        }
    }

    /**
     * Loads the caches and fills the adapter according to {@link #search} content.
     * <br>
     * If {@link #search} is {@code null}, this does nothing.
     */

    private void replaceCacheListFromSearch() {
        if (search != null) {
            runOnUiThread(() -> {


                // The database search was moved into the UI call intentionally. If this is done before the runOnUIThread,
                // then we have 2 sets of caches in memory. This can lead to OOM for huge cache lists.
                final Set<Geocache> cachesFromSearchResult = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                final LastPositionHelper lph = new LastPositionHelper(this);
                adapter.setList(cachesFromSearchResult);
                updateGui();
                lph.setLastListPosition();
            });
        }
    }

    private static String getCacheNumberString(final Resources res, final int count) {
        return res.getQuantityString(R.plurals.cache_counts, count, count);
    }

    protected void updateTitle() {
        setTitle(title);
        adapter.setCurrentListTitle(title);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(getCurrentSubtitle());
        }
        refreshActionBarTitle();
    }

    /**
     * TODO Possibly parts should be a Thread not a Handler
     */
    private static final class DownloadFromWebHandler extends DisposableHandler {
        private final WeakReference<CacheListActivity> activityRef;

        DownloadFromWebHandler(final CacheListActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            final CacheListActivity activity = activityRef.get();
            if (activity != null) {
                activity.updateAdapter();

                final CacheListAdapter adapter = activity.adapter;
                adapter.notifyDataSetChanged();

                final Progress progress = activity.progress;
                switch (msg.what) {
                    case DownloadProgress.MSG_WAITING:  //no caches
                        progress.setMessage(activity.res.getString(R.string.web_import_waiting));
                        break;
                    case DownloadProgress.MSG_LOADING: {  //cache downloading
                        final Resources res = activity.res;
                        progress.setMessage(res.getString(R.string.web_downloading) + ' ' + msg.obj + res.getString(R.string.ellipsis));
                        break;
                    }
                    case DownloadProgress.MSG_LOADED: {  //Cache downloaded
                        final Resources res = activity.res;
                        progress.setMessage(res.getString(R.string.web_downloaded) + ' ' + msg.obj + res.getString(R.string.ellipsis));
                        activity.refreshCurrentList();
                        break;
                    }
                    case DownloadProgress.MSG_SERVER_FAIL:
                        progress.dismiss();
                        activity.showToast(activity.res.getString(R.string.sendToCgeo_download_fail));
                        activity.finish();
                        break;
                    case DownloadProgress.MSG_NO_REGISTRATION:
                        progress.dismiss();
                        activity.showToast(activity.res.getString(R.string.sendToCgeo_no_registration));
                        activity.finish();
                        break;
                    default:  // MSG_DONE
                        adapter.setSelectMode(false);
                        activity.replaceCacheListFromSearch();
                        progress.dismiss();
                        break;
                }
            }
        }
    }

    private static final class ClearOfflineLogsHandler extends DisposableHandler {
        private final WeakReference<CacheListActivity> activityRef;

        ClearOfflineLogsHandler(final CacheListActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            final CacheListActivity activity = activityRef.get();
            if (activity != null) {
                activity.adapter.setSelectMode(false);

                activity.refreshCurrentList();

                activity.replaceCacheListFromSearch();

                activity.progress.dismiss();
            }
        }
    }

    private static final class ImportGpxAttachementFinishedHandler extends WeakReferenceHandler<CacheListActivity> {

        ImportGpxAttachementFinishedHandler(final CacheListActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            final CacheListActivity activity = getReference();
            if (activity != null) {
                activity.refreshCurrentList();
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();

        this.contentStorageActivityHelper = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CONTENT_STORAGE_ACTIVITY_HELPER))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_MULTIPLE, List.class, this::importGpx);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        typeParameters.clear();
        Geopoint extraTargetCoords = null;
        if (extras != null) {
            typeParameters.putAll(extras);
            type = Intents.getListType(getIntent());
            coords = extras.getParcelable(Intents.EXTRA_COORDS);
            extraTargetCoords = extras.getParcelable(Intents.EXTRA_COORDS);
        }
        if (isInvokedFromAttachment()) {
            type = CacheListType.OFFLINE;
            if (coords == null) {
                coords = Geopoint.ZERO;
            }
        }
        if (type == CacheListType.NEAREST) {
            coords = LocationDataProvider.getInstance().currentGeo().getCoords();
        }

        setTitle(title);
        HideActionBarUtils.setContentView(this, R.layout.cacheslist_activity, false);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            currentCacheFilter = savedInstanceState.getParcelable(STATE_GEOCACHE_FILTER);
            sortContext = savedInstanceState.getParcelable(STATE_SORT_CONTEXT);
            type = CacheListType.values()[savedInstanceState.getInt(STATE_LIST_TYPE, type.ordinal())];
            typeParameters.clear();
            typeParameters.putAll(savedInstanceState.getBundle(STATE_TYPE_PARAMETERS));
            listId = savedInstanceState.getInt(STATE_LIST_ID);
            markerId = savedInstanceState.getInt(STATE_MARKER_ID);
            preventAskForDeletion = savedInstanceState.getBoolean(STATE_PREVENTASKFORDELETION);
            offlineListLoadLimit = savedInstanceState.getInt(STATE_OFFLINELISTLOADLIMIT_ID);
            checkForEmtpyList = savedInstanceState.getBoolean(STATE_CHECKFOREMPTYLIST);
        } else {
            sortContext = GeocacheSortContext.getFor(type, "" + listId);
            sortContext.getSort().setTargetCoords(extraTargetCoords);
            offlineListLoadLimit = getOfflineListInitialLoadLimit();
            currentCacheFilter = new GeocacheFilterContext(type.filterContextType);
            checkForEmtpyList = true;
        }

        initAdapter();

        FilterUtils.initializeFilterBar(this, this);
        updateFilterBar();

        restartCacheLoader(false, null);
        refreshListFooter();

        if (isInvokedFromAttachment()) {
            if (extras != null && !StringUtils.isBlank(extras.getString(Intents.EXTRA_NAME))) {
                listNameMemento.rememberTerm(extras.getString(Intents.EXTRA_NAME));
            } else {
                final String data = ContentStorage.get().getName(getIntent().getData());
                listNameMemento.rememberTerm(StringUtils.isNotBlank(data) && StringUtils.endsWith(data.toLowerCase(Locale.ROOT), ".gpx") ? StringUtils.substring(data, 0, -4) : data);
            }
            importGpxAttachement();
        }

        getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(this) {
            @Override
            protected void onReceive(final Context context, final String geocode) {
                if (IterableUtils.matchesAny(adapter.getFilteredList(), geocache -> geocache.getGeocode().equals(geocode))) {
                    final Geocache geocache = DataStore.loadCache(geocode, EnumSet.of(LoadFlags.LoadFlag.DB_MINIMAL));
                    if (geocache != null) {
                        adapter.setElement(geocache);
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        // Save the current Filter
        savedInstanceState.putParcelable(STATE_GEOCACHE_FILTER, currentCacheFilter);
        savedInstanceState.putParcelable(STATE_SORT_CONTEXT, sortContext);
        savedInstanceState.putInt(STATE_LIST_TYPE, type.ordinal());
        savedInstanceState.putBundle(STATE_TYPE_PARAMETERS, typeParameters);
        savedInstanceState.putInt(STATE_LIST_ID, listId);
        savedInstanceState.putInt(STATE_MARKER_ID, markerId);
        savedInstanceState.putBoolean(STATE_PREVENTASKFORDELETION, preventAskForDeletion);
        savedInstanceState.putInt(STATE_OFFLINELISTLOADLIMIT_ID, offlineListLoadLimit);
        savedInstanceState.putBundle(STATE_CONTENT_STORAGE_ACTIVITY_HELPER, contentStorageActivityHelper.getState());
        savedInstanceState.putBoolean(STATE_CHECKFOREMPTYLIST, checkForEmtpyList);
    }

    private void refreshActionBarTitle() {
        if (type.canSwitch) {
            actionBarChooser.setList(listId, adapter.getCount(), resultIsOfflineAndLimited());
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (currentLoader != null && currentLoader.isLoading()) {
            refreshListFooter();
        }
    }

    private boolean isConcreteList() {
        return type == CacheListType.OFFLINE &&
                (listId == StoredList.STANDARD_LIST_ID || listId >= DataStore.customListIdOffset);
    }

    private boolean isInvokedFromAttachment() {
        final Intent intent = getIntent();
        return Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null;
    }

    private void importGpxAttachement() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.gpx_import_select_list_title, listId -> {
            new GPXImporter(CacheListActivity.this, listId, importGpxAttachementFinishedHandler).importGPX();
            switchListById(listId);
        }, true, Collections.singleton(0), -1, listNameMemento);
    }

    @Override
    public void onResume() {
        super.onResume();

        // save current position
        final LastPositionHelper lastPosition = new LastPositionHelper(this);

        // resume location access
        resumeDisposables.add(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA | GeoDirHandler.UPDATE_DIRECTION | GeoDirHandler.LOW_POWER, 250, TimeUnit.MILLISECONDS));

        adapter.setSelectMode(false);
        setAdapterCurrentCoordinates(true);
        lastPosition.refreshListAtLastPosition(checkForEmtpyList);
        checkForEmtpyList = true;

        if (search != null) {
            updateAdapter();
           // loadCachesHandler.sendEmptyMessage(0);
        }
    }

    private void setAdapterCurrentCoordinates(final boolean forceSort) {
        adapter.setActualCoordinates(LocationDataProvider.getInstance().currentGeo().getCoords());
        if (forceSort) {
            adapter.forceSort();
        }
    }

    @Override
    public void onPause() {
        resumeDisposables.clear();
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.cache_list_options, menu);

        ViewUtils.setForParentAndChildren(this.findViewById(R.id.sort_bar),
                v -> openSortDialog(),
                v -> refreshWithSortType(sortContext.getSort().getType()));

        ListNavigationSelectionActionProvider.initialize(menu.findItem(R.id.menu_cache_list_app_provider), app -> app.invoke(CacheListAppUtils.filterCoords(adapter.getList()), CacheListActivity.this, getFilteredSearch()));
        FilterUtils.initializeFilterMenu(this, this);
        MenuUtils.enableIconsInOverflowMenu(menu);
        MenuUtils.tintToolbarAndOverflowIcons(menu);

        return true;
    }

    public void updateSelectSwitchMenuItem(final MenuItem item) {
        ToggleItemType.SELECT_MODE.toggleMenuItem(item, adapter.isSelectMode());
    }

    public Geopoint getReferencePoint() {
        return this.coords;
    }


    /**
     * Menu items which are not at all usable with the current list type should be hidden.
     * Menu items which are usable with the current list type but not in the current situation should be disabled.
     */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean isHistory = type == CacheListType.HISTORY;
        final boolean isOffline = type == CacheListType.OFFLINE;
        final boolean isEmpty = adapter.isEmpty();
        final boolean isConcrete = isConcreteList();
        final boolean isNonDefaultList = isConcrete && listId != StoredList.STANDARD_LIST_ID;
        final List<CacheListApp> listNavigationApps = CacheListApps.getActiveApps();

        try {
            // toplevel menu items
            MenuUtils.setEnabled(menu, R.id.menu_show_on_map, !isEmpty);
            MenuUtils.setVisibleEnabled(menu, R.id.menu_sort, !isHistory, !isEmpty);

            MenuUtils.setEnabled(menu, R.id.menu_switch_select_mode, !isEmpty);
            updateSelectSwitchMenuItem(menu.findItem(R.id.menu_switch_select_mode));
            MenuUtils.setVisible(menu, R.id.menu_invert_selection, adapter.isSelectMode()); // exception to the general rule: only show in select mode
            MenuUtils.setVisible(menu, R.id.menu_select_next20, adapter.isSelectMode()); // same here
            MenuUtils.setVisible(menu, R.id.menu_select_next100, adapter.isSelectMode()); // same here

            MenuUtils.setVisibleEnabled(menu, R.id.menu_cache_list_app_provider, listNavigationApps.size() > 1, !isEmpty);

            // Manage Caches submenu
            MenuUtils.setEnabled(menu, R.id.menu_refresh_stored, !isEmpty);
            if (!isOffline && !isHistory) {
                menu.findItem(R.id.menu_refresh_stored).setTitle(R.string.caches_store_offline);
            }

            MenuUtils.setVisible(menu, R.id.menu_move_to_list, containsStoredCaches());
            setMenuItemLabel(menu, R.id.menu_move_to_list, R.string.caches_move_selected, R.string.caches_move_all);
            MenuUtils.setVisible(menu, R.id.menu_copy_to_list, containsStoredCaches());
            setMenuItemLabel(menu, R.id.menu_copy_to_list, R.string.caches_copy_selected, R.string.caches_copy_all);

            MenuUtils.setEnabled(menu, R.id.menu_add_to_route, !isEmpty);
            setMenuItemLabel(menu, R.id.menu_add_to_route, R.string.caches_append_to_route_selected, R.string.caches_append_to_route_all);
            MenuUtils.setVisible(menu, R.id.menu_delete_events, containsStoredPastEvents());
            MenuUtils.setVisibleEnabled(menu, R.id.menu_clear_offline_logs, isHistory || isOffline, !isEmpty && containsOfflineLogs());
            MenuUtils.setVisibleEnabled(menu, R.id.menu_remove_from_history, isHistory, !isEmpty);
            setMenuItemLabel(menu, R.id.menu_remove_from_history, R.string.cache_remove_from_history, R.string.cache_clear_history);

            final boolean removeFromDevice = removeWillDeleteFromDevice(listId);
            MenuUtils.setVisibleEnabled(menu, R.id.menu_drop_caches, (isHistory || containsStoredCaches()) && !removeFromDevice, !isEmpty);
            setMenuItemLabel(menu, R.id.menu_drop_caches, R.string.caches_remove_selected, R.string.caches_remove_all);
            MenuUtils.setVisibleEnabled(menu, R.id.menu_drop_caches_all_lists, isHistory || containsStoredCaches(), !isEmpty);
            setMenuItemLabel(menu, R.id.menu_drop_caches_all_lists, R.string.caches_remove_selected_completely, R.string.caches_remove_all_completely);

            if (isOffline || type == CacheListType.HISTORY) { // only offline list
                setMenuItemLabel(menu, R.id.menu_refresh_stored, R.string.caches_refresh_selected, R.string.caches_refresh_all);
            } else { // search and global list (all other than offline and history)
                setMenuItemLabel(menu, R.id.menu_refresh_stored, R.string.caches_store_selected, R.string.caches_store_offline);
            }
            MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_bookmarklist, Settings.isGCConnectorActive() && Settings.isGCPremiumMember(), !isEmpty);
            MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_modifiedcoords, Settings.isGCConnectorActive(), !isEmpty);
            MenuUtils.setVisibleEnabled(menu, R.id.menu_upload_allcoords, Settings.isGCConnectorActive(), !isEmpty);
            MenuUtils.setEnabled(menu, R.id.menu_show_attributes, !isEmpty);
            MenuUtils.setEnabled(menu, R.id.menu_set_cache_icon, !isEmpty);

            // Manage Lists submenu
            MenuUtils.setVisible(menu, R.id.menu_lists, isOffline);
            MenuUtils.setVisible(menu, R.id.menu_drop_list, isNonDefaultList);
            MenuUtils.setVisible(menu, R.id.menu_rename_list, isNonDefaultList);
            MenuUtils.setVisible(menu, R.id.menu_rename_list_prefix, isNonDefaultList && DataStore.getListHierarchy().size() > 1);
            MenuUtils.setVisibleEnabled(menu, R.id.menu_make_list_unique, listId != PseudoList.ALL_LIST.id, !isEmpty);
            MenuUtils.setVisible(menu, R.id.menu_set_listmarker, isNonDefaultList);
            MenuUtils.setVisible(menu, R.id.menu_set_askfordeletion, isNonDefaultList);
            MenuUtils.setEnabled(menu, R.id.menu_set_askfordeletion, preventAskForDeletion);

            // Import submenu
            MenuUtils.setVisible(menu, R.id.menu_import, isOffline && listId != PseudoList.ALL_LIST.id);
            MenuUtils.setEnabled(menu, R.id.menu_import_pq, Settings.isGCConnectorActive() && Settings.isGCPremiumMember());
            MenuUtils.setEnabled(menu, R.id.menu_bookmarklists, Settings.isGCConnectorActive() && Settings.isGCPremiumMember());

            // Export
            MenuUtils.setVisibleEnabled(menu, R.id.menu_export, isHistory || isOffline, !isEmpty);
        } catch (final RuntimeException e) {
            Log.e("CacheListActivity.onPrepareOptionsMenu", e);
        }
        MenuUtils.tintToolbarAndOverflowIcons(menu);

        return true;
    }

    private boolean containsStoredCaches() {
        for (final Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (cache.isOffline()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsStoredPastEvents() {
        for (final Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (cache.isOffline() && CalendarUtils.isPastEvent(cache)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsOfflineLogs() {
        for (final Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (cache.hasLogOffline()) {
                return true;
            }
        }
        return false;
    }

    private void setMenuItemLabel(final Menu menu, final int menuId, @StringRes final int resIdSelection, @StringRes final int resId) {
        final MenuItem menuItem = menu.findItem(menuId);
        if (menuItem == null) {
            return;
        }
        final boolean hasSelection = adapter != null && adapter.getCheckedCount() > 0;
        if (hasSelection) {
            menuItem.setTitle(res.getString(resIdSelection) + " (" + adapter.getCheckedCount() + ")");
        } else {
            menuItem.setTitle(res.getString(resId));
        }
    }

    private void setListMarker(final int newListMarker) {
        DataStore.setListEmoji(listId, newListMarker);
        markerId = newListMarker;
        MapMarkerUtils.resetLists();
        adapter.notifyDataSetChanged();
        refreshActionBarTitle();
        refreshCurrentList();
    }

    private void setPreventAskForDeletion(final boolean prevent) {
        DataStore.setListPreventAskForDeletion(listId, prevent);
        preventAskForDeletion = prevent;
        invalidateOptionsMenuCompatible();
    }

    private void setCacheIcons(final int newCacheIcon) {
        if (newCacheIcon == 0) {
            SimpleDialog.of(this).setTitle(R.string.caches_reset_cache_icons_title).setMessage(R.string.caches_reset_cache_icons_title).confirm(() -> setCacheIconsHelper(0));
        } else {
            setCacheIconsHelper(newCacheIcon);
        }
    }

    private void setCacheIconsHelper(final int newCacheIcon) {
        new SetCacheIconCommand(this, adapter.getCheckedOrAllCaches(), newCacheIcon) {
            @Override
            protected void onFinished() {
                adapter.setSelectMode(false);
                refreshCurrentList(AfterLoadAction.CHECK_IF_EMPTY);
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int menuItem = item.getItemId();
        if (menuItem == R.id.menu_show_on_map) {
            goMap();
        } else if (menuItem == R.id.menu_switch_select_mode) {
            adapter.switchSelectMode();
            updateSelectSwitchMenuItem(item);
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_refresh_stored) {
            refreshInBackground(adapter.getCheckedOrAllCaches());
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_add_to_route) {
            appendInBackground(adapter.getCheckedOrAllCaches());
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_drop_caches) {
            deleteCaches(adapter.getCheckedOrAllCaches(), false);
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_drop_caches_all_lists) {
            deleteCaches(adapter.getCheckedOrAllCaches(), true);
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_import_pq) {
            checkForEmtpyList = false;
            startListSelection(REQUEST_CODE_IMPORT_PQ);
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_bookmarklists) {
            checkForEmtpyList = false;
            startListSelection(REQUEST_CODE_IMPORT_BOOKMARK);
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_import_gpx) {
            importGpxSelectFiles();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_create_list) {
            new StoredList.UserInterface(this).promptForListCreation(getListSwitchingRunnable(), StringUtils.EMPTY);
            refreshActionBarTitle();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_drop_list) {
            removeList();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_rename_list) {
            renameList();
        } else if (menuItem == R.id.menu_rename_list_prefix) {
            new StoredList.UserInterface(this).promptForListPrefixRename(() -> {
                refreshCurrentList();
                invalidateOptionsMenuCompatible();
            });
        } else if (menuItem == R.id.menu_invert_selection) {
            adapter.invertSelection();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_select_next20) {
            adapter.selectNextCaches(20);
        } else if (menuItem == R.id.menu_select_next100) {
            adapter.selectNextCaches(100);
        } else if (menuItem == R.id.menu_filter) {
            showFilterMenu();
        } else if (menuItem == R.id.menu_sort) {
            openSortDialog();
        } else if (menuItem == R.id.menu_import_web) {
            importWeb();
        } else if (menuItem == R.id.menu_export_gpx) {
            new GpxExport().export(adapter.getCheckedOrAllCaches(), this, title);
        } else if (menuItem == R.id.menu_export_fieldnotes) {
            new FieldNoteExport().export(adapter.getCheckedOrAllCaches(), this);
        } else if (menuItem == R.id.menu_export_persnotes) {
            new PersonalNoteExport().export(adapter.getCheckedOrAllCaches(), this);
        } else if (menuItem == R.id.menu_upload_modifiedcoords) {
            final Activity that = this;
            SimpleDialog.of(this).setTitle(R.string.caches_upload_modifiedcoords).setMessage(R.string.caches_upload_modifiedcoords_warning).confirm(() -> new BatchUploadModifiedCoordinates(true).export(adapter.getCheckedOrAllCaches(), that));
        } else if (menuItem == R.id.menu_upload_allcoords) {
            final Activity that2 = this;
            SimpleDialog.of(this).setTitle(R.string.caches_upload_allcoords_dialogtitle).setMessage(R.string.caches_upload_allcoords_warning).confirm(() -> new BatchUploadModifiedCoordinates(false).export(adapter.getCheckedOrAllCaches(), that2));
        } else if (menuItem == R.id.menu_remove_from_history) {
            removeFromHistoryCheck();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_move_to_list) {
            moveCachesToOtherList(adapter.getCheckedOrAllCaches());
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_copy_to_list) {
            copyCachesToOtherList(adapter.getCheckedOrAllCaches());
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_delete_events) {
            deletePastEvents();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_create_internal_cache) {
            InternalConnector.interactiveCreateCache(this, coords, StoredList.getConcreteList(listId), false);
        } else if (menuItem == R.id.menu_clear_offline_logs) {
            clearOfflineLogs();
            invalidateOptionsMenuCompatible();
        } else if (menuItem == R.id.menu_show_attributes) {
            adapter.showAttributes();
        } else if (menuItem == R.id.menu_make_list_unique) {
            new MakeListUniqueCommand(this, listId) {

                @Override
                protected void onFinished() {
                    refreshActionBarTitle();
                }

                @Override
                protected void onFinishedUndo() {
                    refreshActionBarTitle();
                }

            }.execute();
        } else if (menuItem == R.id.menu_upload_bookmarklist) {
            BookmarkUtils.askAndUploadCachesToBookmarkList(this, adapter.getCheckedOrAllCaches());
        } else if (menuItem == R.id.menu_set_listmarker) {
            EmojiUtils.selectEmojiPopup(this, markerId, null, this::setListMarker);
        } else if (menuItem == R.id.menu_set_cache_icon) {
            EmojiUtils.selectEmojiPopup(this, -1, null, this::setCacheIcons);
        } else if (menuItem == R.id.menu_set_askfordeletion) {
            setPreventAskForDeletion(false);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void startListSelection(final int requestCode) {
        final Intent intent = new Intent(this, requestCode == REQUEST_CODE_IMPORT_PQ ? PocketQueryListActivity.class : BookmarkListActivity.class);
        intent.putExtra(Intents.EXTRA_PQ_LIST_IMPORT, true);
        startActivityForResult(intent, requestCode);
    }

    private void checkIfEmptyAndRemoveAfterConfirm() {
        final boolean isNonDefaultList = isConcreteList() && listId != StoredList.STANDARD_LIST_ID;
        // Check local cacheList first, and Datastore only if needed (because of filtered lists)
        // Checking is done in this order for performance reasons
        if (isNonDefaultList && !preventAskForDeletion && adapter.isEmpty()
                && DataStore.getAllStoredCachesCount(listId) == 0) {
            // ask user, if he wants to delete the now empty list
            Dialogs.confirmWithCheckbox(this, getString(R.string.list_dialog_remove), getString(R.string.list_dialog_remove_nowempty),
                    CheckboxDialogConfig.newCheckbox(R.string.list_dialog_do_not_ask_me_again)
                            .setActionButtonLabel(CheckboxDialogConfig.ActionButtonLabel.YES_NO)
                            .setPositiveButtonCheckCondition(CheckboxDialogConfig.CheckCondition.UNCHECKED),
                    preventAskForDeletion -> removeListInternal(), this::setPreventAskForDeletion);
        }
    }

    private boolean cacheToShow() {
        if (search == null || adapter.isEmpty()) {
            showToast(res.getString(R.string.warn_no_cache_coord));
            return false;
        }
        return true;
    }

    private SearchResult getFilteredSearch() {
        return new SearchResult(Geocache.getGeocodes(adapter.getFilteredList()));
    }

    private void deletePastEvents() {
        final List<Geocache> deletion = new ArrayList<>();
        for (final Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (CalendarUtils.isPastEvent(cache)) {
                deletion.add(cache);
            }
        }
        deleteCaches(deletion, false);
    }

    private void clearOfflineLogs() {
        SimpleDialog.of(this).setTitle(R.string.caches_clear_offlinelogs).setMessage(R.string.caches_clear_offlinelogs_message).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> {
            progress.show(CacheListActivity.this, null, res.getString(R.string.caches_clear_offlinelogs_progress), true, clearOfflineLogsHandler.disposeMessage());
            clearOfflineLogs(clearOfflineLogsHandler, adapter.getCheckedOrAllCaches());
        });
    }

    /**
     * called from the filter bar view
     */
    @Override
    public void showFilterMenu() {
        GeocacheFilterActivity.selectFilter(this, currentCacheFilter, adapter.getFilteredList(), !resultIsOfflineAndLimited());
    }

    /**
     * called from the filter bar view
     */
    @Override
    public boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, currentCacheFilter);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);

        AdapterContextMenuInfo adapterInfo = null;
        try {
            adapterInfo = (AdapterContextMenuInfo) info;
        } catch (final Exception e) {
            Log.w("CacheListActivity.onCreateContextMenu", e);
        }

        if (adapterInfo == null || adapterInfo.position >= adapter.getCount()) {
            return;
        }
        final Geocache cache = adapter.getItem(adapterInfo.position);
        assert cache != null;

        menu.setHeaderTitle(StringUtils.defaultIfBlank(cache.getName(), cache.getShortGeocode()));

        contextMenuGeocode = cache.getGeocode();

        getMenuInflater().inflate(R.menu.cache_list_context, menu);

        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
        final boolean hasCoords = cache.getCoords() != null;
        MenuUtils.setVisible(menu.findItem(R.id.menu_default_navigation), hasCoords);
        MenuUtils.setVisible(menu.findItem(R.id.menu_navigate), hasCoords);
        MenuUtils.setVisible(menu.findItem(R.id.menu_cache_details), hasCoords);
        final boolean isOffline = cache.isOffline();
        MenuUtils.setVisible(menu.findItem(R.id.menu_drop_cache), isOffline);
        MenuUtils.setVisible(menu.findItem(R.id.menu_move_to_list), isOffline);
        MenuUtils.setVisible(menu.findItem(R.id.menu_copy_to_list), isOffline);
        MenuUtils.setVisible(menu.findItem(R.id.menu_refresh), isOffline);
        MenuUtils.setVisible(menu.findItem(R.id.menu_store_cache), !isOffline);

        LoggingUI.onPrepareOptionsMenu(menu, cache);
    }

    private void moveCachesToOtherList(final Collection<Geocache> caches) {
        if (isConcreteList()) {
            new MoveToListCommand(this, caches, listId) {
                private LastPositionHelper lastPositionHelper;

                @Override
                protected void doCommand() {
                    lastPositionHelper = new LastPositionHelper(CacheListActivity.this);
                    super.doCommand();
                }

                @Override
                protected void onFinished() {
                    lastPositionHelper.refreshListAtLastPosition(true);
                }

            }.execute();
        } else {
            new MoveToListAndRemoveFromOthersCommand(this, caches) {
                private LastPositionHelper lastPositionHelper;

                @Override
                protected void doCommand() {
                    lastPositionHelper = new LastPositionHelper(CacheListActivity.this);
                    super.doCommand();
                }

                @Override
                protected void onFinished() {
                    lastPositionHelper.refreshListAtLastPosition(true);
                }

            }.execute();
        }
    }

    private void copyCachesToOtherList(final Collection<Geocache> caches) {
        new CopyToListCommand(this, caches, listId) {

            @Override
            protected void onFinished() {
                adapter.setSelectMode(false);
                refreshCurrentList(AfterLoadAction.CHECK_IF_EMPTY);
            }

        }.execute();
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        ContextMenu.ContextMenuInfo info = item.getMenuInfo();

        // restore menu info for sub menu items, see
        // https://code.google.com/p/android/issues/detail?id=7139
        if (info == null) {
            info = lastMenuInfo;
            lastMenuInfo = null;
        }

        AdapterContextMenuInfo adapterInfo = null;
        try {
            adapterInfo = (AdapterContextMenuInfo) info;
        } catch (final Exception e) {
            Log.w("CacheListActivity.onContextItemSelected", e);
        }

        final Geocache cache = adapterInfo != null ? getCacheFromAdapter(adapterInfo) : null;

        // just in case the list got resorted while we are executing this code
        if (cache == null || adapterInfo == null) {
            return true;
        }

        final int itemId = item.getItemId();
        if (itemId == R.id.menu_default_navigation) {
            NavigationAppFactory.startDefaultNavigationApplication(1, this, cache);
        } else if (itemId == R.id.menu_navigate) {
            NavigationAppFactory.showNavigationMenu(this, cache, null, null);
        } else if (itemId == R.id.menu_cache_details) {
            CacheDetailActivity.startActivity(this, cache.getGeocode(), cache.getName());
        } else if (itemId == R.id.menu_drop_cache) {
            deleteCaches(Collections.singletonList(cache), false);
        } else if (itemId == R.id.menu_move_to_list) {
            moveCachesToOtherList(Collections.singletonList(cache));
        } else if (itemId == R.id.menu_copy_to_list) {
            copyCachesToOtherList(Collections.singletonList(cache));
        } else if (itemId == R.id.menu_store_cache || itemId == R.id.menu_refresh) {
            CacheDownloaderService.refreshCache(this, cache.getGeocode(), itemId == R.id.menu_refresh, this::refreshCurrentList);
        } else {
            // we must remember the menu info for the sub menu, there is a bug
            // in Android:
            // https://code.google.com/p/android/issues/detail?id=7139
            lastMenuInfo = info;
            final View selectedView = adapterInfo.targetView;
            LoggingUI.onMenuItemSelected(item, this, cache, dialog -> {
                if (selectedView != null) {
                    final CacheListAdapter.ViewHolder holder = (CacheListAdapter.ViewHolder) selectedView.getTag();
                    if (holder != null) {
                        CacheListAdapter.updateViewHolder(holder, cache, res);
                    }
                }
            });
        }
        return true;
    }

    /**
     * Extract a cache from adapter data.
     *
     * @param adapterInfo an adapterInfo
     * @return the pointed cache
     */
    private Geocache getCacheFromAdapter(final AdapterContextMenuInfo adapterInfo) {
        final Geocache cache = adapter.getItem(adapterInfo.position);
        assert cache != null;
        if (cache.getGeocode().equalsIgnoreCase(contextMenuGeocode)) {
            return cache;
        }

        return adapter.findCacheByGeocode(contextMenuGeocode);
    }

    private void setFilter() {
        applyAdapterFilter();
        updateFilterBar();
        updateTitle();
        invalidateOptionsMenuCompatible();
    }

    private void applyAdapterFilter() {
        final GeocacheFilter filter = currentAddFilterCriteria == null ?
                currentCacheFilter.get() : currentCacheFilter.get().clone().and(currentAddFilterCriteria);
        adapter.setFilter(filter);
    }

    @Override
    public void onBackPressed() {
        if (adapter.isSelectMode()) {
            adapter.setSelectMode(false);
            invalidateOptionsMenu(); // update select mode icon
        } else {
            super.onBackPressed();
        }
    }

    private void initAdapter() {
        final ListView listView = getListView();
        registerForContextMenu(listView);

        adapter = new CacheListAdapter(this, adapter == null ? new ArrayList<>() : adapter.getList(), type, sortContext);
        adapter.setStoredLists(StoredList.UserInterface.getMenuLists(true, PseudoList.NEW_LIST.id));
        applyAdapterFilter();

        if (listFooter == null) {
            listFooter = getLayoutInflater().inflate(R.layout.cacheslist_footer, listView, false);
            listFooterLine1 = listFooter.findViewById(R.id.more_caches_1);
            listFooterLine2 = listFooter.findViewById(R.id.more_caches_2);
            listView.addFooterView(listFooter);
        }
        setListAdapter(adapter);

        adapter.forceSort();
        updateSortBar();

        listView.setOnScrollListener(new FastScrollListener(listView));
    }

    private void updateAdapter() {
        final LastPositionHelper lph = new LastPositionHelper(this);
        setAdapterCurrentCoordinates(false);
        adapter.notifyDataSetChanged();
        adapter.forceFilter();
        adapter.checkSpecialSortOrder();
        adapter.forceSort();
        updateGui();
        lph.setLastListPosition();
    }

    private void updateGui() {
        updateSortBar();
        updateTitle();
        refreshListFooter();
    }

    private void refreshListFooter() {
        refreshListFooter(false);
    }

    @SuppressLint("SetTextI18n")
    private void refreshListFooter(final boolean cachesAreLoading) {
        if (listFooter == null) {
            return;
        }

        if (cachesAreLoading) {
            setView(listFooterLine1, res.getString(R.string.caches_more_caches_loading), null);
            setViewGone(listFooterLine2);
            return;
        }

        final int unfilteredListSize = search == null ? adapter.getOriginalListCount() : search.getCount();
        final int totalAchievableListSize = search == null ? unfilteredListSize : Math.max(0, search.getTotalCount());
        final boolean moreToShow = unfilteredListSize > 0 && totalAchievableListSize > unfilteredListSize;

        if (moreToShow && type.isOnline) {
            setViewGone(listFooterLine2);
            setView(listFooterLine1, res.getString(R.string.caches_more_caches) + " (" + res.getString(R.string.caches_more_caches_currently) + ": " + unfilteredListSize + ")", v -> {
                showProgress(true);
                restartCacheLoader(true, null);
            });
        } else if (type.isOnline) {
            setViewGone(listFooterLine2);
            setView(listFooterLine1, res.getString(adapter.isEmpty() ? R.string.caches_no_cache : R.string.caches_more_caches_no), null);
        } else if (moreToShow && type.isStoredInDatabase) {
            final int missingCaches = totalAchievableListSize - unfilteredListSize;
            if (missingCaches > getOfflineListLimitIncrease()) {
                final String info = res.getQuantityString(R.plurals.caches_more_caches_next_x, getOfflineListLimitIncrease(), getOfflineListLimitIncrease());
                setView(listFooterLine1, info, v -> {
                    if (offlineListLoadLimit >= 0) {
                        offlineListLoadLimit += getOfflineListLimitIncrease();
                        refreshCurrentList();
                    }
                });
            } else {
                setViewGone(listFooterLine1);
            }
            setView(listFooterLine2, res.getString(R.string.caches_more_caches_remaining, missingCaches, totalAchievableListSize), v -> {
                offlineListLoadLimit = 0;
                refreshCurrentList();
            });
        } else {
            setViewGone(listFooterLine1);
            setViewGone(listFooterLine2);
        }
    }

    private void setViewGone(final View view) {
        view.setVisibility(View.GONE);
        view.setOnClickListener(null);
        view.setClickable(false);
    }

    private void setView(final TextView view, final String text, final View.OnClickListener clickListener) {
        view.setVisibility(View.VISIBLE);
        view.setText(text);
        view.setClickable(clickListener != null);
        view.setOnClickListener(clickListener);
    }

    private void setView(final TextView view, final String text, final View.OnClickListener clickListener, final View.OnLongClickListener longClickListener) {
        setView(view, text, clickListener);
    }

    private void updateSortBar() {
        final View sortView = this.findViewById(R.id.sort_bar);
        final GeocacheSort.SortType st = sortContext.getSort().getType();
        if (st == null || GeocacheSort.SortType.AUTO.equals(st) || CacheListType.HISTORY.equals(type)) {
            sortView.setVisibility(View.GONE);
        } else {
            sortView.setVisibility(View.VISIBLE);
            final TextView sortTextView = findViewById(R.id.sort_text);
            sortTextView.setText(sortContext.getSort().getDisplayName());
        }
    }

    private void importGpxSelectFiles() {
        contentStorageActivityHelper.selectMultipleFiles(null, PersistableFolder.GPX.getUri());
    }

    private void importGpx(final List<Uri> uris) {
        final GPXImporter importer = new GPXImporter(this, listId, importGpxAttachementFinishedHandler);
        for (Uri uri : uris) {
            importer.importGPX(uri, null, ContentStorage.get().getName(uri));
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (contentStorageActivityHelper.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if ((requestCode == REQUEST_CODE_IMPORT_PQ || requestCode == REQUEST_CODE_IMPORT_BOOKMARK) && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                final Uri uri = data.getData();
                new GPXImporter(this, listId, importGpxAttachementFinishedHandler).importGPX(uri, data.getType(), null);
            }
        } else if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            setAndRefreshFilterForOnlineSearch(data.getParcelableExtra(GeocacheFilterActivity.EXTRA_FILTER_CONTEXT));
        }
    }

    @Override
    public void refreshWithFilter(final GeocacheFilter filter) {
        currentCacheFilter.set(filter);
        setFilter();
        refreshFilterForOnlineSearch();

        refreshCurrentList();
    }

    private void setAndRefreshFilterForOnlineSearch(final GeocacheFilterContext filterContext) {
        currentCacheFilter = filterContext;
        setFilter();
        refreshFilterForOnlineSearch();
    }

    private void refreshFilterForOnlineSearch() {
        //not supported yet for all online searches
        if (type.isOnline && type != CacheListType.POCKET) {
            restartCacheLoader(false, null);
        }
    }

    private void refreshInBackground(final List<Geocache> caches) {
        if (type.isStoredInDatabase && caches.size() > REFRESH_WARNING_THRESHOLD) {
            SimpleDialog.of(this).setTitle(R.string.caches_refresh_all).setMessage(R.string.caches_refresh_all_warning).confirm(() ->
                    CacheDownloaderService.downloadCaches(this, Geocache.getGeocodes(caches), true, type.isStoredInDatabase, this::refreshCurrentList));
        } else {
            CacheDownloaderService.downloadCaches(this, Geocache.getGeocodes(caches), true, type.isStoredInDatabase, this::refreshCurrentList);
        }
    }

    private void appendInBackground(final Collection<Geocache> caches) {
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.appendToIndividualRoute(caches), () -> ActivityMixin.showShortToast(this, R.string.caches_appended_to_route));
        adapter.setSelectMode(false);
    }

    public void removeFromHistoryCheck() {
        final int message = (adapter != null && adapter.getCheckedCount() > 0) ? R.string.cache_remove_from_history
                : R.string.cache_clear_history;
        SimpleDialog.of(this).setTitle(R.string.caches_removing_from_history).setMessage(message).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(this::removeFromHistory);
    }

    private void removeFromHistory() {
        final List<Geocache> caches = adapter.getCheckedOrAllCaches();
        final Collection<String> geocodes = new ArrayList<>(caches.size());
        for (final Geocache cache : caches) {
            geocodes.add(cache.getGeocode());
        }
        DataStore.clearVisitDate(geocodes);
        DataStore.clearLogsOffline(caches);
        refreshCurrentList();
    }

    private void importWeb() {
        // menu is also shown with no device connected
        if (!Settings.isRegisteredForSend2cgeo()) {
            SimpleDialog.of(this).setTitle(R.string.web_import_title).setMessage(R.string.init_sendToCgeo_description).confirm(() -> SettingsActivity.openForScreen(R.string.preference_screen_sendtocgeo, CacheListActivity.this));
            return;
        }

        detailProgress.set(0);
        showProgress(false);
        final DownloadFromWebHandler downloadFromWebHandler = new DownloadFromWebHandler(this);
        progress.show(this, null, res.getString(R.string.web_import_waiting), true, downloadFromWebHandler.disposeMessage());
        Send2CgeoDownloader.loadFromWeb(downloadFromWebHandler, listId);
    }

    private void deleteCaches(@NonNull final Collection<Geocache> caches, final boolean removeFromAllLists) {
        new DeleteCachesFromListCommand(this, caches, listId, removeFromAllLists).execute();
    }

    private static final class LastPositionHelper {
        private final WeakReference<CacheListActivity> activityRef;
        private final int lastListPosition;
        private final int onTopSpace;

        LastPositionHelper(@NonNull final CacheListActivity context) {
            super();
            this.lastListPosition = context.getListView().getFirstVisiblePosition();
            this.activityRef = new WeakReference<>(context);

            final View firstChild = context.getListView().getChildAt(0);
            onTopSpace = (firstChild == null) ? 0 : (firstChild.getTop() - context.getListView().getPaddingTop());

        }

        public void refreshListAtLastPosition(final boolean triggerEmptyCheck) {
            final CacheListActivity activity = activityRef.get();
            if (activity != null) {
                activity.adapter.setSelectMode(false);
                if (activity.type.isStoredInDatabase) {
                    activity.refreshCurrentList(triggerEmptyCheck ? AfterLoadAction.CHECK_IF_EMPTY : AfterLoadAction.NO_ACTION);
                } else {
                    activity.replaceCacheListFromSearch();
                }
            }
            setLastListPosition();
        }

        public void setLastListPosition() {
            final CacheListActivity activity = activityRef.get();
            if (activity != null) {
                if (lastListPosition > 0 && lastListPosition < activity.adapter.getCount()) {
                    activity.getListView().setSelectionFromTop(lastListPosition, onTopSpace);
                }
            }
        }
    }

    public static boolean removeWillDeleteFromDevice(final int listId) {
        return listId == PseudoList.ALL_LIST.id || listId == PseudoList.HISTORY_LIST.id || listId == StoredList.TEMPORARY_LIST.id;
    }

    private static final class DeleteCachesFromListCommand extends AbstractCachesCommand {

        private final LastPositionHelper lastPositionHelper;
        private final int listId;
        private final Map<String, Set<Integer>> oldCachesLists = new HashMap<>();
        private final boolean removeFromAllLists;

        DeleteCachesFromListCommand(@NonNull final CacheListActivity context, final Collection<Geocache> caches, final int listId, final boolean removeFromAllLists) {
            super(context, caches, R.string.command_delete_caches_progress);
            this.lastPositionHelper = new LastPositionHelper(context);
            this.listId = listId;
            this.removeFromAllLists = removeFromAllLists;
        }

        @Override
        public void onFinished() {
            lastPositionHelper.refreshListAtLastPosition(true);
        }

        @Override
        protected void doCommand() {
            if (appliesToAllLists()) {
                oldCachesLists.putAll(DataStore.markDropped(getCaches()));
            } else {
                DataStore.removeFromList(getCaches(), listId);
            }
        }

        public boolean appliesToAllLists() {
            return removeFromAllLists || removeWillDeleteFromDevice(listId);
        }

        @Override
        protected void undoCommand() {
            if (appliesToAllLists()) {
                DataStore.addToLists(getCaches(), oldCachesLists);
            } else {
                DataStore.addToList(getCaches(), listId);
            }
        }

        @Override
        @NonNull
        protected String getResultMessage() {
            final int size = getCaches().size();
            return getContext().getResources().getQuantityString(R.plurals.command_delete_caches_result, size, size);
        }
    }

    private static void clearOfflineLogs(final Handler handler, final List<Geocache> selectedCaches) {
        Schedulers.io().scheduleDirect(() -> {
            DataStore.clearLogsOffline(selectedCaches);
            handler.sendEmptyMessage(DownloadProgress.MSG_DONE);
        });
    }

    private void restartCacheLoader(final boolean nextPage, final Consumer<Bundle> extrasModifier) {
        final Bundle extras = new Bundle();
        extras.putAll(typeParameters);
        if (extrasModifier != null) {
            extrasModifier.accept(extras);
        }
        extras.putBoolean(EXTRAS_NEXTPAGE, nextPage);
        LoaderManager.getInstance(CacheListActivity.this).restartLoader(CACHE_LOADER_ID, extras, CacheListActivity.this);
    }

    @NonNull
    private Action1<Integer> getListSwitchingRunnable() {
        return this::switchListById;
    }

    private void switchListById(final int id) {
        switchListById(id, AfterLoadAction.NO_ACTION);
    }

    private void switchListById(final int id, @NonNull final AfterLoadAction action) {
        if (id < 0) {
            return;
        }

        final CacheListType previousListType = type;

        if (id != listId) {
            //reset load limit
            offlineListLoadLimit = getOfflineListInitialLoadLimit();
        }

        if (id == PseudoList.HISTORY_LIST.id) {
            type = CacheListType.HISTORY;
            if (previousListType != type) {
                currentCacheFilter = new GeocacheFilterContext(type.filterContextType);
            }
            restartCacheLoader(false, e -> e.putSerializable(BUNDLE_ACTION_KEY, action));
        } else {
            if (id == PseudoList.ALL_LIST.id) {
                listId = id;
                title = res.getString(R.string.list_all_lists);
                markerId = EmojiUtils.NO_EMOJI;
                preventAskForDeletion = true;
            } else {
                final StoredList list = DataStore.getList(id);
                listId = list.id;
                title = list.title;
                markerId = list.markerId;
                preventAskForDeletion = list.preventAskForDeletion;
            }
            type = CacheListType.OFFLINE;

            if (previousListType != type) {
                currentCacheFilter = new GeocacheFilterContext(type.filterContextType);
            }
            restartCacheLoader(false, e -> {
                e.putSerializable(BUNDLE_ACTION_KEY, action);
                e.putAll(OfflineGeocacheListLoader.getBundleForList(listId));
            });

            Settings.setLastDisplayedList(listId);
        }

        sortContext = GeocacheSortContext.getFor(type, "" + listId);

        initAdapter();
        setFilter();
        showProgress(true);
        refreshListFooter();
        adapter.setSelectMode(false);
        invalidateOptionsMenuCompatible();
    }

    private void renameList() {
        (new RenameListCommand(this, listId) {

            @Override
            protected void onFinished() {
                refreshCurrentList();
            }
        }).execute();
    }

    private void removeListInternal() {
        new DeleteListCommand(this, listId) {

            private String oldListName;

            @Override
            protected boolean canExecute() {
                oldListName = DataStore.getList(listId).getTitle();
                return super.canExecute();
            }

            @Override
            protected void onFinished() {
                refreshActionBarTitle();
                switchListById(StoredList.STANDARD_LIST_ID);
            }

            @Override
            protected void onFinishedUndo() {
                refreshActionBarTitle();
                for (final StoredList list : DataStore.getLists()) {
                    if (oldListName.equals(list.getTitle())) {
                        switchListById(list.id);
                    }
                }
            }

        }.execute();
    }

    private void removeList() {
        // if there are no caches on this list, don't bother the user with questions.
        // there is no harm in deleting the list, he could recreate it easily
        if (adapter.isEmpty()) {
            removeListInternal();
            return;
        }

        // ask him, if there are caches on the list
        SimpleDialog.of(this).setTitle(R.string.list_dialog_remove_title).setMessage(R.string.list_dialog_remove_description)
                .setPositiveButton(TextParam.id(R.string.list_dialog_remove)).confirm(this::removeListInternal);
    }

    public void goMap() {
        if (!cacheToShow()) {
            return;
        }

        // apply filter settings (if there's a filter)
        final SearchResult searchToUse = getFilteredSearch();
        if (Settings.useLegacyMaps() || listId == 0) {
            DefaultMap.startActivitySearch(this, searchToUse, title, listId);
        } else {
            DefaultMap.startActivityList(this, listId, currentCacheFilter);
        }
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void refreshCurrentList() {
        refreshCurrentList(AfterLoadAction.NO_ACTION);
    }

    private void refreshCurrentList(@NonNull final AfterLoadAction action) {
        // do not refresh any of the dynamic search result lists but history, which might have been cleared
        if (!type.isStoredInDatabase) {
            return;
        }

        final LastPositionHelper lph = new LastPositionHelper(this);
        refreshActionBarTitle();
        switchListById(listId, action);
        lph.setLastListPosition();
    }

    public static Intent getActivityOfflineIntent(final Context context) {
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, CacheListType.OFFLINE);
        return cachesIntent;
    }

    public static void startActivityOffline(final Context context) {
        context.startActivity(getActivityOfflineIntent(context));
    }

    public static void startActivityOwner(final Context context, final String userName) {
        if (!checkNonBlankUsername(context, userName)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, CacheListType.OWNER);
        cachesIntent.putExtra(Intents.EXTRA_USERNAME, userName);
        context.startActivity(cachesIntent);
    }

    public static void startActivityFilter(final Context context) {
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, CacheListType.SEARCH_FILTER);
        context.startActivity(cachesIntent);
    }

    /**
     * Check if a given username is valid (non blank), and show a toast if it isn't.
     *
     * @param context  an activity
     * @param username the username to check
     * @return <tt>true</tt> if the username is not blank, <tt>false</tt> otherwise
     */
    private static boolean checkNonBlankUsername(final Context context, final String username) {
        if (StringUtils.isBlank(username)) {
            ActivityMixin.showToast(context, R.string.warn_no_username);
            return false;
        }
        return true;
    }

    public static void startActivityFinder(final Context context, final String userName) {
        if (!checkNonBlankUsername(context, userName)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, CacheListType.FINDER);
        cachesIntent.putExtra(Intents.EXTRA_USERNAME, userName);
        context.startActivity(cachesIntent);
    }

    private void updateFilterBar() {
        FilterUtils.updateFilterBar(this, getActiveFilterName(), getActiveFilterSavedDifferently());
    }

    @Nullable
    private String getActiveFilterName() {
        if (currentCacheFilter.get().isFiltering()) {
            return currentCacheFilter.get().toUserDisplayableString();
        }
        return null;
    }

    @Nullable
    private Boolean getActiveFilterSavedDifferently() {
        if (currentCacheFilter.get().isFiltering()) {
            return currentCacheFilter.get().isSavedDifferently();
        }
        return null;
    }

    public static Intent getNearestIntent(final Activity context) {
        return Intents.putListType(new Intent(context, CacheListActivity.class), CacheListType.NEAREST);
    }

    public static Intent getHistoryIntent(final Context context) {
        return Intents.putListType(new Intent(context, CacheListActivity.class), CacheListType.HISTORY);
    }

    public static void startActivityAddress(final Context context, final Geopoint coords, final String address) {
        final Intent addressIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(addressIntent, CacheListType.ADDRESS);
        addressIntent.putExtra(Intents.EXTRA_COORDS, coords);
        addressIntent.putExtra(Intents.EXTRA_ADDRESS, address);
        context.startActivity(addressIntent);
    }

    /**
     * start list activity, by searching around the given point.
     *
     * @param name name of coordinates, will lead to a title like "Around ..." instead of directly showing the
     *             coordinates as title
     */
    public static void startActivityCoordinates(final AbstractActivity context, final Geopoint coords, @Nullable final String name) {
        if (!isValidCoords(context, coords)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, CacheListType.COORDINATE);
        cachesIntent.putExtra(Intents.EXTRA_COORDS, coords);
        if (StringUtils.isNotEmpty(name)) {
            cachesIntent.putExtra(Intents.EXTRA_TITLE, context.getString(R.string.around, name));
        }
        context.startActivity(cachesIntent);
    }

    private static boolean isValidCoords(final AbstractActivity context, final Geopoint coords) {
        if (coords == null) {
            context.showToast(CgeoApplication.getInstance().getString(R.string.warn_no_coordinates));
            return false;
        }
        return true;
    }

    public static void startActivityKeyword(final AbstractActivity context, final String keyword) {
        if (keyword == null) {
            context.showToast(CgeoApplication.getInstance().getString(R.string.warn_no_keyword));
            return;
        }
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, CacheListType.KEYWORD);
        cachesIntent.putExtra(Intents.EXTRA_KEYWORD, keyword);
        context.startActivity(cachesIntent);
    }

    public static void startActivityMap(final Context context, final SearchResult search) {
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        cachesIntent.putExtra(Intents.EXTRA_SEARCH, search);
        Intents.putListType(cachesIntent, CacheListType.MAP);
        context.startActivity(cachesIntent);
    }

    public static void startActivityLastViewed(final Context context, final SearchResult search) {
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        cachesIntent.putExtra(Intents.EXTRA_SEARCH, search);
        Intents.putListType(cachesIntent, CacheListType.LAST_VIEWED);
        context.startActivity(cachesIntent);
    }

    public static void startActivityPocketDownload(@NonNull final Context context, @NonNull final GCList pocketQuery) {
        final String guid = pocketQuery.getGuid();
        if (guid == null) {
            ActivityMixin.showToast(context, CgeoApplication.getInstance().getString(R.string.warn_pocket_query_select));
            return;
        }
        startActivityWithAttachment(context, pocketQuery);
    }

    public static void startActivityPocket(@NonNull final Context context, @NonNull final GCList pocketQuery) {
        final String guid = pocketQuery.getGuid();
        if (guid == null) {
            ActivityMixin.showToast(context, CgeoApplication.getInstance().getString(R.string.warn_pocket_query_select));
            return;
        }
        startActivityPocket(context, pocketQuery, CacheListType.POCKET);
    }

    private static void startActivityWithAttachment(@NonNull final Context context, @NonNull final GCList pocketQuery) {
        final Uri uri = pocketQuery.getUri();
        final Intent cachesIntent = new Intent(Intent.ACTION_VIEW, uri, context, CacheListActivity.class);
        cachesIntent.setDataAndType(uri, pocketQuery.isBookmarkList() ? "application/xml" : "application/zip");
        cachesIntent.putExtra(Intents.EXTRA_NAME, pocketQuery.getName());
        context.startActivity(cachesIntent);
    }

    private static void startActivityPocket(@NonNull final Context context, @NonNull final GCList pocketQuery, final CacheListType cacheListType) {
        final Intent cachesIntent = new Intent(context, CacheListActivity.class);
        Intents.putListType(cachesIntent, cacheListType);
        cachesIntent.putExtra(Intents.EXTRA_NAME, pocketQuery.getName());
        cachesIntent.putExtra(Intents.EXTRA_POCKET_GUID, pocketQuery.getShortGuid());
        cachesIntent.putExtra(Intents.EXTRA_POCKET_HASH, pocketQuery.getPqHash());
        context.startActivity(cachesIntent);
    }

    // Loaders

    @NonNull
    @Override
    public Loader<SearchResult> onCreateLoader(final int type, final Bundle extras) {
        if (type != CACHE_LOADER_ID || extras == null) {
            throw new IllegalArgumentException("invalid loader type " + type + " or empty extras: " + extras);
        }
        final boolean isNextPage = extras.getBoolean(EXTRAS_NEXTPAGE, false);
        AbstractSearchLoader loader = null;
        preventAskForDeletion = true;

        if (isNextPage) {
            loader = new NextPageGeocacheListLoader(this, search);
        } else {
            switch (this.type) {
                case OFFLINE:
                    // open either the requested or the last list
                    if (extras.containsKey(Intents.EXTRA_LIST_ID)) {
                        listId = extras.getInt(Intents.EXTRA_LIST_ID);
                    } else {
                        listId = Settings.getLastDisplayedList();
                    }
                    if (listId == PseudoList.ALL_LIST.id) {
                        title = res.getString(R.string.list_all_lists);
                        markerId = EmojiUtils.NO_EMOJI;
                    } else if (listId <= StoredList.TEMPORARY_LIST.id) {
                        listId = StoredList.STANDARD_LIST_ID;
                        title = res.getString(R.string.stored_caches_button);
                        markerId = EmojiUtils.NO_EMOJI;
                    } else {
                        final StoredList list = DataStore.getList(listId);
                        // list.id may be different if listId was not valid
                        if (list.id != listId) {
                            showToast(getString(R.string.list_not_available));
                        }
                        listId = list.id;
                        title = list.title;
                        markerId = list.markerId;
                        preventAskForDeletion = list.preventAskForDeletion;
                    }

                    loader = new OfflineGeocacheListLoader(this, coords, listId, currentCacheFilter.get(), sortContext.getSort().getComparator(), false, offlineListLoadLimit);

                    break;
                case HISTORY:
                    title = res.getString(R.string.caches_history);
                    listId = PseudoList.HISTORY_LIST.id;
                    markerId = EmojiUtils.NO_EMOJI;
                    loader = new OfflineGeocacheListLoader(this, coords, PseudoList.HISTORY_LIST.id, currentCacheFilter.get(), VisitComparator.singleton, sortContext.getSort().isInverse(), offlineListLoadLimit);
                    break;
                case NEAREST:
                    title = res.getString(R.string.caches_nearby);
                    markerId = EmojiUtils.NO_EMOJI;
                    loader = new CoordsGeocacheListLoader(this, sortContext.getSort(), coords, true);
                    break;
                case COORDINATE:
                    title = coords.toString();
                    markerId = EmojiUtils.NO_EMOJI;
                    loader = new CoordsGeocacheListLoader(this, sortContext.getSort(), coords, false);
                    break;
                case KEYWORD:
                    final String keyword = extras.getString(Intents.EXTRA_KEYWORD);
                    markerId = EmojiUtils.NO_EMOJI;
                    title = listNameMemento.rememberTerm(keyword);
                    if (keyword != null) {
                        loader = new KeywordGeocacheListLoader(this, sortContext.getSort(), keyword);
                    }
                    break;
                case ADDRESS:
                    final String address = extras.getString(Intents.EXTRA_ADDRESS);
                    if (StringUtils.isNotBlank(address)) {
                        title = listNameMemento.rememberTerm(address);
                    } else {
                        title = coords.toString();
                    }
                    markerId = EmojiUtils.NO_EMOJI;
                    loader = new CoordsGeocacheListLoader(this, sortContext.getSort(), coords, false);
                    break;
                case FINDER:
                    final String username = extras.getString(Intents.EXTRA_USERNAME);
                    title = listNameMemento.rememberTerm(username);
                    markerId = EmojiUtils.NO_EMOJI;
                    if (username != null) {
                        loader = new FinderGeocacheListLoader(this, sortContext.getSort(), username);
                    }
                    break;
                case SEARCH_FILTER:
                    markerId = EmojiUtils.NO_EMOJI;
                    loader = new SearchFilterGeocacheListLoader(this, currentCacheFilter.get(), sortContext.getSort());
                    break;
                case OWNER:
                    final String ownerName = extras.getString(Intents.EXTRA_USERNAME);
                    title = listNameMemento.rememberTerm(ownerName);
                    markerId = EmojiUtils.NO_EMOJI;
                    if (ownerName != null) {
                        loader = new OwnerGeocacheListLoader(this, sortContext.getSort(), ownerName);
                    }
                    break;
                case MAP:
                    title = res.getString(R.string.map_map);
                    markerId = EmojiUtils.NO_EMOJI;
                    search = (SearchResult) extras.get(Intents.EXTRA_SEARCH);
                    replaceCacheListFromSearch();
                    //loadCachesHandler.sendMessage(Message.obtain());
                    loader = new NullGeocacheListLoader(this, search);
                    break;
                case LAST_VIEWED:
                    title = res.getString(R.string.cache_recently_viewed);
                    markerId = EmojiUtils.NO_EMOJI;
                    search = (SearchResult) extras.get(Intents.EXTRA_SEARCH);
                    replaceCacheListFromSearch();
                    //loadCachesHandler.sendMessage(Message.obtain());
                    loader = new NullGeocacheListLoader(this, search);
                    break;
                case POCKET:
                    final String shortGuid = extras.getString(Intents.EXTRA_POCKET_GUID);
                    final String pqHash = extras.getString(Intents.EXTRA_POCKET_HASH);
                    title = listNameMemento.rememberTerm(extras.getString(Intents.EXTRA_NAME));
                    markerId = EmojiUtils.NO_EMOJI;
                    loader = new PocketGeocacheListLoader(this, shortGuid, pqHash);
                    break;
                default:
                    //can never happen, makes Codacy happy
                    break;
            }
        }
        // if there is a title given in the activity start request, use this one instead of the default
        if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_TITLE))) {
            title = extras.getString(Intents.EXTRA_TITLE);
        }
        if (loader != null && extras.getSerializable(BUNDLE_ACTION_KEY) != null) {
            final AfterLoadAction action = (AfterLoadAction) extras.getSerializable(BUNDLE_ACTION_KEY);
            loader.setAfterLoadAction(action);
        }
        if (loader != null) {
            if (loader instanceof LiveFilterGeocacheListLoader) {
                currentAddFilterCriteria = ((LiveFilterGeocacheListLoader) loader).getAdditionalFilterParameter();
            } else {
                currentAddFilterCriteria = null;
            }
        }
        updateTitle();
        showProgress(true);
        refreshListFooter();

        if (loader == null) {
            Log.w("LOADER IS NULL!!!");
        }

        currentLoader = loader;
        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<SearchResult> arg0, final SearchResult searchIn) {
        // The database search was moved into the UI call intentionally. If this is done before the runOnUIThread,
        // then we have 2 sets of caches in memory. This can lead to OOM for huge cache lists.
        if (searchIn != null) {
            final Set<Geocache> cachesFromSearchResult = searchIn.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            final LastPositionHelper lph = new LastPositionHelper(this);
            adapter.setList(cachesFromSearchResult);
            search = searchIn;
            updateGui();
            lph.setLastListPosition();

            if (search.getError() != StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.err_download_fail) + ' ' + search.getError().getErrorString() + '.');
            }
        }
        showProgress(false);
        invalidateOptionsMenuCompatible();
        if (arg0 instanceof AbstractSearchLoader) {
            switch (((AbstractSearchLoader) arg0).getAfterLoadAction()) {
                case CHECK_IF_EMPTY:
                    checkIfEmptyAndRemoveAfterConfirm();
                    break;
                case NO_ACTION:
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<SearchResult> arg0) {
        //Not interesting
    }

    /**
     * Calculate the subtitle of the current list depending on (optional) filters.
     */
    private CharSequence getCurrentSubtitle() {
        if (search == null) {
            return getCacheNumberString(getResources(), 0);
        }

        final int totalCount = type.isStoredInDatabase ? search.getTotalCount() : search.getCount();

        final StringBuilder result = new StringBuilder();
        final boolean isFiltered = adapter.hasActiveFilter() && adapter.getCount() != totalCount;
        if (isFiltered || resultIsOfflineAndLimited()) {
            result.append(adapter.getCount());
            if (resultIsOfflineAndLimited()) {
                result.append("+");
            }
            result.append('/');
        }
        result.append(getCacheNumberString(getResources(), totalCount));

        return result.toString();
    }

    private boolean resultIsOfflineAndLimited() {
        return type.isStoredInDatabase && offlineListLoadLimit > 0 && search != null && search.getTotalCount() > offlineListLoadLimit && search.getCount() == offlineListLoadLimit;
    }

    /**
     * Used to indicate if an action should be taken after the AbstractSearchLoader has finished
     */
    public enum AfterLoadAction {
        /**
         * Take no action
         */
        NO_ACTION,
        /**
         * Check if the list is empty and prompt for deletion
         */
        CHECK_IF_EMPTY
    }

    public static int getOfflineListInitialLoadLimit() {
        return Settings.getListInitialLoadLimit();
    }

    public static int getOfflineListLimitIncrease() {
        return 100;
    }

    private void showProgress(final boolean loading) {
        final View progressBar = findViewById(R.id.loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        final ListView list = getListView();
        list.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private boolean openSortDialog() {
        final List<Pair<GeocacheSort.SortType, String>> availableTypes = sortContext.getSort().getAvailableTypes();
        final List<GeocacheSort.SortType> typeList = new ArrayList<>();
        final HashMap<GeocacheSort.SortType, String> typeToString = new HashMap<>();
        for (Pair<GeocacheSort.SortType, String> entry : availableTypes) {
            typeToString.put(entry.first, entry.second);
            typeList.add(entry.first);
        }

        final SimpleDialog.ItemSelectModel<GeocacheSort.SortType> model = new SimpleDialog.ItemSelectModel<>();
        model
                .setScrollAnchor(sortContext.getSort().getType())
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO)
                .setItems(typeList)
                .setSelectedItems(Collections.singleton(sortContext.getSort().getType()))
                .setDisplayMapper((f) -> TextParam.text(typeToString.get(f)));

        SimpleDialog.of(this).setTitle(R.string.caches_sort)
                .selectSingle(model, this::refreshWithSortType);

        return true;
    }

    private boolean refreshWithSortType(final GeocacheSort.SortType sortType) {
        sortContext.getSort().setAndToggle(sortType);
        sortContext.save();
        adapter.forceSort();
        updateSortBar();
        refreshCurrentList();
        //for online searches, restart search with new sort argument
        if (type.isOnline && type != CacheListType.POCKET) {
            restartCacheLoader(false, null);
        }

        return true;
    }
}
