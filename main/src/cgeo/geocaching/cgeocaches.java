package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cachelist.CacheListAppFactory;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.filter.AttributeFilter;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.filter.ModifiedFilter;
import cgeo.geocaching.filter.SizeFilter;
import cgeo.geocaching.filter.StateFilter;
import cgeo.geocaching.filter.TrackablesFilter;
import cgeo.geocaching.filter.TypeFilter;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DateComparator;
import cgeo.geocaching.sorting.DifficultyComparator;
import cgeo.geocaching.sorting.EventDateComparator;
import cgeo.geocaching.sorting.FindsComparator;
import cgeo.geocaching.sorting.GeocodeComparator;
import cgeo.geocaching.sorting.InventoryComparator;
import cgeo.geocaching.sorting.NameComparator;
import cgeo.geocaching.sorting.PopularityComparator;
import cgeo.geocaching.sorting.RatingComparator;
import cgeo.geocaching.sorting.SizeComparator;
import cgeo.geocaching.sorting.StateComparator;
import cgeo.geocaching.sorting.TerrainComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.sorting.VoteComparator;
import cgeo.geocaching.ui.CacheListAdapter;
import cgeo.geocaching.utils.RunnableWithArgument;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class cgeocaches extends AbstractListActivity {

    private static final int MAX_LIST_ITEMS = 1000;
    private static final String EXTRAS_LIST_TYPE = "type";
    private static final int MENU_REFRESH_STORED = 2;
    private static final int MENU_CACHE_DETAILS = 4;
    private static final int MENU_DROP_CACHES = 5;
    private static final int MENU_IMPORT_GPX = 6;
    private static final int MENU_CREATE_LIST = 7;
    private static final int MENU_DROP_LIST = 8;
    private static final int MENU_INVERT_SELECTION = 9;
    private static final int MENU_SORT_DISTANCE = 10;
    private static final int MENU_SORT_DIFFICULTY = 11;
    private static final int MENU_SORT_TERRAIN = 12;
    private static final int MENU_SORT_SIZE = 13;
    private static final int MENU_SORT_FAVORITES = 14;
    private static final int MENU_SORT_NAME = 15;
    private static final int MENU_SORT_GEOCODE = 16;
    private static final int MENU_SWITCH_LIST = 17;
    private static final int MENU_SORT_RATING = 18;
    private static final int MENU_SORT_VOTE = 19;
    private static final int MENU_SORT_INVENTORY = 20;
    private static final int MENU_IMPORT_WEB = 21;
    private static final int MENU_EXPORT_NOTES = 22;
    private static final int MENU_REMOVE_FROM_HISTORY = 23;
    private static final int MENU_DROP_CACHE = 24;
    private static final int MENU_MOVE_TO_LIST = 25;
    private static final int MENU_FILTER_CLEAR = 26;
    private static final int MENU_FILTER_TRACKABLES = 27;
    private static final int SUBMENU_FILTER_SIZE = 28;
    private static final int SUBMENU_FILTER_TYPE = 29;
    private static final int MENU_SWITCH_SELECT_MODE = 52;
    private static final int SUBMENU_SHOW_MAP = 54;
    private static final int SUBMENU_MANAGE_LISTS = 55;
    private static final int SUBMENU_MANAGE_OFFLINE = 56;
    private static final int SUBMENU_SORT = 57;
    private static final int SUBMENU_FILTER = 58;
    private static final int SUBMENU_IMPORT = 59;
    private static final int SUBMENU_MANAGE_HISTORY = 60;
    private static final int MENU_SORT_DATE = 61;
    private static final int MENU_SORT_FINDS = 62;
    private static final int MENU_SORT_STATE = 63;
    private static final int MENU_RENAME_LIST = 64;
    private static final int MENU_DROP_CACHES_AND_LIST = 65;
    private static final int MENU_DEFAULT_NAVIGATION = 66;
    private static final int SUBMENU_FILTER_ATTRIBUTES = 67;
    private static final int SUBMENU_FILTER_STATE = 68;
    private static final int MENU_NAVIGATION = 69;
    private static final int MENU_FILTER_MODIFIED = 70;

    private String action = null;
    private CacheListType type = null;
    private Geopoint coords = null;
    private CacheType cacheType = Settings.getCacheType();
    private String keyword = null;
    private String address = null;
    private String username = null;
    private SearchResult search = null;
    private List<cgCache> cacheList = new ArrayList<cgCache>();
    private CacheListAdapter adapter = null;
    private LayoutInflater inflater = null;
    private View listFooter = null;
    private TextView listFooterText = null;
    private ProgressDialog waitDialog = null;
    private Float northHeading = 0f;
    private cgGeo geo = null;
    private cgDirection dir = null;
    private UpdateLocationCallback geoUpdate = new UpdateLocation();
    private UpdateDirectionCallback dirUpdate = new UpdateDirection();
    private String title = "";
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    private LoadDetailsThread threadDetails = null;
    private LoadFromWebThread threadWeb = null;
    private DropDetailsThread threadR = null;
    private ExportFieldNotesThread threadF = null;
    private RemoveFromHistoryThread threadH = null;
    private int listId = StoredList.TEMPORARY_LIST_ID;
    private List<StoredList> lists = null;
    private GeocodeComparator gcComparator = new GeocodeComparator();
    private Handler loadCachesHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (search != null) {
                    setTitle(title + " [" + search.getCount() + "]");
                    cacheList.clear();

                    final Set<cgCache> caches = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    if (CollectionUtils.isNotEmpty(caches)) {
                        cacheList.addAll(caches);
                        Collections.sort(cacheList, gcComparator);
                    }
                } else {
                    setTitle(title);
                }

                setAdapter();

                setDateComparatorForEventList();

                if (cacheList == null) {
                    showToast(res.getString(R.string.err_list_load_fail));
                }
                setMoreCaches();

                if (cacheList != null && search != null && search.getError() == StatusCode.UNAPPROVED_LICENSE) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(cgeocaches.this);
                    dialog.setTitle(res.getString(R.string.license));
                    dialog.setMessage(res.getString(R.string.err_license));
                    dialog.setCancelable(true);
                    dialog.setNegativeButton(res.getString(R.string.license_dismiss), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            cgBase.clearCookies();
                            dialog.cancel();
                        }
                    });
                    dialog.setPositiveButton(res.getString(R.string.license_show), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            cgBase.clearCookies();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/software/agreement.aspx?ID=0")));
                        }
                    });

                    AlertDialog alert = dialog.create();
                    alert.show();
                } else if (app != null && search != null && search.getError() != null) {
                    showToast(res.getString(R.string.err_download_fail) + " " + search.getError().getErrorString(res) + ".");

                    hideLoading();
                    showProgress(false);

                    finish();
                    return;
                }

                if (geo != null && geo.coordsNow != null) {
                    adapter.setActualCoordinates(geo.coordsNow);
                    adapter.setActualHeading(northHeading);
                }
            } catch (Exception e) {
                showToast(res.getString(R.string.err_detail_cache_find_any));
                Log.e(Settings.tag, "cgeocaches.loadCachesHandler: " + e.toString());

                hideLoading();
                showProgress(false);

                finish();
                return;
            }

            try {
                hideLoading();
                showProgress(false);
            } catch (Exception e2) {
                Log.e(Settings.tag, "cgeocaches.loadCachesHandler.2: " + e2.toString());
            }

            if (adapter != null) {
                adapter.setSelectMode(false, true);
            }
        }
    };
    private Handler loadNextPageHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (search != null) {
                    setTitle(title + " [" + search.getCount() + "]");
                    cacheList.clear();

                    final Set<cgCache> caches = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    if (CollectionUtils.isNotEmpty(caches)) {
                        cacheList.addAll(caches);
                        caches.clear();
                        Collections.sort(cacheList, gcComparator);
                    }
                    if (adapter != null) {
                        adapter.reFilter();
                    }
                } else {
                    setTitle(title);
                }

                setAdapter();

                if (cacheList == null) {
                    showToast(res.getString(R.string.err_list_load_fail));
                }
                setMoreCaches();

                if (search != null && search.getError() != null) {
                    showToast(res.getString(R.string.err_download_fail) + " " + search.getError().getErrorString(res) + ".");

                    listFooter.setOnClickListener(new MoreCachesListener());
                    hideLoading();
                    showProgress(false);

                    finish();
                    return;
                }

                if (geo != null && geo.coordsNow != null) {
                    adapter.setActualCoordinates(geo.coordsNow);
                    adapter.setActualHeading(northHeading);
                }
            } catch (Exception e) {
                showToast(res.getString(R.string.err_detail_cache_find_next));
                Log.e(Settings.tag, "cgeocaches.loadNextPageHandler: " + e.toString());
            }

            hideLoading();
            showProgress(false);

            if (adapter != null) {
                adapter.setSelectMode(false, true);
            }
        }
    };
    private Handler loadDetailsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setAdapter();

            if (msg.what > -1) {
                if (waitDialog != null) {
                    cacheList.get(msg.what).setStatusChecked(false);

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                    int minutesRemaining = ((detailTotal - detailProgress) * secondsElapsed / ((detailProgress > 0) ? detailProgress : 1) / 60);

                    waitDialog.setProgress(detailProgress);
                    if (minutesRemaining < 1) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                    } else if (minutesRemaining == 1) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getString(R.string.caches_eta_min));
                    } else {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getString(R.string.caches_eta_mins));
                    }
                }
            } else {
                if (cacheList != null && search != null) {
                    final Set<cgCache> cacheListTmp = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    if (CollectionUtils.isNotEmpty(cacheListTmp)) {
                        cacheList.clear();
                        cacheList.addAll(cacheListTmp);
                        cacheListTmp.clear();
                        Collections.sort(cacheList, gcComparator);
                    }
                }

                if (geo != null && geo.coordsNow != null) {
                    adapter.setActualCoordinates(geo.coordsNow);
                    adapter.setActualHeading(northHeading);
                }

                showProgress(false);
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }

                if (geo == null) {
                    geo = app.startGeo(geoUpdate);
                }
                if (Settings.isLiveList() && Settings.isUseCompass() && dir == null) {
                    dir = app.startDir(cgeocaches.this, dirUpdate);
                }
            }
        }
    };
    private Handler downloadFromWebHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            setAdapter();

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            if (msg.what == 0) { //no caches
                waitDialog.setMessage(res.getString(R.string.web_import_waiting));
            } else if (msg.what == 1) { //cache downloading
                waitDialog.setMessage(res.getString(R.string.web_downloading) + " " + (String) msg.obj + "...");
            } else if (msg.what == 2) { //Cache downloaded
                waitDialog.setMessage(res.getString(R.string.web_downloaded) + " " + (String) msg.obj + ".");
                refreshCurrentList();
            } else if (msg.what == -2) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
                showToast(res.getString(R.string.sendToCgeo_download_fail));
                finish();
                return;
            } else if (msg.what == -3) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
                showToast(res.getString(R.string.sendToCgeo_no_registration));
                finish();
                return;
            } else {
                if (adapter != null) {
                    adapter.setSelectMode(false, true);
                }

                cacheList.clear();

                final Set<cgCache> cacheListTmp = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                if (CollectionUtils.isNotEmpty(cacheListTmp)) {
                    cacheList.addAll(cacheListTmp);
                    cacheListTmp.clear();

                    Collections.sort(cacheList, gcComparator);
                }

                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
            }
        }
    };
    private Handler dropDetailsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (adapter != null) {
                adapter.setSelectMode(false, true);
            }

            refreshCurrentList();

            cacheList.clear();

            final Set<cgCache> cacheListTmp = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            if (CollectionUtils.isNotEmpty(cacheListTmp)) {
                cacheList.addAll(cacheListTmp);
                cacheListTmp.clear();

                Collections.sort(cacheList, gcComparator);
            }

            if (waitDialog != null) {
                waitDialog.dismiss();
                waitDialog.setOnCancelListener(null);
            }
        }
    };
    private Handler removeFromHistoryHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setAdapter();

            if (msg.what > -1) {
                cacheList.get(msg.what).setStatusChecked(false);
                waitDialog.setProgress(detailProgress);
            } else {
                if (adapter != null) {
                    adapter.setSelectMode(false, true);
                }

                // reload history list
                (new LoadByHistoryThread(loadCachesHandler)).start();

                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
            }
        }
    };
    private Handler exportFieldNotesHandler = new Handler() {

        @Override
        public void handleMessage(Message msg)
        {
            setAdapter();

            if (msg.what > -1)
            {
                cacheList.get(msg.what).setStatusChecked(false);
                waitDialog.setProgress(detailProgress);
            }
            else if (-2 == msg.what)
            {
                showToast(res.getString(R.string.info_fieldnotes_exported_to) + ": " + msg.obj.toString());
            }
            else if (-3 == msg.what)
            {
                showToast(res.getString(R.string.err_fieldnotes_export_failed));
            }
            else
            {
                if (adapter != null)
                {
                    adapter.setSelectMode(false, true);
                }

                if (waitDialog != null)
                {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
            }
        }
    };

    private Handler importGpxAttachementFinishedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            refreshCurrentList();
        }
    };

    private ContextMenuInfo lastMenuInfo;
    private String contextMenuGeocode = "";
    /**
     * the navigation menu item for the cache list (not the context menu!), or <code>null</code>
     */
    private MenuItem navigationMenu;

    public cgeocaches() {
        super(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        app.setAction(action);

        setTheme();
        setContentView(R.layout.caches);
        setTitle("caches");

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Object typeObject = extras.get(EXTRAS_LIST_TYPE);
            type = (typeObject instanceof CacheListType) ? (CacheListType) typeObject : CacheListType.OFFLINE;
            coords = new Geopoint(extras.getDouble("latitude"), extras.getDouble("longitude"));
            cacheType = Settings.getCacheType();
            keyword = extras.getString("keyword");
            address = extras.getString("address");
            username = extras.getString("username");
        }
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            type = CacheListType.OFFLINE;
            if (coords == null) {
                coords = new Geopoint(0, 0);
            }
        }

        init();

        Thread threadPure;
        cgSearchThread thread;

        switch (type) {
            case OFFLINE:
                listId = Settings.getLastList();
                if (listId <= StoredList.TEMPORARY_LIST_ID) {
                    listId = StoredList.STANDARD_LIST_ID;
                    title = res.getString(R.string.stored_caches_button);
                } else {
                    final StoredList list = app.getList(listId);
                    // list.id may be different if listId was not valid
                    listId = list.id;
                    title = list.title;
                }

                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                threadPure = new LoadByOfflineThread(loadCachesHandler, coords, listId);
                threadPure.start();

                break;
            case HISTORY:
                title = res.getString(R.string.caches_history);
                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                threadPure = new LoadByHistoryThread(loadCachesHandler);
                threadPure.start();
                break;
            case NEAREST:
                action = "pending";
                title = res.getString(R.string.caches_nearby);
                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                thread = new LoadByCoordsThread(loadCachesHandler, coords);
                thread.setRecaptchaHandler(new cgSearchHandler(this, res, thread));
                thread.start();
                break;
            case COORDINATE:
                action = "planning";
                title = coords.toString();
                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                thread = new LoadByCoordsThread(loadCachesHandler, coords);
                thread.setRecaptchaHandler(new cgSearchHandler(this, res, thread));
                thread.start();
                break;
            case KEYWORD:
                title = keyword;
                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                thread = new LoadByKeywordThread(loadCachesHandler, keyword);
                thread.setRecaptchaHandler(new cgSearchHandler(this, res, thread));
                thread.start();
                break;
            case ADDRESS:
                action = "planning";
                if (StringUtils.isNotBlank(address)) {
                    title = address;
                    setTitle(title);
                    showProgress(true);
                    setLoadingCaches();
                } else {
                    title = coords.toString();
                    setTitle(title);
                    showProgress(true);
                    setLoadingCaches();
                }

                thread = new LoadByCoordsThread(loadCachesHandler, coords);
                thread.setRecaptchaHandler(new cgSearchHandler(this, res, thread));
                thread.start();
                break;
            case USERNAME:
                title = username;
                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                thread = new LoadByUserNameThread(loadCachesHandler, username);
                thread.setRecaptchaHandler(new cgSearchHandler(this, res, thread));
                thread.start();
                break;
            case OWNER:
                title = username;
                setTitle(title);
                showProgress(true);
                setLoadingCaches();

                thread = new LoadByOwnerThread(loadCachesHandler, username);
                thread.setRecaptchaHandler(new cgSearchHandler(this, res, thread));
                thread.start();
                break;
            case MAP:
                title = res.getString(R.string.map_map);
                setTitle(title);
                showProgress(true);
                SearchResult result = extras != null ? (SearchResult) extras.get("search") : null;
                search = new SearchResult(result);
                loadCachesHandler.sendMessage(Message.obtain());
                break;
            default:
                title = "caches";
                setTitle(title);
                Log.e(Settings.tag, "cgeocaches.onCreate: No action or unknown action specified");
                break;
        }
        prepareFilterBar();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            importGpxAttachement();
        }
    }

    private void importGpxAttachement() {
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.gpx_import_title))
                .setMessage(res.getString(R.string.gpx_import_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new GPXImporter(cgeocaches.this, listId, importGpxAttachementFinishedHandler).importGPX();
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();

        init();

        if (adapter != null && geo != null && geo.coordsNow != null) {
            adapter.setActualCoordinates(geo.coordsNow);
            adapter.setActualHeading(northHeading);
        }

        if (adapter != null) {
            adapter.setSelectMode(false, true);
            if (geo != null && geo.coordsNow != null) {
                adapter.forceSort(geo.coordsNow);
            }
        }

        if (loadCachesHandler != null && search != null) {
            loadCachesHandler.sendEmptyMessage(0);
        }

        // refresh standard list if it has changed (new caches downloaded)
        if (type == CacheListType.OFFLINE && listId >= StoredList.STANDARD_LIST_ID && search != null) {
            SearchResult newSearch = cgBase.searchByStored(coords, cacheType, listId);
            if (newSearch != null && newSearch.totalCnt != search.totalCnt) {
                refreshCurrentList();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (adapter != null) {
            adapter = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenuFilter = menu.addSubMenu(0, SUBMENU_FILTER, 0, res.getString(R.string.caches_filter)).setIcon(R.drawable.ic_menu_filter);
        subMenuFilter.setHeaderTitle(res.getString(R.string.caches_filter_title));
        if (Settings.getCacheType() == CacheType.ALL) {
            subMenuFilter.add(0, SUBMENU_FILTER_TYPE, 0, res.getString(R.string.caches_filter_type));
        }
        subMenuFilter.add(0, SUBMENU_FILTER_SIZE, 0, res.getString(R.string.caches_filter_size));
        subMenuFilter.add(0, SUBMENU_FILTER_ATTRIBUTES, 0, res.getString(R.string.cache_attributes));
        subMenuFilter.add(0, SUBMENU_FILTER_STATE, 0, res.getString(R.string.cache_status));
        subMenuFilter.add(0, MENU_FILTER_TRACKABLES, 0, res.getString(R.string.caches_filter_track));
        subMenuFilter.add(0, MENU_FILTER_MODIFIED, 0, res.getString(R.string.caches_filter_modified));
        subMenuFilter.add(0, MENU_FILTER_CLEAR, 0, res.getString(R.string.caches_filter_clear));

        SubMenu subMenuSort = menu.addSubMenu(0, SUBMENU_SORT, 0, res.getString(R.string.caches_sort)).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        subMenuSort.setHeaderTitle(res.getString(R.string.caches_sort_title));

        // sort the context menu labels alphabetically for easier reading
        Map<String, Integer> comparators = new HashMap<String, Integer>();
        comparators.put(res.getString(R.string.caches_sort_distance), MENU_SORT_DISTANCE);
        comparators.put(res.getString(R.string.caches_sort_difficulty), MENU_SORT_DIFFICULTY);
        comparators.put(res.getString(R.string.caches_sort_terrain), MENU_SORT_TERRAIN);
        comparators.put(res.getString(R.string.caches_sort_size), MENU_SORT_SIZE);
        comparators.put(res.getString(R.string.caches_sort_favorites), MENU_SORT_FAVORITES);
        comparators.put(res.getString(R.string.caches_sort_name), MENU_SORT_NAME);
        comparators.put(res.getString(R.string.caches_sort_gccode), MENU_SORT_GEOCODE);
        comparators.put(res.getString(R.string.caches_sort_rating), MENU_SORT_RATING);
        comparators.put(res.getString(R.string.caches_sort_vote), MENU_SORT_VOTE);
        comparators.put(res.getString(R.string.caches_sort_inventory), MENU_SORT_INVENTORY);
        comparators.put(res.getString(R.string.caches_sort_date), MENU_SORT_DATE);
        comparators.put(res.getString(R.string.caches_sort_finds), MENU_SORT_FINDS);
        comparators.put(res.getString(R.string.caches_sort_state), MENU_SORT_STATE);

        List<String> sortedLabels = new ArrayList<String>(comparators.keySet());
        Collections.sort(sortedLabels);
        for (String label : sortedLabels) {
            Integer id = comparators.get(label);
            subMenuSort.add(1, id.intValue(), 0, label).setCheckable(true).setChecked(id.intValue() == MENU_SORT_DISTANCE);
        }

        subMenuSort.setGroupCheckable(1, true, true);

        menu.add(0, MENU_SWITCH_SELECT_MODE, 0, res.getString(R.string.caches_select_mode)).setIcon(android.R.drawable.ic_menu_agenda);
        menu.add(0, MENU_INVERT_SELECTION, 0, res.getString(R.string.caches_select_invert)).setIcon(R.drawable.ic_menu_mark);
        if (type == CacheListType.OFFLINE) {
            SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_OFFLINE, 0, res.getString(R.string.caches_manage)).setIcon(android.R.drawable.ic_menu_save);
            subMenu.add(0, MENU_DROP_CACHES, 0, res.getString(R.string.caches_drop_all)); // delete saved caches
            subMenu.add(0, MENU_DROP_CACHES_AND_LIST, 0, res.getString(R.string.caches_drop_all_and_list));
            subMenu.add(0, MENU_REFRESH_STORED, 0, res.getString(R.string.cache_offline_refresh)); // download details for all caches
            subMenu.add(0, MENU_MOVE_TO_LIST, 0, res.getString(R.string.cache_menu_move_list));
            subMenu.add(0, MENU_EXPORT_NOTES, 0, res.getString(R.string.cache_export_fieldnote)); // export field notes
            if (Settings.getWebDeviceCode() == null)
            {
                menu.add(0, MENU_IMPORT_GPX, 0, res.getString(R.string.gpx_import_title)).setIcon(android.R.drawable.ic_menu_upload); // import gpx file
            } else {
                SubMenu subMenuImport = menu.addSubMenu(0, SUBMENU_IMPORT, 0, res.getString(R.string.import_title)).setIcon(android.R.drawable.ic_menu_upload); // import
                subMenuImport.add(1, MENU_IMPORT_GPX, 0, res.getString(R.string.gpx_import_title)).setCheckable(false).setChecked(false);
                subMenuImport.add(1, MENU_IMPORT_WEB, 0, res.getString(R.string.web_import_title)).setCheckable(false).setChecked(false);
            }
        } else {
            if (type == CacheListType.HISTORY)
            {
                SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_HISTORY, 0, res.getString(R.string.caches_manage)).setIcon(android.R.drawable.ic_menu_save);
                subMenu.add(0, MENU_REMOVE_FROM_HISTORY, 0, res.getString(R.string.cache_clear_history)); // remove from history
                subMenu.add(0, MENU_EXPORT_NOTES, 0, res.getString(R.string.cache_export_fieldnote)); // export field notes
            }
            menu.add(0, MENU_REFRESH_STORED, 0, res.getString(R.string.caches_store_offline)).setIcon(android.R.drawable.ic_menu_set_as); // download details for all caches
        }

        navigationMenu = CacheListAppFactory.addMenuItems(menu, this, res);

        if (type == CacheListType.OFFLINE) {
            SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_LISTS, 0, res.getString(R.string.list_menu)).setIcon(android.R.drawable.ic_menu_more);
            subMenu.add(0, MENU_CREATE_LIST, 0, res.getString(R.string.list_menu_create));
            subMenu.add(0, MENU_DROP_LIST, 0, res.getString(R.string.list_menu_drop));
            subMenu.add(0, MENU_RENAME_LIST, 0, res.getString(R.string.list_menu_rename));
            subMenu.add(0, MENU_SWITCH_LIST, 0, res.getString(R.string.list_menu_change));
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            if (adapter != null && adapter.getSelectMode()) {
                menu.findItem(MENU_SWITCH_SELECT_MODE).setTitle(res.getString(R.string.caches_select_mode_exit))
                        .setIcon(R.drawable.ic_menu_clear_playlist);
                menu.findItem(MENU_INVERT_SELECTION).setVisible(true);
            } else {
                menu.findItem(MENU_SWITCH_SELECT_MODE).setTitle(res.getString(R.string.caches_select_mode))
                        .setIcon(android.R.drawable.ic_menu_agenda);
                menu.findItem(MENU_INVERT_SELECTION).setVisible(false);
            }

            boolean hasSelection = adapter != null && adapter.getChecked() > 0;
            boolean isNonDefaultList = listId != StoredList.STANDARD_LIST_ID;

            if (type == CacheListType.OFFLINE) { // only offline list
                if (hasSelection) {
                    menu.findItem(MENU_DROP_CACHES).setTitle(res.getString(R.string.caches_drop_selected) + " (" + adapter.getChecked() + ")");
                } else {
                    menu.findItem(MENU_DROP_CACHES).setTitle(res.getString(R.string.caches_drop_all));
                }
                menu.findItem(MENU_DROP_CACHES_AND_LIST).setVisible(!hasSelection && isNonDefaultList);

                if (hasSelection) {
                    menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_refresh_selected) + " (" + adapter.getChecked() + ")");
                } else {
                    menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_refresh_all));
                }

                if (hasSelection) {
                    menu.findItem(MENU_MOVE_TO_LIST).setTitle(res.getString(R.string.caches_move_selected) + " (" + adapter.getChecked() + ")");
                } else {
                    menu.findItem(MENU_MOVE_TO_LIST).setTitle(res.getString(R.string.caches_move_all));
                }
            } else { // search and history list (all other than offline)
                if (hasSelection) {
                    menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_store_selected) + " (" + adapter.getChecked() + ")");
                } else {
                    menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_store_offline));
                }
            }

            // Hide menus if cache-list is empty
            int[] hideIfEmptyList = new int[] {
                    MENU_SWITCH_SELECT_MODE,
                    SUBMENU_MANAGE_OFFLINE,
                    SUBMENU_MANAGE_HISTORY,
                    SUBMENU_SHOW_MAP,
                    SUBMENU_SORT,
                    MENU_REFRESH_STORED };

            boolean menuVisible = cacheList.size() > 0;
            for (int itemId : hideIfEmptyList) {
                MenuItem item = menu.findItem(itemId);
                if (null != item) {
                    item.setVisible(menuVisible);
                }
            }

            if (navigationMenu != null) {
                navigationMenu.setVisible(menuVisible);
            }

            MenuItem item = menu.findItem(MENU_DROP_LIST);
            if (item != null) {
                item.setVisible(isNonDefaultList);
            }
            item = menu.findItem(MENU_RENAME_LIST);
            if (item != null) {
                item.setVisible(isNonDefaultList);
            }

            boolean multipleLists = app.getLists().size() >= 2;
            item = menu.findItem(MENU_SWITCH_LIST);
            if (item != null) {
                item.setVisible(multipleLists);
            }
            item = menu.findItem(MENU_MOVE_TO_LIST);
            if (item != null) {
                item.setVisible(multipleLists);
            }

            item = menu.findItem(MENU_REMOVE_FROM_HISTORY);
            if (null != item) {
                if (hasSelection) {
                    item.setTitle(res.getString(R.string.cache_remove_from_history) + " (" + adapter.getChecked() + ")");
                } else {
                    item.setTitle(res.getString(R.string.cache_clear_history));
                }
            }

            item = menu.findItem(MENU_EXPORT_NOTES);
            if (null != item) {
                // Hide Field Notes export if there are no caches with logs
                item.setVisible(false);
                for (cgCache cache : cacheList) {
                    if (cache.isLogOffline()) {
                        item.setVisible(true);
                        if (hasSelection) {
                            item.setTitle(res.getString(R.string.cache_export_fieldnote) + " (" + adapter.getChecked() + ")");
                        } else {
                            item.setTitle(res.getString(R.string.cache_export_fieldnote));
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(Settings.tag, "cgeocaches.onPrepareOptionsMenu: " + e.toString());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case MENU_SWITCH_SELECT_MODE:
                if (adapter != null) {
                    adapter.switchSelectMode();
                }
                invalidateOptionsMenuCompatible();
                return true;
            case MENU_REFRESH_STORED:
                refreshStored();
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
                createList(null);
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
                if (adapter != null) {
                    adapter.invertSelection();
                }
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_SORT_DISTANCE:
                setComparator(item, null);
                return false;
            case MENU_SORT_DIFFICULTY:
                setComparator(item, new DifficultyComparator());
                return false;
            case MENU_SORT_TERRAIN:
                setComparator(item, new TerrainComparator());
                return false;
            case MENU_SORT_SIZE:
                setComparator(item, new SizeComparator());
                return false;
            case MENU_SORT_FAVORITES:
                setComparator(item, new PopularityComparator());
                return false;
            case MENU_SORT_NAME:
                setComparator(item, new NameComparator());
                return false;
            case MENU_SORT_GEOCODE:
                setComparator(item, new GeocodeComparator());
                return false;
            case MENU_SWITCH_LIST:
                selectList(null);
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_SORT_RATING:
                setComparator(item, new RatingComparator());
                return false;
            case MENU_SORT_VOTE:
                setComparator(item, new VoteComparator());
                return false;
            case MENU_SORT_INVENTORY:
                setComparator(item, new InventoryComparator());
                return false;
            case MENU_SORT_DATE:
                setComparator(item, new DateComparator());
                return true;
            case MENU_SORT_FINDS:
                setComparator(item, new FindsComparator(app));
                return true;
            case MENU_SORT_STATE:
                setComparator(item, new StateComparator());
                return true;
            case SUBMENU_FILTER_TYPE:
                showFilterMenu(TypeFilter.getAllFilters(), res.getString(R.string.caches_filter_type_title));
                return true;
            case SUBMENU_FILTER_SIZE:
                showFilterMenu(SizeFilter.getAllFilters(), res.getString(R.string.caches_filter_size_title));
                return true;
            case SUBMENU_FILTER_ATTRIBUTES:
                showFilterMenu(AttributeFilter.getAllFilters(), res.getString(R.string.cache_attributes));
                return true;
            case SUBMENU_FILTER_STATE:
                showFilterMenu(StateFilter.getAllFilters(), res.getString(R.string.cache_status));
                return true;
            case MENU_FILTER_TRACKABLES:
                setFilter(new TrackablesFilter(res.getString(R.string.caches_filter_track)));
                return true;
            case MENU_FILTER_MODIFIED:
                setFilter(new ModifiedFilter(res.getString(R.string.caches_filter_modified)));
                return true;
            case MENU_FILTER_CLEAR:
                if (adapter != null) {
                    setFilter(null);
                }
                return false;
            case MENU_IMPORT_WEB:
                importWeb();
                return false;
            case MENU_EXPORT_NOTES:
                exportFieldNotes();
                return false;
            case MENU_REMOVE_FROM_HISTORY:
                removeFromHistoryCheck();
                invalidateOptionsMenuCompatible();
                return false;
            case MENU_MOVE_TO_LIST:
                moveCachesToOtherList();
                invalidateOptionsMenuCompatible();
                return true;
        }

        return CacheListAppFactory.onMenuItemSelected(item, geo, cacheList, this, search);
    }

    private void showFilterMenu(final IFilter[] filters, final String menuTitle) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(menuTitle);

        final String[] names = new String[filters.length];
        for (int i = 0; i < filters.length; i++) {
            names[i] = filters[i].getName();
        }
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                setFilter(filters[item]);
            }
        });

        builder.create().show();
    }

    private void setComparator(MenuItem item,
            CacheComparator comparator) {
        if (adapter != null) {
            adapter.setComparator(comparator);
        }
        item.setChecked(true);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);

        if (adapter == null) {
            return;
        }

        AdapterContextMenuInfo adapterInfo = null;
        try {
            adapterInfo = (AdapterContextMenuInfo) info;
        } catch (Exception e) {
            Log.w(Settings.tag, "cgeocaches.onCreateContextMenu: " + e.toString());
        }

        if (adapterInfo == null || adapterInfo.position >= adapter.getCount()) {
            return;
        }
        final cgCache cache = adapter.getItem(adapterInfo.position);

        if (StringUtils.isNotBlank(cache.getName())) {
            menu.setHeaderTitle(cache.getName());
        } else {
            menu.setHeaderTitle(cache.getGeocode());
        }

        contextMenuGeocode = cache.getGeocode();

        if (cache.getCoords() != null) {
            menu.add(0, MENU_DEFAULT_NAVIGATION, 0, NavigationAppFactory.getDefaultNavigationApplication(this).getName());
            menu.add(1, MENU_NAVIGATION, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_mapmode);
            addVisitMenu(menu, cache);
            menu.add(0, MENU_CACHE_DETAILS, 0, res.getString(R.string.cache_menu_details));
        }
        if (cache.getListId() >= 1) {
            menu.add(0, MENU_DROP_CACHE, 0, res.getString(R.string.cache_offline_drop));
            final List<StoredList> cacheLists = app.getLists();
            final int listCount = cacheLists.size();
            if (listCount > 1) {
                menu.add(0, MENU_MOVE_TO_LIST, 0, res.getString(R.string.cache_menu_move_list));
            }
        }
    }

    private void moveCachesToOtherList() {
        final List<StoredList> cacheLists = app.getLists();
        ArrayList<String> listNames = new ArrayList<String>();
        for (StoredList list : cacheLists) {
            listNames.add(list.getTitleAndCount());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(res.getString(R.string.cache_menu_move_list));
        builder.setItems(listNames.toArray(new String[listNames.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                moveCachesToList(cacheLists.get(item));
            }
        });
        builder.create().show();
    }

    private void moveCachesToList(final StoredList list) {
        int newListId = list.id;
        final boolean moveAll = adapter.getChecked() == 0;
        for (final cgCache c : Collections.unmodifiableList(cacheList)) {
            if (moveAll || c.isStatusChecked()) {
                app.moveToList(c.getGeocode(), newListId);
            }
        }
        adapter.resetChecks();

        refreshCurrentList();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int id = item.getItemId();
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
            Log.w(Settings.tag, "cgeocaches.onContextItemSelected: " + e.toString());
        }

        if (id == MENU_DEFAULT_NAVIGATION) {
            final cgCache cache = getCacheFromAdapter(adapterInfo);
            NavigationAppFactory.startDefaultNavigationApplication(geo, this, cache, null, null);
            return true;
        } else if (id == MENU_NAVIGATION) {
            final cgCache cache = getCacheFromAdapter(adapterInfo);
            NavigationAppFactory.showNavigationMenu(geo, this, cache, null, null);
            return true;
        } else if (id == MENU_LOG_VISIT) {
            return getCacheFromAdapter(adapterInfo).logVisit(this);
        } else if (id == MENU_CACHE_DETAILS) {
            final Intent cachesIntent = new Intent(this, CacheDetailActivity.class);
            final cgCache cache = getCacheFromAdapter(adapterInfo);
            cachesIntent.putExtra("geocode", cache.getGeocode().toUpperCase());
            cachesIntent.putExtra("name", cache.getName());
            startActivity(cachesIntent);

            return true;
        } else if (id == MENU_DROP_CACHE) {
            cgBase.dropCache(getCacheFromAdapter(adapterInfo), new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    refreshCurrentList();
                }
            });
            return true;
        } else if (id == MENU_MOVE_TO_LIST) {
            final String geocode = getCacheFromAdapter(adapterInfo).getGeocode();
            final List<StoredList> cacheLists = app.getLists();
            ArrayList<String> listNames = new ArrayList<String>();
            for (StoredList list : cacheLists) {
                listNames.add(list.getTitleAndCount());
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(res.getString(R.string.cache_menu_move_list));
            builder.setItems(listNames.toArray(new String[listNames.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    final int newListId = cacheLists.get(item).id;
                    app.moveToList(geocode, newListId);
                    adapter.resetChecks();
                    refreshCurrentList();
                }
            });
            builder.create().show();
            return true;
        }

        // we must remember the menu info for the sub menu, there is a bug
        // in Android:
        // https://code.google.com/p/android/issues/detail?id=7139
        lastMenuInfo = info;

        if (adapterInfo != null) {
            // create a search for a single cache (as if in details view)
            final cgCache cache = getCacheFromAdapter(adapterInfo);

            int logType = id - MENU_LOG_VISIT_OFFLINE;
            cache.logOffline(this, LogType.getById(logType));
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
    private cgCache getCacheFromAdapter(final AdapterContextMenuInfo adapterInfo) {
        final cgCache cache = adapter.getItem(adapterInfo.position);
        if (cache.getGeocode().equalsIgnoreCase(contextMenuGeocode)) {
            return cache;
        }

        return adapter.findCacheByGeocode(contextMenuGeocode);
    }

    private boolean setFilter(IFilter filter) {
        if (adapter != null) {
            adapter.setFilter(filter);
            prepareFilterBar();
            invalidateOptionsMenuCompatible();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (adapter != null) {
                if (adapter.resetChecks()) {
                    return true;
                } else if (adapter.getSelectMode()) {
                    adapter.setSelectMode(false, true);
                    return true;
                }
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
        }
        if (listFooterText == null) {
            listFooterText = (TextView) listFooter.findViewById(R.id.more_caches);
        }

        if (adapter == null) {
            final ListView list = getListView();

            registerForContextMenu(list);
            list.setLongClickable(true);
            list.addFooterView(listFooter);

            adapter = new CacheListAdapter(this, cacheList, type);
            setListAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        adapter.reFilter();

        if (geo != null) {
            adapter.setActualCoordinates(geo.coordsNow);
        }
        if (dir != null) {
            adapter.setActualHeading(dir.directionNow);
        }
    }

    private void setLoadingCaches() {
        if (listFooter == null) {
            return;
        }
        if (listFooterText == null) {
            return;
        }

        listFooterText.setText(res.getString(R.string.caches_more_caches_loading));
        listFooter.setClickable(false);
        listFooter.setOnClickListener(null);
    }

    private void setMoreCaches() {
        if (listFooter == null) {
            return;
        }
        if (listFooterText == null) {
            return;
        }

        boolean enableMore = type != CacheListType.OFFLINE && cacheList != null && cacheList.size() < MAX_LIST_ITEMS;
        if (enableMore && search != null) {
            final int count = search.getTotal();
            enableMore = enableMore && count > 0 && cacheList.size() < count;
        }

        if (enableMore) {
            listFooterText.setText(res.getString(R.string.caches_more_caches) + " (" + res.getString(R.string.caches_more_caches_currently) + ": " + cacheList.size() + ")");
            listFooter.setOnClickListener(new MoreCachesListener());
        } else {
            if (CollectionUtils.isEmpty(cacheList)) {
                listFooterText.setText(res.getString(R.string.caches_no_cache));
            } else {
                listFooterText.setText(res.getString(R.string.caches_more_caches_no));
            }
            listFooter.setOnClickListener(null);
        }
        listFooter.setClickable(enableMore);
    }

    private void init() {
        // sensor & geolocation manager
        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }
        if (Settings.isLiveList() && Settings.isUseCompass() && dir == null) {
            dir = app.startDir(this, dirUpdate);
        }

        if (CollectionUtils.isNotEmpty(cacheList)) {
            setMoreCaches();
        }

        setTitle(title);
        setAdapter();

        if (geo != null) {
            geoUpdate.updateLocation(geo);
        }
        if (dir != null) {
            dirUpdate.updateDirection(dir);
        }
    }

    private void importGpx() {
        cgeogpxes.startSubActivity(this, listId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshCurrentList();
    }

    public void refreshStored() {
        if (adapter != null && adapter.getChecked() > 0) {
            // there are some checked caches
            detailTotal = adapter.getChecked();
        } else {
            // no checked caches, download everything (when already stored - refresh them)
            detailTotal = cacheList.size();
        }
        detailProgress = 0;

        showProgress(false);
        waitDialog = new ProgressDialog(this);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                try {
                    if (threadDetails != null) {
                        threadDetails.kill();
                    }

                    if (geo == null) {
                        geo = app.startGeo(geoUpdate);
                    }
                    if (Settings.isLiveList() && Settings.isUseCompass() && dir == null) {
                        dir = app.startDir(cgeocaches.this, dirUpdate);
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.onOptionsItemSelected.onCancel: " + e.toString());
                }
            }
        });

        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        int etaTime = ((detailTotal * 25) / 60);
        if (etaTime < 1) {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
        } else if (etaTime == 1) {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getString(R.string.caches_eta_min));
        } else {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getString(R.string.caches_eta_mins));
        }
        waitDialog.setCancelable(true);
        waitDialog.setMax(detailTotal);
        waitDialog.show();

        detailProgressTime = System.currentTimeMillis();

        threadDetails = new LoadDetailsThread(loadDetailsHandler, listId);
        threadDetails.start();
    }

    public void removeFromHistoryCheck()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(true);
        dialog.setTitle(res.getString(R.string.caches_removing_from_history));
        dialog.setMessage((adapter != null && adapter.getChecked() > 0) ? res.getString(R.string.cache_remove_from_history)
                : res.getString(R.string.cache_clear_history));
        dialog.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                removeFromHistory();
                dialog.cancel();
            }
        });
        dialog.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }

    public void removeFromHistory()
    {
        if (adapter != null && adapter.getChecked() > 0)
        {
            // there are some checked caches
            detailTotal = adapter.getChecked();
        }
        else
        {
            // no checked caches, remove all
            detailTotal = cacheList.size();
        }
        detailProgress = 0;

        showProgress(false);
        waitDialog = new ProgressDialog(this);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0)
            {
                try
                {
                    if (threadH != null)
                    {
                        threadH.kill();
                    }
                } catch (Exception e)
                {
                    Log.e(Settings.tag, "cgeocaches.removeFromHistory.onCancel: " + e.toString());
                }
            }
        });

        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setMessage(res.getString(R.string.caches_removing_from_history));

        waitDialog.setCancelable(true);
        waitDialog.setMax(detailTotal);
        waitDialog.show();

        threadH = new RemoveFromHistoryThread(removeFromHistoryHandler);
        threadH.start();
    }

    public void exportFieldNotes()
    {
        if (adapter != null && adapter.getChecked() > 0)
        {
            // there are some checked caches
            detailTotal = adapter.getChecked();
        }
        else
        {
            // no checked caches, export all
            detailTotal = cacheList.size();
        }
        detailProgress = 0;

        showProgress(false);
        waitDialog = new ProgressDialog(this);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0)
            {
                try
                {
                    if (threadF != null)
                    {
                        threadF.kill();
                    }
                } catch (Exception e)
                {
                    Log.e(Settings.tag, "cgeocaches.exportFieldNotes.onCancel: " + e.toString());
                }
            }
        });

        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setMessage(res.getString(R.string.caches_exporting_fieldnote));

        waitDialog.setCancelable(true);
        waitDialog.setMax(detailTotal);
        waitDialog.show();

        threadF = new ExportFieldNotesThread(exportFieldNotesHandler);
        threadF.start();
    }

    public void importWeb() {
        detailProgress = 0;

        showProgress(false);
        waitDialog = new ProgressDialog(this);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                try {
                    if (threadWeb != null) {
                        threadWeb.kill();
                    }

                    if (geo == null) {
                        geo = app.startGeo(geoUpdate);
                    }
                    if (Settings.isLiveList() && Settings.isUseCompass() && dir == null) {
                        dir = app.startDir(cgeocaches.this, dirUpdate);
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.importWeb.onCancel: " + e.toString());
                }
            }
        });

        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.setMessage(res.getString(R.string.web_import_waiting));
        waitDialog.setCancelable(true);
        waitDialog.show();

        threadWeb = new LoadFromWebThread(downloadFromWebHandler, listId);
        threadWeb.start();
    }

    public void dropStored(final boolean removeListAfterwards) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(true);
        dialog.setTitle(res.getString(R.string.caches_drop_stored));

        if (adapter != null && adapter.getChecked() > 0) {
            dialog.setMessage(res.getString(R.string.caches_drop_selected_ask));
        } else {
            dialog.setMessage(res.getString(R.string.caches_drop_all_ask));
        }
        dialog.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dropSelected();
                if (removeListAfterwards) {
                    removeList(false);
                }
                dialog.cancel();
            }
        });
        dialog.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }

    public void dropSelected() {
        waitDialog = new ProgressDialog(this);
        waitDialog.setMessage(res.getString(R.string.caches_drop_progress));
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                try {
                    if (threadR != null) {
                        threadR.kill();
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.onOptionsItemSelected.onCancel: " + e.toString());
                }
            }
        });

        waitDialog.setCancelable(true);
        waitDialog.show();

        threadR = new DropDetailsThread(dropDetailsHandler);
        threadR.start();
    }

    private class UpdateLocation implements UpdateLocationCallback {

        @Override
        public void updateLocation(cgGeo geo) {
            if (geo == null) {
                return;
            }
            if (adapter == null) {
                return;
            }

            try {
                if (cacheList != null && geo.coordsNow != null) {
                    adapter.setActualCoordinates(geo.coordsNow);
                }

                if (!Settings.isUseCompass() || geo.speedNow > 5) { // use GPS when speed is higher than 18 km/h
                    if (!Settings.isUseCompass()) {
                        adapter.setActualHeading(geo.bearingNow);
                    }
                    if (northHeading != null) {
                        adapter.setActualHeading(northHeading);
                    }
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to UpdateLocation location.");
            }
        }
    }

    private class UpdateDirection implements UpdateDirectionCallback {

        @Override
        public void updateDirection(cgDirection dir) {
            if (!Settings.isLiveList()) {
                return;
            }
            if (dir == null || dir.directionNow == null) {
                return;
            }

            northHeading = dir.directionNow;
            if (northHeading != null && adapter != null && (geo == null || geo.speedNow <= 5)) { // use compass when speed is lower than 18 km/h) {
                adapter.setActualHeading(northHeading);
            }
        }
    }

    private class LoadByOfflineThread extends Thread {

        final private Handler handler;
        final private Geopoint coords;
        final private int listId;

        public LoadByOfflineThread(final Handler handlerIn, final Geopoint coordsIn, int listIdIn) {
            handler = handlerIn;
            coords = coordsIn;
            listId = listIdIn;
        }

        @Override
        public void run() {
            search = cgBase.searchByStored(coords, Settings.getCacheType(), listId);
            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadByHistoryThread extends Thread {

        final private Handler handler;

        public LoadByHistoryThread(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            search = cgeoapplication.getInstance().getHistoryOfCaches(true, coords != null ? Settings.getCacheType() : CacheType.ALL);
            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadNextPageThread extends cgSearchThread {

        private final Handler handler;

        public LoadNextPageThread(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            search = cgBase.searchByNextPage(this, search, Settings.isShowCaptcha());

            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadByCoordsThread extends cgSearchThread {

        final private Handler handler;
        final private Geopoint coords;

        public LoadByCoordsThread(final Handler handler, final Geopoint coords) {
            setPriority(Thread.MIN_PRIORITY);

            this.handler = handler;
            this.coords = coords;

            if (coords == null) {
                showToast(res.getString(R.string.warn_no_coordinates));

                finish();
                return;
            }
        }

        @Override
        public void run() {
            search = cgBase.searchByCoords(this, coords, cacheType, Settings.isShowCaptcha());

            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadByKeywordThread extends cgSearchThread {

        final private Handler handler;
        final private String keyword;

        public LoadByKeywordThread(final Handler handler, final String keyword) {
            setPriority(Thread.MIN_PRIORITY);

            this.handler = handler;
            this.keyword = keyword;

            if (keyword == null) {
                showToast(res.getString(R.string.warn_no_keyword));

                finish();
                return;
            }
        }

        @Override
        public void run() {
            search = cgBase.searchByKeyword(this, keyword, cacheType, Settings.isShowCaptcha());
            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadByUserNameThread extends cgSearchThread {

        final private Handler handler;
        final private String username;

        public LoadByUserNameThread(final Handler handler, final String username) {
            setPriority(Thread.MIN_PRIORITY);

            this.handler = handler;
            this.username = username;

            if (StringUtils.isBlank(username)) {
                showToast(res.getString(R.string.warn_no_username));

                finish();
                return;
            }
        }

        @Override
        public void run() {
            search = cgBase.searchByUsername(this, username, cacheType, Settings.isShowCaptcha());
            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadByOwnerThread extends cgSearchThread {

        final private Handler handler;
        final private String username;

        public LoadByOwnerThread(final Handler handler, final String username) {
            setPriority(Thread.MIN_PRIORITY);

            this.handler = handler;
            this.username = username;

            if (StringUtils.isBlank(username)) {
                showToast(res.getString(R.string.warn_no_username));

                finish();
                return;
            }
        }

        @Override
        public void run() {
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", username);
            if (cacheType != null) {
                params.put("cacheType", cacheType.id);
            }

            search = cgBase.searchByOwner(this, username, cacheType, Settings.isShowCaptcha());

            handler.sendMessage(Message.obtain());
        }
    }

    private class LoadDetailsThread extends Thread {

        final private Handler handler;
        final private int listIdLD;
        private volatile boolean needToStop = false;
        private int checked = 0;
        private long last = 0L;

        public LoadDetailsThread(Handler handlerIn, int listId) {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;

            // in case of online lists, set the list id to the standard list
            this.listIdLD = Math.max(listId, StoredList.STANDARD_LIST_ID);

            if (adapter != null) {
                checked = adapter.getChecked();
            }
        }

        public void kill() {
            needToStop = true;
        }

        @Override
        public void run() {
            if (dir != null) {
                dir = app.removeDir();
            }
            if (geo != null) {
                geo = app.removeGeo();
            }

            final List<cgCache> cacheListTemp = new ArrayList<cgCache>(cacheList);
            for (cgCache cache : cacheListTemp) {
                if (checked > 0 && !cache.isStatusChecked()) {
                    handler.sendEmptyMessage(0);

                    yield();
                    continue;
                }

                try {
                    if (needToStop) {
                        Log.i(Settings.tag, "Stopped storing process.");
                        break;
                    }

                    if ((System.currentTimeMillis() - last) < 1500) {
                        try {
                            int delay = 1000 + ((Double) (Math.random() * 1000)).intValue() - (int) (System.currentTimeMillis() - last);
                            if (delay < 0) {
                                delay = 500;
                            }

                            Log.i(Settings.tag, "Waiting for next cache " + delay + " ms");
                            sleep(delay);
                        } catch (Exception e) {
                            Log.e(Settings.tag, "cgeocaches.LoadDetailsThread.sleep: " + e.toString());
                        }
                    }

                    if (needToStop) {
                        Log.i(Settings.tag, "Stopped storing process.");
                        break;
                    }

                    detailProgress++;
                    cgBase.storeCache(cgeocaches.this, cache, null, listIdLD, null);

                    handler.sendEmptyMessage(cacheList.indexOf(cache));

                    yield();
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.LoadDetailsThread: " + e.toString());
                }

                last = System.currentTimeMillis();
            }
            cacheListTemp.clear();

            handler.sendEmptyMessage(-1);
        }
    }

    private class LoadFromWebThread extends Thread {

        final private Handler handler;
        final private int listIdLFW;
        private volatile boolean needToStop = false;

        public LoadFromWebThread(Handler handlerIn, int listId) {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;
            listIdLFW = listId;
        }

        public void kill() {
            needToStop = true;
        }

        @Override
        public void run() {
            if (dir != null) {
                dir = app.removeDir();
            }
            if (geo != null) {
                geo = app.removeGeo();
            }

            int delay = -1;
            int times = 0;

            while (times < 3 * 60 / 5) // maximum: 3 minutes, every 5 seconds
            {
                if (needToStop)
                {
                    handler.sendEmptyMessage(-1);
                    break;
                }

                //download new code
                String deviceCode = Settings.getWebDeviceCode();
                if (deviceCode == null) {
                    deviceCode = "";
                }
                final Parameters params = new Parameters("code", deviceCode);
                HttpResponse responseFromWeb = cgBase.request("http://send2.cgeo.org/read.html", params, true);

                if (responseFromWeb != null && responseFromWeb.getStatusLine().getStatusCode() == 200) {
                    final String response = cgBase.getResponseData(responseFromWeb);
                    if (response.length() > 2) {

                        String GCcode = response;

                        delay = 1;
                        Message mes = Message.obtain();
                        mes.what = 1;
                        mes.obj = GCcode;
                        handler.sendMessage(mes);
                        yield();

                        cgBase.storeCache(cgeocaches.this, null, GCcode, listIdLFW, null);

                        Message mes1 = Message.obtain();
                        mes1.what = 2;
                        mes1.obj = GCcode;
                        handler.sendMessage(mes1);
                        yield();
                    } else if ("RG".equals(cgBase.getResponseData(responseFromWeb))) {
                        //Server returned RG (registration) and this device no longer registered.
                        Settings.setWebNameCode(null, null);
                        needToStop = true;
                        handler.sendEmptyMessage(-3);
                        return;
                    } else {
                        delay = 0;
                        handler.sendEmptyMessage(0);
                        yield();
                    }
                }
                if (responseFromWeb == null || responseFromWeb.getStatusLine().getStatusCode() != 200) {
                    needToStop = true;
                    handler.sendEmptyMessage(-2);
                    return;
                }

                try {
                    yield();
                    if (delay == 0)
                    {
                        sleep(5000); //No caches 5s
                        times++;
                    } else {
                        sleep(500); //Cache was loaded 0.5s
                        times = 0;
                    }
                } catch (InterruptedException e) {
                    Log.e(Settings.tag, "cgeocaches.LoadFromWebThread.sleep: " + e.toString());
                }
            }
            handler.sendEmptyMessage(-1);
        }
    }

    private class DropDetailsThread extends Thread {

        final private Handler handler;
        private volatile boolean needToStop = false;
        private int checked = 0;

        public DropDetailsThread(Handler handlerIn) {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;

            if (adapter != null) {
                checked = adapter.getChecked();
            }
        }

        public void kill() {
            needToStop = true;
        }

        @Override
        public void run() {
            if (dir != null) {
                dir = app.removeDir();
            }
            if (geo != null) {
                geo = app.removeGeo();
            }

            final List<cgCache> cacheListTemp = new ArrayList<cgCache>(cacheList);
            for (cgCache cache : cacheListTemp) {
                if (checked > 0 && !cache.isStatusChecked()) {
                    continue;
                }

                try {
                    if (needToStop) {
                        Log.i(Settings.tag, "Stopped dropping process.");
                        break;
                    }

                    app.markDropped(cache.getGeocode());
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.DropDetailsThread: " + e.toString());
                }
            }
            cacheListTemp.clear();

            handler.sendEmptyMessage(-1);
        }
    }

    private class RemoveFromHistoryThread extends Thread {

        final private Handler handler;
        private volatile boolean needToStop = false;
        private int checked = 0;

        public RemoveFromHistoryThread(Handler handlerIn) {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;

            if (adapter != null) {
                checked = adapter.getChecked();
            }
        }

        public void kill() {
            needToStop = true;
        }

        @Override
        public void run() {
            for (cgCache cache : cacheList) {
                if (checked > 0 && !cache.isStatusChecked()) {
                    handler.sendEmptyMessage(0);

                    yield();
                    continue;
                }

                try {
                    if (needToStop) {
                        Log.i(Settings.tag, "Stopped removing process.");
                        break;
                    }

                    app.clearVisitDate(cache.getGeocode());

                    detailProgress++;
                    handler.sendEmptyMessage(cacheList.indexOf(cache));

                    yield();
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.RemoveFromHistoryThread: " + e.toString());
                }
            }

            handler.sendEmptyMessage(-1);
        }
    }

    private class ExportFieldNotesThread extends Thread
    {
        private final Handler handler;
        private volatile boolean needToStop = false;
        private int checked = 0;

        public ExportFieldNotesThread(Handler handlerIn)
        {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;

            if (adapter != null)
            {
                checked = adapter.getChecked();
            }
        }

        public void kill()
        {
            needToStop = true;
        }

        @Override
        public void run()
        {
            SimpleDateFormat fieldNoteDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            StringBuffer fieldNoteBuffer = new StringBuffer(500);

            // We need our own HashMap because LogType will give us localized and maybe
            // different strings than gc.com expects in the field note
            // We only need such logtypes that are possible to log via c:geo
            Map<LogType, String> logTypes = new HashMap<LogType, String>();
            logTypes.put(LogType.LOG_FOUND_IT, "Found it");
            logTypes.put(LogType.LOG_DIDNT_FIND_IT, "Didn't find it");
            logTypes.put(LogType.LOG_NOTE, "Write Note");
            logTypes.put(LogType.LOG_NEEDS_ARCHIVE, "Needs archived");
            logTypes.put(LogType.LOG_NEEDS_MAINTENANCE, "Needs Maintenance");
            logTypes.put(LogType.LOG_WILL_ATTEND, "Will Attend");
            logTypes.put(LogType.LOG_ATTENDED, "Attended");
            logTypes.put(LogType.LOG_WEBCAM_PHOTO_TAKEN, "Webcam Photo Taken");

            for (cgCache cache : cacheList) {
                if (checked > 0 && !cache.isStatusChecked()) {
                    handler.sendEmptyMessage(0);

                    yield();
                    continue;
                }

                try {
                    if (needToStop)
                    {
                        Log.i(Settings.tag, "Stopped exporting process.");
                        break;
                    }

                    if (cache.isLogOffline())
                    {
                        cgLog log = app.loadLogOffline(cache.getGeocode());

                        if (null != logTypes.get(log.type))
                        {
                            fieldNoteBuffer.append(cache.getGeocode())
                                    .append(',')
                                    .append(fieldNoteDateFormat.format(new Date(log.date)))
                                    .append(',')
                                    .append(logTypes.get(log.type))
                                    .append(",\"")
                                    .append(StringUtils.replaceChars(log.log, '"', '\''))
                                    .append("\"\n");
                        }
                    }

                    detailProgress++;

                    handler.sendEmptyMessage(cacheList.indexOf(cache));

                    yield();
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.ExportFieldNotesThread: " + e.toString());
                }
            }

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/field-notes");
                exportLocation.mkdirs();

                SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                File exportFile = new File(exportLocation + "/" + fileNameDateFormat.format(new Date()) + ".txt");

                OutputStream os = null;
                Writer fw = null;
                try
                {
                    os = new FileOutputStream(exportFile);
                    fw = new OutputStreamWriter(os, "ISO-8859-1"); // TODO: gc.com doesn't support UTF-8
                    fw.write(fieldNoteBuffer.toString());

                    Message.obtain(handler, -2, exportFile).sendToTarget();
                } catch (IOException e) {
                    Log.e(Settings.tag, "cgeocaches.ExportFieldNotesThread: " + e.toString());
                    handler.sendEmptyMessage(-3);
                } finally
                {
                    if (fw != null)
                    {
                        try {
                            fw.close();
                        } catch (IOException e) {
                            Log.e(Settings.tag, "cgeocaches.ExportFieldNotesThread: " + e.toString());
                        }
                    }
                }
            }

            handler.sendEmptyMessage(-1);
        }
    }

    private class MoreCachesListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            showProgress(true);
            setLoadingCaches();
            listFooter.setOnClickListener(null);

            LoadNextPageThread thread;
            thread = new LoadNextPageThread(loadNextPageHandler);
            thread.setRecaptchaHandler(new cgSearchHandler(cgeocaches.this, res, thread));
            thread.start();
        }
    }

    private void hideLoading() {
        final ListView list = getListView();
        final RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);

        if (list.getVisibility() == View.GONE) {
            list.setVisibility(View.VISIBLE);
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

        lists = app.getLists();

        if (lists == null) {
            return;
        }

        final List<CharSequence> listsTitle = new ArrayList<CharSequence>();
        for (StoredList list : lists) {
            listsTitle.add(list.getTitleAndCount());
        }
        listsTitle.add("<" + res.getString(R.string.list_menu_create) + ">");

        final CharSequence[] items = new CharSequence[listsTitle.size()];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(res.getString(R.string.list_title));
        builder.setItems(listsTitle.toArray(items), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int itemId) {
                if (itemId >= lists.size()) {
                    // create new list on the fly
                    createList(new RunnableWithArgument<Integer>() {

                        @Override
                        public void run(final Integer arg) {
                            switchListById(arg.intValue());
                        }
                    });
                }
                else {
                    switchListById(lists.get(itemId).id);
                }
            }
        });
        builder.create().show();
    }

    public void switchListById(int id) {
        StoredList list = null;

        if (id >= 0) {
            list = app.getList(id);
        } else {
            return;
        }

        listId = list.id;
        title = list.title;

        Settings.saveLastList(listId);

        showProgress(true);
        setLoadingCaches();

        (new MoveCachesToListThread(listId, new MoveHandler())).start();
        invalidateOptionsMenuCompatible();
    }

    private class MoveHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Thread threadPure = new LoadByOfflineThread(loadCachesHandler, coords, msg.what);
            threadPure.start();
        }
    }

    private class MoveCachesToListThread extends Thread {
        final private int listId;
        final private Handler handler;

        public MoveCachesToListThread(int listIdIn, Handler handlerIn) {
            listId = listIdIn;
            handler = handlerIn;
        }

        @Override
        public void run() {
            int checked = adapter.getChecked();
            if (checked > 0) {
                final List<cgCache> cacheListTemp = new ArrayList<cgCache>(cacheList);
                for (cgCache cache : cacheListTemp) {
                    if (cache.isStatusChecked()) {
                        app.moveToList(cache.getGeocode(), listId);
                    }
                }
            }

            handler.sendEmptyMessage(listId);
        }
    }

    private void handleListNameInput(final String defaultValue, int dialogTitle, int buttonTitle, final RunnableWithArgument<String> runnable) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final View view = inflater.inflate(R.layout.list_create_dialog, null);
        final EditText input = (EditText) view.findViewById(R.id.text);
        input.setText(defaultValue);

        alert.setTitle(dialogTitle);
        alert.setView(view);
        alert.setPositiveButton(buttonTitle, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // remove whitespaces added by autocompletion of Android keyboard
                String listName = StringUtils.trim(input.getText().toString());
                if (StringUtils.isNotBlank(listName)) {
                    runnable.run(listName);
                }
            }
        });
        alert.setNegativeButton(res.getString(R.string.list_dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        alert.show();
    }

    private void createList(final RunnableWithArgument<Integer> runAfterwards) {
        handleListNameInput("", R.string.list_dialog_create_title, R.string.list_dialog_create, new RunnableWithArgument<String>() {

            @Override
            public void run(final String listName) {
                final int newId = app.createList(listName);

                if (newId >= 10) {
                    showToast(res.getString(R.string.list_dialog_create_ok));
                    if (runAfterwards != null) {
                        runAfterwards.run(newId);
                    }
                } else {
                    showToast(res.getString(R.string.list_dialog_create_err));
                }
            }
        });
    }

    private void renameList() {
        final StoredList list = app.getList(listId);
        handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, new RunnableWithArgument<String>() {

            @Override
            public void run(final String listName) {
                app.renameList(listId, listName);
                refreshCurrentList();
            }
        });
    }

    private void removeListInternal() {
        boolean status = app.removeList(listId);

        if (status) {
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
            public void onClick(DialogInterface dialog, int whichButton) {
                removeListInternal();
            }
        });
        alert.setNegativeButton(res.getString(R.string.list_dialog_cancel), new DialogInterface.OnClickListener() {
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

        SearchResult searchToUse = search;

        // apply filter settings (if there's a filter)
        if (adapter != null) {
            Set<String> geocodes = new HashSet<String>();
            for (cgCache cache : adapter.getFilteredList()) {
                geocodes.add(cache.getGeocode());
            }
            searchToUse = new SearchResult(geocodes);
        }

        int count = searchToUse.getCount();
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
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.OFFLINE);
        context.startActivity(cachesIntent);
    }

    public static void startActivityCachesAround(final AbstractActivity context, final Geopoint coords) {
        cgeocaches cachesActivity = new cgeocaches();

        Intent cachesIntent = new Intent(context, cachesActivity.getClass());
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.COORDINATE);
        cachesIntent.putExtra("latitude", coords.getLatitude());
        cachesIntent.putExtra("longitude", coords.getLongitude());

        context.startActivity(cachesIntent);
    }

    public static void startActivityOwner(final AbstractActivity context, final String userName) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);

        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.OWNER);
        cachesIntent.putExtra("username", userName);

        context.startActivity(cachesIntent);
    }

    public static void startActivityUserName(final AbstractActivity context, final String userName) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);

        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.USERNAME);
        cachesIntent.putExtra("username", userName);

        context.startActivity(cachesIntent);
    }

    private void prepareFilterBar() {
        if (Settings.getCacheType() != CacheType.ALL || adapter.isFilter()) {
            String filter = "";
            String cacheType = Settings.getCacheType().getL10n();

            if (adapter.isFilter()) {
                filter = ", " + adapter.getFilterName();
            }

            ((TextView) findViewById(R.id.filter_text)).setText(cacheType + filter);
            findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.filter_bar).setVisibility(View.GONE);
        }
    }

    /**
     * set date comparator for pure event lists
     */
    private void setDateComparatorForEventList() {
        if (CollectionUtils.isNotEmpty(cacheList)) {
            boolean eventsOnly = true;
            for (cgCache cache : cacheList) {
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

    public static void startActivityNearest(final Context context, final Geopoint coordsNow) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.NEAREST);
        cachesIntent.putExtra("latitude", coordsNow.getLatitude());
        cachesIntent.putExtra("longitude", coordsNow.getLongitude());
        context.startActivity(cachesIntent);
    }

    public static void startActivityHistory(Context context) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.HISTORY);
        context.startActivity(cachesIntent);
    }

    public static void startActivityAddress(Context context, double latitude, double longitude, String address) {
        Intent addressIntent = new Intent(context, cgeocaches.class);
        addressIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.ADDRESS);
        addressIntent.putExtra("latitude", latitude);
        addressIntent.putExtra("longitude", longitude);
        addressIntent.putExtra("address", address);
        context.startActivity(addressIntent);
    }

    public static void startActivityCoordinates(final Context context, double latitude, double longitude) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.COORDINATE);
        cachesIntent.putExtra("latitude", latitude);
        cachesIntent.putExtra("longitude", longitude);
        context.startActivity(cachesIntent);
    }

    public static void startActivityKeyword(final Context context, final String keyword) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.KEYWORD);
        cachesIntent.putExtra("keyword", keyword);
        context.startActivity(cachesIntent);
    }

    public static void startActivityMap(final Context context, final SearchResult search) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.MAP);
        cachesIntent.putExtra("search", search);
        context.startActivity(cachesIntent);
    }

}
