package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cachelist.CacheListAppFactory;
import cgeo.geocaching.connector.gc.SearchHandler;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.export.ExportFactory;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.filter.FilterUserInterface;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.loaders.AbstractSearchLoader;
import cgeo.geocaching.loaders.AbstractSearchLoader.CacheListLoaderType;
import cgeo.geocaching.loaders.AddressGeocacheListLoader;
import cgeo.geocaching.loaders.CoordsGeocacheListLoader;
import cgeo.geocaching.loaders.HistoryGeocacheListLoader;
import cgeo.geocaching.loaders.KeywordGeocacheListLoader;
import cgeo.geocaching.loaders.OfflineGeocacheListLoader;
import cgeo.geocaching.loaders.OwnerGeocacheListLoader;
import cgeo.geocaching.loaders.RemoveFromHistoryLoader;
import cgeo.geocaching.loaders.UsernameGeocacheListLoader;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.ComparatorUserInterface;
import cgeo.geocaching.sorting.EventDateComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.ui.CacheListAdapter;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.utils.DateUtils;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class cgeocaches extends AbstractListActivity implements FilteredActivity, LoaderManager.LoaderCallbacks<SearchResult> {

    private static final int MAX_LIST_ITEMS = 1000;
    private static final int MENU_REFRESH_STORED = 2;
    private static final int MENU_CACHE_DETAILS = 4;
    private static final int MENU_DROP_CACHES = 5;
    private static final int MENU_IMPORT_GPX = 6;
    private static final int MENU_CREATE_LIST = 7;
    private static final int MENU_DROP_LIST = 8;
    private static final int MENU_INVERT_SELECTION = 9;
    private static final int MENU_SWITCH_LIST = 17;
    private static final int MENU_IMPORT_WEB = 21;
    private static final int MENU_EXPORT = 22;
    private static final int MENU_REMOVE_FROM_HISTORY = 23;
    private static final int MENU_DROP_CACHE = 24;
    private static final int MENU_MOVE_TO_LIST = 25;
    private static final int MENU_SWITCH_SELECT_MODE = 52;
    private static final int SUBMENU_SHOW_MAP = 54;
    private static final int SUBMENU_MANAGE_LISTS = 55;
    private static final int SUBMENU_MANAGE_OFFLINE = 56;
    private static final int MENU_SORT = 57;
    private static final int SUBMENU_MANAGE_HISTORY = 60;
    private static final int MENU_RENAME_LIST = 64;
    private static final int MENU_DROP_CACHES_AND_LIST = 65;
    private static final int MENU_DEFAULT_NAVIGATION = 66;
    private static final int MENU_NAVIGATION = 69;
    private static final int MENU_STORE_CACHE = 73;
    private static final int MENU_FILTER = 74;
    private static final int MENU_NEGATE_FILTER = 77;
    private static final int MENU_DELETE_EVENTS = 75;
    private static final int MENU_CLEAR_OFFLINE_LOGS = 76;

    private static final int MSG_DONE = -1;
    private static final int MSG_RESTART_GEO_AND_DIR = -2;
    private static final int MSG_CANCEL = -99;

    private CacheListType type = null;
    private Geopoint coords = null;
    private SearchResult search = null;
    /** The list of shown caches shared with Adapter. Don't manipulate outside of main thread only with Handler */
    private final List<Geocache> cacheList = new ArrayList<Geocache>();
    private CacheListAdapter adapter = null;
    private LayoutInflater inflater = null;
    private View listFooter = null;
    private TextView listFooterText = null;
    private final Progress progress = new Progress();
    private String title = "";
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    private LoadDetailsThread threadDetails = null;
    private LoadFromWebThread threadWeb = null;
    private int listId = StoredList.TEMPORARY_LIST_ID; // Only meaningful for the OFFLINE type
    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {

        @Override
        public void updateGeoData(final IGeoData geo) {
            if (geo.getCoords() != null) {
                adapter.setActualCoordinates(geo.getCoords());
            }
            if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                adapter.setActualHeading(geo.getBearing());
            }
        }

        @Override
        public void updateDirection(final float direction) {
            if (!Settings.isLiveList()) {
                return;
            }

            if (app.currentGeo().getSpeed() <= 5) { // use compass when speed is lower than 18 km/h) {
                final float northHeading = DirectionProvider.getDirectionNow(cgeocaches.this, direction);
                adapter.setActualHeading(northHeading);
            }
        }

    };
    private ContextMenuInfo lastMenuInfo;
    private String contextMenuGeocode = "";
    /**
     * the navigation menu item for the cache list (not the context menu!), or <code>null</code>
     */
    private MenuItem navigationMenu;

    public void handleCachesLoaded() {
        try {
            setAdapter();

            updateTitle();

            setDateComparatorForEventList();

            showFooterMoreCaches();

            if (search != null && search.getError() == StatusCode.UNAPPROVED_LICENSE) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(res.getString(R.string.license));
                dialog.setMessage(res.getString(R.string.err_license));
                dialog.setCancelable(true);
                dialog.setNegativeButton(res.getString(R.string.license_dismiss), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Cookies.clearCookies();
                        dialog.cancel();
                    }
                });
                dialog.setPositiveButton(res.getString(R.string.license_show), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Cookies.clearCookies();
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/software/agreement.aspx?ID=0")));
                    }
                });

                AlertDialog alert = dialog.create();
                alert.show();
            } else if (search != null && search.getError() != null) {
                showToast(res.getString(R.string.err_download_fail) + ' ' + search.getError().getErrorString(res) + '.');

                hideLoading();
                showProgress(false);

                finish();
                return;
            }

            setAdapterCurrentCoordinates(false);
        } catch (Exception e) {
            showToast(res.getString(R.string.err_detail_cache_find_any));
            Log.e("cgeocaches.loadCachesHandler", e);

            hideLoading();
            showProgress(false);

            finish();
            return;
        }

        try {
            hideLoading();
            showProgress(false);
        } catch (Exception e2) {
            Log.e("cgeocaches.loadCachesHandler.2", e2);
        }

        adapter.setSelectMode(false);
    }

    private Handler loadCachesHandler = new LoadCachesHandler(this);

    private static class LoadCachesHandler extends WeakReferenceHandler<cgeocaches> {

        protected LoadCachesHandler(cgeocaches activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final cgeocaches activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.handleCachesLoaded();
        }
    }

    /**
     * Loads the caches and fills the {@link #cacheList} according to {@link #search} content.
     *
     * If {@link #search} is <code>null</code>, this does nothing.
     */

    private void replaceCacheListFromSearch() {
        if (search != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cacheList.clear();

                    // The database search was moved into the UI call intentionally. If this is done before the runOnUIThread,
                    // then we have 2 sets of caches in memory. This can lead to OOM for huge cache lists.
                    final Set<Geocache> cachesFromSearchResult = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);

                    cacheList.addAll(cachesFromSearchResult);
                    adapter.reFilter();
                    updateTitle();
                    showFooterMoreCaches();
                }
            });
        }
    }

    protected void updateTitle() {
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        if (adapter.isFiltered()) {
            numbers.add(adapter.getCount());
        }
        if (search != null) {
            numbers.add(search.getCount());
        }
        if (numbers.isEmpty()) {
            setTitle(title);
        }
        else {
            setTitle(title + " [" + StringUtils.join(numbers, '/') + ']');
        }
    }

    private Handler loadDetailsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setAdapter();

            if (msg.what > -1) {
                cacheList.get(msg.what).setStatusChecked(false);

                adapter.notifyDataSetChanged();

                int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                int minutesRemaining = ((detailTotal - detailProgress) * secondsElapsed / ((detailProgress > 0) ? detailProgress : 1) / 60);

                progress.setProgress(detailProgress);
                if (minutesRemaining < 1) {
                    progress.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                } else {
                    progress.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getQuantityString(R.plurals.caches_eta_mins, minutesRemaining));
                }
            } else if (msg.what == MSG_CANCEL) {
                if (threadDetails != null) {
                    threadDetails.kill();
                }
            } else if (msg.what == MSG_RESTART_GEO_AND_DIR) {
                startGeoAndDir();
            } else {
                if (search != null) {
                    final Set<Geocache> cacheListTmp = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    if (CollectionUtils.isNotEmpty(cacheListTmp)) {
                        cacheList.clear();
                        cacheList.addAll(cacheListTmp);
                    }
                }

                setAdapterCurrentCoordinates(false);

                showProgress(false);
                progress.dismiss();

                startGeoAndDir();
            }
        }
    };

    /**
     * TODO Possibly parts should be a Thread not a Handler
     */
    private Handler downloadFromWebHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            setAdapter();

            adapter.notifyDataSetChanged();

            if (msg.what == 0) { //no caches
                progress.setMessage(res.getString(R.string.web_import_waiting));
            } else if (msg.what == 1) { //cache downloading
                progress.setMessage(res.getString(R.string.web_downloading) + " " + msg.obj + '…');
            } else if (msg.what == 2) { //Cache downloaded
                progress.setMessage(res.getString(R.string.web_downloaded) + " " + msg.obj + '…');
                refreshCurrentList();
            } else if (msg.what == -2) {
                progress.dismiss();
                showToast(res.getString(R.string.sendToCgeo_download_fail));
                finish();
            } else if (msg.what == -3) {
                progress.dismiss();
                showToast(res.getString(R.string.sendToCgeo_no_registration));
                finish();
            } else if (msg.what == MSG_CANCEL) {
                if (threadWeb != null) {
                    threadWeb.kill();
                }
            } else {
                adapter.setSelectMode(false);

                replaceCacheListFromSearch();

                progress.dismiss();
            }
        }
    };
    private Handler dropDetailsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_CANCEL) {
                adapter.setSelectMode(false);

                refreshCurrentList();

                replaceCacheListFromSearch();

                progress.dismiss();
            }
        }
    };
    private Handler clearOfflineLogsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_CANCEL) {
                adapter.setSelectMode(false);

                refreshCurrentList();

                replaceCacheListFromSearch();

                progress.dismiss();
            }
        }
    };

    private Handler importGpxAttachementFinishedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            refreshCurrentList();
        }
    };
    private AbstractSearchLoader currentLoader;

    public cgeocaches() {
        super(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.caches);

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Object typeObject = extras.get(Intents.EXTRA_LIST_TYPE);
            type = (typeObject instanceof CacheListType) ? (CacheListType) typeObject : CacheListType.OFFLINE;
            coords = (Geopoint) extras.getParcelable(Intents.EXTRAS_COORDS);
        }
        else {
            extras = new Bundle();
        }
        if (isInvokedFromAttachment()) {
            type = CacheListType.OFFLINE;
            if (coords == null) {
                coords = new Geopoint(0.0, 0.0);
            }
        }

        currentLoader = (AbstractSearchLoader) getSupportLoaderManager().initLoader(type.ordinal(), extras, this);

        // init
        if (CollectionUtils.isNotEmpty(cacheList)) {
            if (currentLoader.isStarted()) {
                showFooterLoadingCaches();
            }
            else {
                showFooterMoreCaches();
            }
        }

        setTitle(title);
        setAdapter();

        prepareFilterBar();

        if (isInvokedFromAttachment()) {
            importGpxAttachement();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (currentLoader.isLoading()) {
            showFooterLoadingCaches();
        }
    }

    private boolean isConcreteList() {
        return type == CacheListType.OFFLINE &&
                (listId == StoredList.STANDARD_LIST_ID || listId >= cgData.customListIdOffset);
    }

    private boolean isInvokedFromAttachment() {
        return Intent.ACTION_VIEW.equals(getIntent().getAction());
    }

    private void importGpxAttachement() {
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.gpx_import_title))
                .setMessage(res.getString(R.string.gpx_import_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new GPXImporter(cgeocaches.this, listId, importGpxAttachementFinishedHandler).importGPX();
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        startGeoAndDir();

        adapter.setSelectMode(false);
        setAdapterCurrentCoordinates(true);

        if (loadCachesHandler != null && search != null) {
            replaceCacheListFromSearch();
            loadCachesHandler.sendEmptyMessage(0);
        }

        // refresh standard list if it has changed (new caches downloaded)
        if (type == CacheListType.OFFLINE && listId >= StoredList.STANDARD_LIST_ID && search != null) {
            SearchResult newSearch = cgData.getBatchOfStoredCaches(coords, Settings.getCacheType(), listId);
            if (newSearch != null && newSearch.getTotal() != search.getTotal()) {
                refreshCurrentList();
            }
        }
    }

    private void setAdapterCurrentCoordinates(final boolean forceSort) {
        final Geopoint coordsNow = app.currentGeo().getCoords();
        if (coordsNow != null) {
            adapter.setActualCoordinates(coordsNow);
            if (forceSort) {
                adapter.forceSort();
            }
        }
    }

    @Override
    public void onPause() {
        removeGeoAndDir();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FILTER, 0, res.getString(R.string.caches_filter)).setIcon(R.drawable.ic_menu_filter);
        menu.add(0, MENU_NEGATE_FILTER, 0, "Negate Filters").setIcon(R.drawable.ic_menu_filter);

        if (type != CacheListType.HISTORY) {
            menu.add(0, MENU_SORT, 0, res.getString(R.string.caches_sort)).setIcon(R.drawable.ic_menu_sort_alphabetically);
        }

        menu.add(0, MENU_SWITCH_SELECT_MODE, 0, res.getString(R.string.caches_select_mode)).setIcon(R.drawable.ic_menu_agenda);
        menu.add(0, MENU_INVERT_SELECTION, 0, res.getString(R.string.caches_select_invert)).setIcon(R.drawable.ic_menu_mark);
        if (type == CacheListType.OFFLINE) {
            SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_OFFLINE, 0, res.getString(R.string.caches_manage)).setIcon(R.drawable.ic_menu_save);
            subMenu.add(0, MENU_DROP_CACHES, 0, res.getString(R.string.caches_drop_all)); // delete saved caches
            subMenu.add(0, MENU_DROP_CACHES_AND_LIST, 0, res.getString(R.string.caches_drop_all_and_list));
            subMenu.add(0, MENU_REFRESH_STORED, 0, res.getString(R.string.cache_offline_refresh)); // download details for all caches
            subMenu.add(0, MENU_MOVE_TO_LIST, 0, res.getString(R.string.cache_menu_move_list));
            subMenu.add(0, MENU_DELETE_EVENTS, 0, res.getString(R.string.caches_delete_events));
            subMenu.add(0, MENU_CLEAR_OFFLINE_LOGS, 0, res.getString(R.string.caches_clear_offlinelogs));

            //TODO: add submenu/AlertDialog and use R.string.gpx_import_title
            subMenu.add(0, MENU_IMPORT_GPX, 0, res.getString(R.string.gpx_import_title));
            if (Settings.getWebDeviceCode() != null) {
                subMenu.add(0, MENU_IMPORT_WEB, 0, res.getString(R.string.web_import_title));
            }

            subMenu.add(0, MENU_EXPORT, 0, res.getString(R.string.export)); // export caches
        } else {
            if (type == CacheListType.HISTORY) {
                SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_HISTORY, 0, res.getString(R.string.caches_manage)).setIcon(R.drawable.ic_menu_save);
                subMenu.add(0, MENU_REMOVE_FROM_HISTORY, 0, res.getString(R.string.cache_clear_history)); // remove from history
                subMenu.add(0, MENU_EXPORT, 0, res.getString(R.string.export)); // export caches
            }
            menu.add(0, MENU_REFRESH_STORED, 0, res.getString(R.string.caches_store_offline)).setIcon(R.drawable.ic_menu_set_as); // download details for all caches
        }

        navigationMenu = CacheListAppFactory.addMenuItems(menu, this, res);

        if (type == CacheListType.OFFLINE) {
            SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_LISTS, 0, res.getString(R.string.list_menu)).setIcon(R.drawable.ic_menu_more);
            subMenu.add(0, MENU_CREATE_LIST, 0, res.getString(R.string.list_menu_create));
            subMenu.add(0, MENU_DROP_LIST, 0, res.getString(R.string.list_menu_drop));
            subMenu.add(0, MENU_RENAME_LIST, 0, res.getString(R.string.list_menu_rename));
            subMenu.add(0, MENU_SWITCH_LIST, 0, res.getString(R.string.list_menu_change));
        }

        return true;
    }

    private static void setVisible(final Menu menu, final int itemId, final boolean visible) {
        final MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            if (adapter.isSelectMode()) {
                menu.findItem(MENU_SWITCH_SELECT_MODE).setTitle(res.getString(R.string.caches_select_mode_exit))
                        .setIcon(R.drawable.ic_menu_clear_playlist);
                menu.findItem(MENU_INVERT_SELECTION).setVisible(true);
            } else {
                menu.findItem(MENU_SWITCH_SELECT_MODE).setTitle(res.getString(R.string.caches_select_mode))
                        .setIcon(R.drawable.ic_menu_agenda);
                menu.findItem(MENU_INVERT_SELECTION).setVisible(false);
            }

            final boolean isEmpty = cacheList.isEmpty();
            final boolean isConcrete = isConcreteList();

            setVisible(menu, MENU_SWITCH_SELECT_MODE, !isEmpty);
            setVisible(menu, SUBMENU_MANAGE_HISTORY, !isEmpty);
            setVisible(menu, SUBMENU_SHOW_MAP, !isEmpty);
            setVisible(menu, MENU_SORT, !isEmpty);
            setVisible(menu, MENU_REFRESH_STORED, !isEmpty && (isConcrete || type != CacheListType.OFFLINE));
            setVisible(menu, MENU_DROP_CACHES, !isEmpty);
            setVisible(menu, MENU_DROP_CACHES_AND_LIST, isConcrete && !isEmpty);
            setVisible(menu, MENU_DELETE_EVENTS, isConcrete && !isEmpty && containsEvents());
            setVisible(menu, MENU_MOVE_TO_LIST, !isEmpty);
            setVisible(menu, MENU_EXPORT, !isEmpty);
            setVisible(menu, MENU_REMOVE_FROM_HISTORY, !isEmpty);
            setVisible(menu, MENU_CLEAR_OFFLINE_LOGS, !isEmpty && containsOfflineLogs());
            setVisible(menu, MENU_IMPORT_GPX, isConcrete);
            setVisible(menu, MENU_IMPORT_WEB, isConcrete);

            if (navigationMenu != null) {
                navigationMenu.setVisible(!isEmpty);
            }

            final boolean hasSelection = adapter != null && adapter.getCheckedCount() > 0;
            final boolean isNonDefaultList = isConcrete && listId != StoredList.STANDARD_LIST_ID;

            if (type == CacheListType.OFFLINE) { // only offline list
                setMenuItemLabel(menu, MENU_DROP_CACHES, R.string.caches_drop_selected, R.string.caches_drop_all);
                menu.findItem(MENU_DROP_CACHES_AND_LIST).setVisible(!hasSelection && isNonDefaultList && !adapter.isFiltered());
                setMenuItemLabel(menu, MENU_REFRESH_STORED, R.string.caches_refresh_selected, R.string.caches_refresh_all);
                setMenuItemLabel(menu, MENU_MOVE_TO_LIST, R.string.caches_move_selected, R.string.caches_move_all);
            } else { // search and history list (all other than offline)
                setMenuItemLabel(menu, MENU_REFRESH_STORED, R.string.caches_store_selected, R.string.caches_store_offline);
            }

            MenuItem item = menu.findItem(MENU_DROP_LIST);
            if (item != null) {
                item.setVisible(isNonDefaultList);
            }
            item = menu.findItem(MENU_RENAME_LIST);
            if (item != null) {
                item.setVisible(isNonDefaultList);
            }

            final boolean multipleLists = cgData.getLists().size() >= 2;
            item = menu.findItem(MENU_SWITCH_LIST);
            if (item != null) {
                item.setVisible(multipleLists);
            }
            item = menu.findItem(MENU_MOVE_TO_LIST);
            if (item != null) {
                item.setVisible(!isEmpty);
            }

            setMenuItemLabel(menu, MENU_REMOVE_FROM_HISTORY, R.string.cache_remove_from_history, R.string.cache_clear_history);
            setMenuItemLabel(menu, MENU_EXPORT, R.string.export, R.string.export);
        } catch (Exception e) {
            Log.e("cgeocaches.onPrepareOptionsMenu", e);
        }

        return true;
    }

    private boolean containsEvents() {
        for (Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (cache.isEventCache()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsOfflineLogs() {
        for (Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (cache.isLogOffline()) {
                return true;
            }
        }
        return false;
    }

    private void setMenuItemLabel(final Menu menu, final int menuId, final int resIdSelection, final int resId) {
        final MenuItem menuItem = menu.findItem(menuId);
        if (menuItem == null) {
            return;
        }
        boolean hasSelection = adapter != null && adapter.getCheckedCount() > 0;
        if (hasSelection) {
            menuItem.setTitle(res.getString(resIdSelection) + " (" + adapter.getCheckedCount() + ")");
        } else {
            menuItem.setTitle(res.getString(resId));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case MENU_SWITCH_SELECT_MODE:
                adapter.switchSelectMode();
                invalidateOptionsMenuCompatible();
                return true;
            case MENU_REFRESH_STORED:
                refreshStored(adapter.getCheckedOrAllCaches());
                invalidateOptionsMenuCompatible();
                return true;
            case MENU_DROP_CACHES:
                dropStored(false);
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_DROP_CACHES_AND_LIST:
                dropStored(true);
                invalidateOptionsMenuCompatible();
                return true;
            case MENU_IMPORT_GPX:
                importGpx();
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_CREATE_LIST:
                new StoredList.UserInterface(this).promptForListCreation(null);
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_DROP_LIST:
                removeList(true);
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_RENAME_LIST:
                renameList();
                return false;
            case MENU_INVERT_SELECTION:
                adapter.invertSelection();
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_SWITCH_LIST:
                selectList(null);
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_FILTER:
                showFilterMenu(null);
                return true;
                        case MENU_NEGATE_FILTER:
                if (filter != null) {
                cacheList.clear();
                    cacheList.addAll(adapter.negateFilter(filter));

                    int duplicates = 0;
                    int size = cacheList.size();
                    for (int i = 0; i < size - 1; i++) {
                        for (int j = i + 1; j < size; j++) {
                            if (!cacheList.get(j).equals(cacheList.get(i))) {
                                continue;
                            }
                            duplicates++;
                            cacheList.remove(j);
                            j--;
                            size--;
                        }
                }
                }
            case MENU_SORT:
                new ComparatorUserInterface(this).selectComparator(adapter.getCacheComparator(), new RunnableWithArgument<CacheComparator>() {
                    @Override
                    public void run(CacheComparator selectedComparator) {
                        setComparator(selectedComparator);
                    }
                });
                return true;
            case MENU_IMPORT_WEB:
                importWeb();
                return false;
            case MENU_EXPORT:
                ExportFactory.showExportMenu(adapter.getCheckedOrAllCaches(), this);
                return false;
            case MENU_REMOVE_FROM_HISTORY:
                removeFromHistoryCheck();
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_MOVE_TO_LIST:
                moveCachesToOtherList();
                invalidateOptionsMenuCompatible();
                return true;
            case MENU_DELETE_EVENTS:
                deletePastEvents();
                invalidateOptionsMenuCompatible();
                return true;
            case MENU_CLEAR_OFFLINE_LOGS:
                clearOfflineLogs();
                invalidateOptionsMenuCompatible();
                return true;
            default:
                return CacheListAppFactory.onMenuItemSelected(item, cacheList, this, search);
        }
    }

    public void deletePastEvents() {
        progress.show(this, null, res.getString(R.string.caches_drop_progress), true, dropDetailsHandler.obtainMessage(MSG_CANCEL));
        final List<Geocache> deletion = new ArrayList<Geocache>();
        for (Geocache cache : adapter.getCheckedOrAllCaches()) {
            if (cache.isEventCache()) {
                final Date eventDate = cache.getHiddenDate();
                if (DateUtils.daysSince(eventDate.getTime()) > 0) {
                    deletion.add(cache);
                }
            }
        }
        new DropDetailsThread(dropDetailsHandler, deletion).start();
    }

    public void clearOfflineLogs() {
        progress.show(this, null, res.getString(R.string.caches_clear_offlinelogs_progress), true, clearOfflineLogsHandler.obtainMessage(MSG_CANCEL));
        new ClearOfflineLogsThread(clearOfflineLogsHandler).start();
    }

    /**
     * called from the filter bar view
     */
    @Override
    public void showFilterMenu(final View view) {
        new FilterUserInterface(this).selectFilter(new RunnableWithArgument<IFilter>() {
            @Override
            public void run(IFilter selectedFilter) {
                if (selectedFilter != null) {
                    setFilter(selectedFilter);
                }
                else {
                    // clear filter
                    setFilter(null);
                }
            }
        });
    }

    private void setComparator(final CacheComparator comparator) {
        adapter.setComparator(comparator);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);

        AdapterContextMenuInfo adapterInfo = null;
        try {
            adapterInfo = (AdapterContextMenuInfo) info;
        } catch (Exception e) {
            Log.w("cgeocaches.onCreateContextMenu", e);
        }

        if (adapterInfo == null || adapterInfo.position >= adapter.getCount()) {
            return;
        }
        final Geocache cache = adapter.getItem(adapterInfo.position);

        menu.setHeaderTitle(StringUtils.defaultIfBlank(cache.getName(), cache.getGeocode()));

        contextMenuGeocode = cache.getGeocode();

        if (cache.getCoords() != null) {
            menu.add(0, MENU_DEFAULT_NAVIGATION, 0, NavigationAppFactory.getDefaultNavigationApplication().getName());
            menu.add(1, MENU_NAVIGATION, 0, res.getString(R.string.cache_menu_navigate)).setIcon(R.drawable.ic_menu_mapmode);
            LoggingUI.addMenuItems(menu, cache);
            menu.add(0, MENU_CACHE_DETAILS, 0, res.getString(R.string.cache_menu_details));
        }
        if (cache.isOffline()) {
            menu.add(0, MENU_DROP_CACHE, 0, res.getString(R.string.cache_offline_drop));
            menu.add(0, MENU_MOVE_TO_LIST, 0, res.getString(R.string.cache_menu_move_list));
            menu.add(0, MENU_EXPORT, 0, res.getString(R.string.export));
        }
        else {
            menu.add(0, MENU_STORE_CACHE, 0, res.getString(R.string.cache_offline_store));
        }
    }

    private void moveCachesToOtherList() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.cache_menu_move_list, new RunnableWithArgument<Integer>() {

            @Override
            public void run(Integer newListId) {
                cgData.moveToList(adapter.getCheckedOrAllCaches(), newListId);
                adapter.setSelectMode(false);

                refreshCurrentList();
            }
        }, true, listId);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
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
        } catch (Exception e) {
            Log.w("cgeocaches.onContextItemSelected", e);
        }

        final Geocache cache = adapterInfo != null ? getCacheFromAdapter(adapterInfo) : null;

        // just in case the list got resorted while we are executing this code
        if (cache == null) {
            return true;
        }

        final int id = item.getItemId();
        switch (id) {
            case MENU_DEFAULT_NAVIGATION:
                NavigationAppFactory.startDefaultNavigationApplication(1, this, cache);
                break;
            case MENU_NAVIGATION:
                NavigationAppFactory.showNavigationMenu(this, cache, null, null);
                break;
            case MENU_CACHE_DETAILS:
                final Intent cachesIntent = new Intent(this, CacheDetailActivity.class);
                cachesIntent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
                cachesIntent.putExtra(Intents.EXTRA_NAME, cache.getName());
                startActivity(cachesIntent);
                break;
            case MENU_DROP_CACHE:
                cache.drop(new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        adapter.notifyDataSetChanged();
                        refreshCurrentList();
                    }
                });
                break;
            case MENU_MOVE_TO_LIST:
                new StoredList.UserInterface(this).promptForListSelection(R.string.cache_menu_move_list, new RunnableWithArgument<Integer>() {

                    @Override
                    public void run(Integer newListId) {
                        cgData.moveToList(Collections.singletonList(cache), newListId);
                        adapter.setSelectMode(false);
                        refreshCurrentList();
                    }
                }, true, listId);
                break;
            case MENU_STORE_CACHE:
                refreshStored(Collections.singletonList(cache));
                break;
            case MENU_EXPORT:
                ExportFactory.showExportMenu(Collections.singletonList(cache), this);
                return false;
            default:
                // we must remember the menu info for the sub menu, there is a bug
                // in Android:
                // https://code.google.com/p/android/issues/detail?id=7139
                lastMenuInfo = info;
                LoggingUI.onMenuItemSelected(item, this, cache);
        }

        return true;
    }

    /**
     * Extract a cache from adapter data.
     *
     * @param adapterInfo
     *            an adapterInfo
     * @return the pointed cache
     */
    private Geocache getCacheFromAdapter(final AdapterContextMenuInfo adapterInfo) {
        final Geocache cache = adapter.getItem(adapterInfo.position);
        if (cache.getGeocode().equalsIgnoreCase(contextMenuGeocode)) {
            return cache;
        }

        return adapter.findCacheByGeocode(contextMenuGeocode);
    }

    private boolean setFilter(IFilter filter) {
        adapter.setFilter(filter);
        prepareFilterBar();
        updateTitle();
        invalidateOptionsMenuCompatible();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (adapter.isSelectMode()) {
                adapter.setSelectMode(false);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setAdapter() {
        if (listFooter == null) {
            if (inflater == null) {
                inflater = getLayoutInflater();
            }
            listFooter = inflater.inflate(R.layout.caches_footer, null);
            listFooter.setClickable(true);
            listFooter.setOnClickListener(new MoreCachesListener());

            listFooterText = (TextView) listFooter.findViewById(R.id.more_caches);

            getListView().addFooterView(listFooter);
        }

        if (adapter == null) {
            final ListView list = getListView();

            registerForContextMenu(list);
            list.setLongClickable(true);

            adapter = new CacheListAdapter(this, cacheList, type);
            setListAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        adapter.forceSort();
        adapter.reFilter();
    }

    private void showFooterLoadingCaches() {
        if (listFooter == null) {
            return;
        }

        listFooterText.setText(res.getString(R.string.caches_more_caches_loading));
        listFooter.setClickable(false);
        listFooter.setOnClickListener(null);
    }

    private void showFooterMoreCaches() {
        if (listFooter == null) {
            return;
        }

        boolean enableMore = (type != CacheListType.OFFLINE && cacheList.size() < MAX_LIST_ITEMS);
        if (enableMore && search != null) {
            final int count = search.getTotal();
            enableMore = enableMore && count > 0 && cacheList.size() < count;
        }

        if (enableMore) {
            listFooterText.setText(res.getString(R.string.caches_more_caches) + " (" + res.getString(R.string.caches_more_caches_currently) + ": " + cacheList.size() + ")");
            listFooter.setOnClickListener(new MoreCachesListener());
        } else {
            listFooterText.setText(res.getString(CollectionUtils.isEmpty(cacheList) ? R.string.caches_no_cache : R.string.caches_more_caches_no));
            listFooter.setOnClickListener(null);
        }
        listFooter.setClickable(enableMore);
    }

    private void startGeoAndDir() {
        geoDirHandler.startGeo();
        if (Settings.isLiveMap()) {
            geoDirHandler.startDir();
        }
    }

    private void removeGeoAndDir() {
        geoDirHandler.stopGeoAndDir();
    }

    private void importGpx() {
        GpxFileListActivity.startSubActivity(this, listId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshCurrentList();
    }

    public void refreshStored(final List<Geocache> caches) {
        detailTotal = caches.size();
        if (detailTotal == 0) {
            return;
        }

        if (Settings.getChooseList() && type != CacheListType.OFFLINE) {
            // let user select list to store cache in
            new StoredList.UserInterface(this).promptForListSelection(R.string.list_title,
                    new RunnableWithArgument<Integer>() {
                        @Override
                        public void run(final Integer selectedListId) {
                            refreshStored(caches, selectedListId);
                        }
                    }, true, StoredList.TEMPORARY_LIST_ID);
        } else {
            refreshStored(caches, this.listId);
        }
    }

    private void refreshStored(final List<Geocache> caches, final int storeListId) {
        detailProgress = 0;

        showProgress(false);

        int etaTime = ((detailTotal * 25) / 60);
        String message;
        if (etaTime < 1) {
            message = res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm);
        } else {
            message = res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getQuantityString(R.plurals.caches_eta_mins, etaTime);
        }

        progress.show(this, null, message, ProgressDialog.STYLE_HORIZONTAL, loadDetailsHandler.obtainMessage(MSG_CANCEL));
        progress.setMaxProgressAndReset(detailTotal);

        detailProgressTime = System.currentTimeMillis();

        threadDetails = new LoadDetailsThread(loadDetailsHandler, caches, storeListId);
        threadDetails.start();
    }

    public void removeFromHistoryCheck() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(true);
        dialog.setTitle(res.getString(R.string.caches_removing_from_history));
        dialog.setMessage((adapter != null && adapter.getCheckedCount() > 0) ? res.getString(R.string.cache_remove_from_history)
                : res.getString(R.string.cache_clear_history));
        dialog.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                removeFromHistory();
                dialog.cancel();
            }
        });
        dialog.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }

    public void removeFromHistory() {
        final List<Geocache> caches = adapter.getCheckedOrAllCaches();
        final String geocodes[] = new String[caches.size()];
        for (int i = 0; i < geocodes.length; i++) {
            geocodes[i] = caches.get(i).getGeocode();
        }
        Bundle b = new Bundle();
        b.putStringArray(Intents.EXTRA_CACHELIST, geocodes);
        getSupportLoaderManager().initLoader(CacheListLoaderType.REMOVE_FROM_HISTORY.ordinal(), b, this);
    }

    public void importWeb() {
        detailProgress = 0;

        showProgress(false);
        progress.show(this, null, res.getString(R.string.web_import_waiting), true, downloadFromWebHandler.obtainMessage(MSG_CANCEL));

        threadWeb = new LoadFromWebThread(downloadFromWebHandler, listId);
        threadWeb.start();
    }

    public void dropStored(final boolean removeListAfterwards) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(true);
        dialog.setTitle(res.getString(R.string.caches_drop_stored));

        if (adapter.getCheckedCount() > 0) {
            dialog.setMessage(res.getString(R.string.caches_drop_selected_ask));
        } else {
            dialog.setMessage(res.getString(R.string.caches_drop_all_ask));
        }
        dialog.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dropSelected();
                if (removeListAfterwards) {
                    removeList(false);
                }
                dialog.cancel();
            }
        });
        dialog.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }

    public void dropSelected() {
        progress.show(this, null, res.getString(R.string.caches_drop_progress), true, dropDetailsHandler.obtainMessage(MSG_CANCEL));
        new DropDetailsThread(dropDetailsHandler, adapter.getCheckedOrAllCaches()).start();
    }

    /**
     * Thread to refresh the cache details.
     */

    private class LoadDetailsThread extends Thread {

        final private Handler handler;
        final private int listIdLD;
        private volatile boolean needToStop = false;
        private long last = 0L;
        final private List<Geocache> caches;

        public LoadDetailsThread(Handler handlerIn, List<Geocache> caches, int listId) {
            handler = handlerIn;
            this.caches = caches;

            // in case of online lists, set the list id to the standard list
            this.listIdLD = Math.max(listId, StoredList.STANDARD_LIST_ID);
        }

        public void kill() {
            needToStop = true;
        }

        @Override
        public void run() {
            removeGeoAndDir();

            final List<Geocache> cachesWithStaticMaps = new ArrayList<Geocache>(this.caches.size());
            for (Geocache cache : this.caches) {
                if (Settings.isStoreOfflineMaps() && cache.hasStaticMap()) {
                    cachesWithStaticMaps.add(cache);
                    continue;
                }
                if (!refreshCache(cache)) {
                    // in case of interruption avoid the second loop
                    cachesWithStaticMaps.clear();
                    break;
                }
            }

            for (Geocache cache : cachesWithStaticMaps) {
                if (!refreshCache(cache)) {
                    break;
                }
            }

            handler.sendEmptyMessage(MSG_RESTART_GEO_AND_DIR);
            handler.sendEmptyMessage(MSG_DONE);
        }

        /**
         * Refreshes the cache information.
         *
         * @param cache
         *            The cache to refresh
         * @return
         *         <code>false</code> if the storing was interrupted, <code>true</code> otherwise
         */
        private boolean refreshCache(Geocache cache) {
            try {
                if (needToStop) {
                    throw new InterruptedException("Stopped storing process.");
                }

                if ((System.currentTimeMillis() - last) < 1500) {
                    try {
                        int delay = 1000 + ((Double) (Math.random() * 1000)).intValue() - (int) (System.currentTimeMillis() - last);
                        if (delay < 0) {
                            delay = 500;
                        }

                        Log.i("Waiting for next cache " + delay + " ms");
                    } catch (Exception e) {
                        Log.e("cgeocaches.LoadDetailsThread.sleep", e);
                    }
                }

                if (needToStop) {
                    throw new InterruptedException("Stopped storing process.");
                }

                detailProgress++;
                cache.refresh(listIdLD, null);

                handler.sendEmptyMessage(cacheList.indexOf(cache));

                yield();
            } catch (InterruptedException e) {
                Log.i(e.getMessage());
                return false;
            } catch (Exception e) {
                Log.e("cgeocaches.LoadDetailsThread", e);
            }

            last = System.currentTimeMillis();
            return true;
        }
    }

    private class LoadFromWebThread extends Thread {

        final private Handler handler;
        final private int listIdLFW;
        private volatile boolean needToStop = false;

        public LoadFromWebThread(Handler handlerIn, int listId) {
            handler = handlerIn;
            listIdLFW = listId;
        }

        public void kill() {
            needToStop = true;
        }

        @Override
        public void run() {

            removeGeoAndDir();

            int delay = -1;
            int times = 0;

            int ret = MSG_DONE;
            while (!needToStop && times < 3 * 60 / 5) { // maximum: 3 minutes, every 5 seconds
                //download new code
                String deviceCode = Settings.getWebDeviceCode();
                if (deviceCode == null) {
                    deviceCode = "";
                }
                final Parameters params = new Parameters("code", deviceCode);
                HttpResponse responseFromWeb = Network.getRequest("http://send2.cgeo.org/read.html", params);

                if (responseFromWeb != null && responseFromWeb.getStatusLine().getStatusCode() == 200) {
                    final String response = Network.getResponseData(responseFromWeb);
                    if (response.length() > 2) {
                        delay = 1;
                        handler.sendMessage(handler.obtainMessage(1, response));
                        yield();

                        Geocache.storeCache(null, response, listIdLFW, false, null);

                        handler.sendMessage(handler.obtainMessage(2, response));
                        yield();
                    } else if ("RG".equals(response)) {
                        //Server returned RG (registration) and this device no longer registered.
                        Settings.setWebNameCode(null, null);
                        ret = -3;
                        needToStop = true;
                        break;
                    } else {
                        delay = 0;
                        handler.sendEmptyMessage(0);
                        yield();
                    }
                }
                if (responseFromWeb == null || responseFromWeb.getStatusLine().getStatusCode() != 200) {
                    ret = -2;
                    needToStop = true;
                    break;
                }

                try {
                    yield();
                    if (delay == 0) {
                        sleep(5000); //No caches 5s
                        times++;
                    } else {
                        sleep(500); //Cache was loaded 0.5s
                        times = 0;
                    }
                } catch (InterruptedException e) {
                    Log.e("cgeocaches.LoadFromWebThread.sleep", e);
                }
            }

            handler.sendEmptyMessage(ret);

            startGeoAndDir();
        }
    }

    private class DropDetailsThread extends Thread {

        final private Handler handler;
        final private List<Geocache> selected;

        public DropDetailsThread(Handler handlerIn, List<Geocache> selectedIn) {
            handler = handlerIn;
            selected = selectedIn;
        }

        @Override
        public void run() {
            removeGeoAndDir();
            cgData.markDropped(selected);
            handler.sendEmptyMessage(MSG_DONE);

            startGeoAndDir();
        }
    }

    private class ClearOfflineLogsThread extends Thread {

        final private Handler handler;
        final private List<Geocache> selected;

        public ClearOfflineLogsThread(Handler handlerIn) {
            handler = handlerIn;
            selected = adapter.getCheckedOrAllCaches();
        }

        @Override
        public void run() {
            cgData.clearLogsOffline(selected);
            handler.sendEmptyMessage(MSG_DONE);
        }
    }

    private class MoreCachesListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            showProgress(true);
            showFooterLoadingCaches();
            listFooter.setOnClickListener(null);

            currentLoader.startLoading();
        }
    }

    private void hideLoading() {
        final ListView list = getListView();
        if (list.getVisibility() == View.GONE) {
            list.setVisibility(View.VISIBLE);
            final View loading = findViewById(R.id.loading);
            loading.setVisibility(View.GONE);
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void selectList(View view) {
        if (type != CacheListType.OFFLINE) {
            return;
        }
        new StoredList.UserInterface(this).promptForListSelection(R.string.list_title, new RunnableWithArgument<Integer>() {

            @Override
            public void run(final Integer selectedListId) {
                switchListById(selectedListId);
            }
        });
    }

    public void switchListById(int id) {
        if (id < 0) {
            return;
        }

        StoredList list = cgData.getList(id);
        if (list == null) {
            return;
        }

        listId = list.id;
        title = list.title;

        Settings.saveLastList(listId);

        showProgress(true);
        showFooterLoadingCaches();
        cgData.moveToList(adapter.getCheckedCaches(), listId);

        currentLoader = (AbstractSearchLoader) getSupportLoaderManager().initLoader(CacheListType.OFFLINE.ordinal(), new Bundle(), this);
        currentLoader.reset();
        currentLoader.startLoading();

        invalidateOptionsMenuCompatible();
    }

    private void renameList() {
        new StoredList.UserInterface(this).promptForListRename(listId, new Runnable() {

            @Override
            public void run() {
                refreshCurrentList();
            }
        });
    }

    private void removeListInternal() {
        if (cgData.removeList(listId)) {
            showToast(res.getString(R.string.list_dialog_remove_ok));
            switchListById(StoredList.STANDARD_LIST_ID);
        } else {
            showToast(res.getString(R.string.list_dialog_remove_err));
        }
    }

    private void removeList(final boolean askForConfirmation) {
        // if there are no caches on this list, don't bother the user with questions.
        // there is no harm in deleting the list, he could recreate it easily
        if (CollectionUtils.isEmpty(cacheList)) {
            removeListInternal();
            return;
        }

        if (!askForConfirmation) {
            removeListInternal();
            return;
        }

        // ask him, if there are caches on the list
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.list_dialog_remove_title);
        alert.setMessage(R.string.list_dialog_remove_description);
        alert.setPositiveButton(R.string.list_dialog_remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                removeListInternal();
            }
        });
        alert.setNegativeButton(res.getString(R.string.list_dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        alert.show();
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goMap(View view) {
        if (search == null || CollectionUtils.isEmpty(cacheList)) {
            showToast(res.getString(R.string.warn_no_cache_coord));

            return;
        }

        // apply filter settings (if there's a filter)
        Set<String> geocodes = new HashSet<String>();
        for (Geocache cache : adapter.getFilteredList()) {
            geocodes.add(cache.getGeocode());
        }

        final SearchResult searchToUse = new SearchResult(geocodes);
        final int count = searchToUse.getCount();
        String mapTitle = title;
        if (count > 0) {
            mapTitle = title + " [" + count + "]";
        }
        CGeoMap.startActivitySearch(this, searchToUse, mapTitle);
    }

    @Override
    public void goManual(View view) {
        switch (type) {
            case OFFLINE:
                ActivityMixin.goManual(this, "c:geo-stored");
                break;
            case HISTORY:
                ActivityMixin.goManual(this, "c:geo-history");
                break;
            default:
                ActivityMixin.goManual(this, "c:geo-nearby");
                break;
        }
    }

    private void refreshCurrentList() {
        switchListById(listId);
    }

    public static void startActivityOffline(final Context context) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.OFFLINE);
        context.startActivity(cachesIntent);
    }

    public static void startActivityOwner(final AbstractActivity context, final String userName) {
        if (!isValidUsername(context, userName)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.OWNER);
        cachesIntent.putExtra(Intents.EXTRA_USERNAME, userName);
        context.startActivity(cachesIntent);
    }

    private static boolean isValidUsername(AbstractActivity context, String username) {
        if (StringUtils.isBlank(username)) {
            context.showToast(cgeoapplication.getInstance().getString(R.string.warn_no_username));
            return false;
        }
        return true;
    }

    public static void startActivityUserName(final AbstractActivity context, final String userName) {
        if (!isValidUsername(context, userName)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.USERNAME);
        cachesIntent.putExtra(Intents.EXTRA_USERNAME, userName);
        context.startActivity(cachesIntent);
    }

    private void prepareFilterBar() {
        if (Settings.getCacheType() != CacheType.ALL || adapter.isFiltered()) {
            final StringBuilder output = new StringBuilder(Settings.getCacheType().getL10n());

            if (adapter.isFiltered()) {
                output.append(", ").append(adapter.getFilterName());
            }

            ((TextView) findViewById(R.id.filter_text)).setText(output.toString());
            findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.filter_bar).setVisibility(View.GONE);
        }
    }

    /**
     * set date comparator for pure event lists
     *
     * TODO: move this method into the adapter
     */
    private void setDateComparatorForEventList() {
        if (CollectionUtils.isNotEmpty(cacheList)) {
            boolean eventsOnly = true;
            for (Geocache cache : cacheList) {
                if (!cache.isEventCache()) {
                    eventsOnly = false;
                    break;
                }
            }
            if (eventsOnly) {
                adapter.setComparator(new EventDateComparator());
            }
            else if (type == CacheListType.HISTORY) {
                adapter.setComparator(new VisitComparator());
            }
            else if (adapter.getCacheComparator() != null && adapter.getCacheComparator() instanceof EventDateComparator) {
                adapter.setComparator(null);
            }
        }
    }

    public static void startActivityNearest(final AbstractActivity context, final Geopoint coordsNow) {
        if (!isValidCoords(context, coordsNow)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.NEAREST);
        cachesIntent.putExtra(Intents.EXTRAS_COORDS, coordsNow);
        context.startActivity(cachesIntent);
    }

    public static void startActivityHistory(Context context) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.HISTORY);
        context.startActivity(cachesIntent);
    }

    public static void startActivityAddress(final Context context, final Geopoint coords, final String address) {
        final Intent addressIntent = new Intent(context, cgeocaches.class);
        addressIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.ADDRESS);
        addressIntent.putExtra(Intents.EXTRAS_COORDS, coords);
        addressIntent.putExtra(Intents.EXTRA_ADDRESS, address);
        context.startActivity(addressIntent);
    }

    public static void startActivityCoordinates(final AbstractActivity context, final Geopoint coords) {
        if (!isValidCoords(context, coords)) {
            return;
        }
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.COORDINATE);
        cachesIntent.putExtra(Intents.EXTRAS_COORDS, coords);
        context.startActivity(cachesIntent);
    }

    private static boolean isValidCoords(AbstractActivity context, Geopoint coords) {
        if (coords == null) {
            context.showToast(cgeoapplication.getInstance().getString(R.string.warn_no_coordinates));
            return false;
        }
        return true;
    }

    public static void startActivityKeyword(final AbstractActivity context, final String keyword) {
        if (keyword == null) {
            context.showToast(cgeoapplication.getInstance().getString(R.string.warn_no_keyword));
            return;
        }
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.KEYWORD);
        cachesIntent.putExtra(Intents.EXTRA_KEYWORD, keyword);
        context.startActivity(cachesIntent);
    }

    public static void startActivityMap(final Context context, final SearchResult search) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(Intents.EXTRA_LIST_TYPE, CacheListType.MAP);
        cachesIntent.putExtra(Intents.EXTRA_SEARCH, search);
        context.startActivity(cachesIntent);
    }

    // Loaders

    @Override
    public Loader<SearchResult> onCreateLoader(int type, Bundle extras) {
        AbstractSearchLoader loader = null;
        CacheListLoaderType enumType = CacheListLoaderType.values()[type];
        final String username = extras.getString(Intents.EXTRA_USERNAME);
        switch (enumType) {
            case OFFLINE:
                listId = Settings.getLastList();
                if (listId <= StoredList.TEMPORARY_LIST_ID) {
                    listId = StoredList.STANDARD_LIST_ID;
                    title = res.getString(R.string.stored_caches_button);
                } else {
                    final StoredList list = cgData.getList(listId);
                    // list.id may be different if listId was not valid
                    listId = list.id;
                    title = list.title;
                }

                loader = new OfflineGeocacheListLoader(this.getBaseContext(), coords, listId);

                break;
            case HISTORY:
                title = res.getString(R.string.caches_history);
                loader = new HistoryGeocacheListLoader(app, coords);
                break;
            case NEAREST:
                title = res.getString(R.string.caches_nearby);
                loader = new CoordsGeocacheListLoader(app, coords);
                break;
            case COORDINATE:
                title = coords.toString();
                loader = new CoordsGeocacheListLoader(app, coords);
                break;
            case KEYWORD:
                final String keyword = extras.getString(Intents.EXTRA_KEYWORD);
                title = keyword;
                loader = new KeywordGeocacheListLoader(app, keyword);
                break;
            case ADDRESS:
                final String address = extras.getString(Intents.EXTRA_ADDRESS);
                if (StringUtils.isNotBlank(address)) {
                    title = address;
                } else {
                    title = coords.toString();
                }
                if (coords != null) {
                    loader = new CoordsGeocacheListLoader(app, coords);
                    }
                else {
                    loader = new AddressGeocacheListLoader(app, address);
                }
                break;
            case USERNAME:
                title = username;
                loader = new UsernameGeocacheListLoader(app, username);
                break;
            case OWNER:
                title = username;
                loader = new OwnerGeocacheListLoader(app, username);
                break;
            case MAP:
                //TODO Build Nullloader
                title = res.getString(R.string.map_map);
                search = (SearchResult) extras.get(Intents.EXTRA_SEARCH);
                replaceCacheListFromSearch();
                loadCachesHandler.sendMessage(Message.obtain());
                break;
            case REMOVE_FROM_HISTORY:
                title = res.getString(R.string.caches_history);
                loader = new RemoveFromHistoryLoader(app, extras.getStringArray(Intents.EXTRA_CACHELIST), coords);
                break;

            default:
                title = "caches";
                setTitle(title);
                Log.e("cgeocaches.onCreate: No action or unknown action specified");
                return null;
        }
        setTitle(title);
        showProgress(true);
        showFooterLoadingCaches();

        if (loader != null) {
            loader.setRecaptchaHandler(new SearchHandler(this, res, loader));
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<SearchResult> arg0, SearchResult searchIn) {
        // The database search was moved into the UI call intentionally. If this is done before the runOnUIThread,
        // then we have 2 sets of caches in memory. This can lead to OOM for huge cache lists.
        if (searchIn != null) {
            cacheList.clear();
            final Set<Geocache> cachesFromSearchResult = searchIn.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            cacheList.addAll(cachesFromSearchResult);
            search = searchIn;
            adapter.reFilter();
            updateTitle();
            showFooterMoreCaches();
        }
        showProgress(false);
        hideLoading();
    }

    @Override
    public void onLoaderReset(Loader<SearchResult> arg0) {
        //Not interessting
    }
}

