package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cachelist.CacheListAppFactory;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.export.ExportFactory;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.filter.FilterUserInterface;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
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
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class cgeocaches extends AbstractListActivity implements IObserver<IGeoData> {

    private static final int MAX_LIST_ITEMS = 1000;
    private static final String EXTRAS_LIST_TYPE = "type";
    private static final String EXTRAS_COORDS = "coords";
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
    private static final int MENU_EXPORT = 22;
    private static final int MENU_REMOVE_FROM_HISTORY = 23;
    private static final int MENU_DROP_CACHE = 24;
    private static final int MENU_MOVE_TO_LIST = 25;
    private static final int MENU_SWITCH_SELECT_MODE = 52;
    private static final int SUBMENU_SHOW_MAP = 54;
    private static final int SUBMENU_MANAGE_LISTS = 55;
    private static final int SUBMENU_MANAGE_OFFLINE = 56;
    private static final int SUBMENU_SORT = 57;
    private static final int SUBMENU_MANAGE_HISTORY = 60;
    private static final int MENU_SORT_DATE = 61;
    private static final int MENU_SORT_FINDS = 62;
    private static final int MENU_SORT_STATE = 63;
    private static final int MENU_RENAME_LIST = 64;
    private static final int MENU_DROP_CACHES_AND_LIST = 65;
    private static final int MENU_DEFAULT_NAVIGATION = 66;
    private static final int MENU_NAVIGATION = 69;
    private static final int MENU_STORE_CACHE = 73;
    private static final int MENU_FILTER = 74;

    private static final int MSG_DONE = -1;
    private static final int MSG_RESTART_GEO_AND_DIR = -2;
    private static final int MSG_CANCEL = -99;

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
    private Progress progress = new Progress();
    private Float northHeading = 0f;
    private cgDirection dir = null;
    private UpdateDirectionCallback dirUpdate = new UpdateDirection();
    private String title = "";
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    private LoadDetailsThread threadDetails = null;
    private LoadFromWebThread threadWeb = null;
    private RemoveFromHistoryThread threadH = null;
    private int listId = StoredList.TEMPORARY_LIST_ID;
    private GeocodeComparator gcComparator = new GeocodeComparator();
    private Handler loadCachesHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (search != null) {
                    setTitle(title + " [" + search.getCount() + ']');
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
                            Cookies.clearCookies();
                            dialog.cancel();
                        }
                    });
                    dialog.setPositiveButton(res.getString(R.string.license_show), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            Cookies.clearCookies();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/software/agreement.aspx?ID=0")));
                        }
                    });

                    AlertDialog alert = dialog.create();
                    alert.show();
                } else if (app != null && search != null && search.getError() != null) {
                    showToast(res.getString(R.string.err_download_fail) + ' ' + search.getError().getErrorString(res) + '.');

                    hideLoading();
                    showProgress(false);

                    finish();
                    return;
                }

                final Geopoint coordsNow = app.currentGeo().getCoords();
                if (coordsNow != null) {
                    adapter.setActualCoordinates(coordsNow);
                    adapter.setActualHeading(northHeading);
                }
            } catch (Exception e) {
                showToast(res.getString(R.string.err_detail_cache_find_any));
                Log.e("cgeocaches.loadCachesHandler: " + e.toString());

                hideLoading();
                showProgress(false);

                finish();
                return;
            }

            try {
                hideLoading();
                showProgress(false);
            } catch (Exception e2) {
                Log.e("cgeocaches.loadCachesHandler.2: " + e2.toString());
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

                final Geopoint coordsNow = app.currentGeo().getCoords();
                if (coordsNow != null) {
                    adapter.setActualCoordinates(coordsNow);
                    adapter.setActualHeading(northHeading);
                }
            } catch (Exception e) {
                showToast(res.getString(R.string.err_detail_cache_find_next));
                Log.e("cgeocaches.loadNextPageHandler: " + e.toString());
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
                cacheList.get(msg.what).setStatusChecked(false);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                int minutesRemaining = ((detailTotal - detailProgress) * secondsElapsed / ((detailProgress > 0) ? detailProgress : 1) / 60);

                progress.setProgress(detailProgress);
                if (minutesRemaining < 1) {
                    progress.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                } else if (minutesRemaining == 1) {
                    progress.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getString(R.string.caches_eta_min));
                } else {
                    progress.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getString(R.string.caches_eta_mins));
                }
            } else if (msg.what == MSG_CANCEL) {
                if (threadDetails != null) {
                    threadDetails.kill();
                }
            } else if (msg.what == MSG_RESTART_GEO_AND_DIR) {
                startGeoAndDir();
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

                final Geopoint coordsNow = app.currentGeo().getCoords();
                if (coordsNow != null) {
                    adapter.setActualCoordinates(coordsNow);
                    adapter.setActualHeading(northHeading);
                }

                showProgress(false);
                progress.dismiss();

                startGeoAndDir();
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
                progress.setMessage(res.getString(R.string.web_import_waiting));
            } else if (msg.what == 1) { //cache downloading
                progress.setMessage(res.getString(R.string.web_downloading) + " " + (String) msg.obj + "...");
            } else if (msg.what == 2) { //Cache downloaded
                progress.setMessage(res.getString(R.string.web_downloaded) + " " + (String) msg.obj + ".");
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

                progress.dismiss();
            }
        }
    };
    private Handler dropDetailsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_CANCEL) {
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

                progress.dismiss();
            }
        }
    };
    private Handler removeFromHistoryHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setAdapter();

            if (msg.what > -1) {
                cacheList.get(msg.what).setStatusChecked(false);
                progress.setProgress(detailProgress);
            } else if (msg.what == MSG_CANCEL) {
                if (threadH != null) {
                    threadH.kill();
                }
            } else {
                if (adapter != null) {
                    adapter.setSelectMode(false, true);
                }

                // reload history list
                (new LoadByHistoryThread(loadCachesHandler)).start();

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
        if (CollectionUtils.isNotEmpty(cacheList)) {
            setMoreCaches();
        }

        setTitle(title);
        setAdapter();

        app.setAction(action);

        setTheme();
        setContentView(R.layout.caches);
        setTitle("caches");

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Object typeObject = extras.get(EXTRAS_LIST_TYPE);
            type = (typeObject instanceof CacheListType) ? (CacheListType) typeObject : CacheListType.OFFLINE;
            coords = (Geopoint) extras.getParcelable(EXTRAS_COORDS);
            cacheType = Settings.getCacheType();
            keyword = extras.getString("keyword");
            address = extras.getString("address");
            username = extras.getString("username");
        }
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            type = CacheListType.OFFLINE;
            if (coords == null) {
                coords = new Geopoint(0.0, 0.0);
            }
        }

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
                Log.e("cgeocaches.onCreate: No action or unknown action specified");
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
    public void onResume() {
        super.onResume();

        init();

        if (adapter != null) {
            adapter.setSelectMode(false, true);
            final Geopoint coordsNow = app.currentGeo().getCoords();
            if (coordsNow != null) {
                adapter.setActualCoordinates(coordsNow);
                adapter.setActualHeading(northHeading);
                adapter.forceSort(coordsNow);
            }
        }

        if (loadCachesHandler != null && search != null) {
            loadCachesHandler.sendEmptyMessage(0);
        }

        // refresh standard list if it has changed (new caches downloaded)
        if (type == CacheListType.OFFLINE && listId >= StoredList.STANDARD_LIST_ID && search != null) {
            SearchResult newSearch = cgeoapplication.getInstance().getBatchOfStoredCaches(true, coords, cacheType, listId);
            if (newSearch != null && newSearch.getTotal() != search.getTotal()) {
                refreshCurrentList();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (adapter != null) {
            adapter = null;
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        removeGeoAndDir();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FILTER, 0, res.getString(R.string.caches_filter)).setIcon(R.drawable.ic_menu_filter);

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
            subMenuSort.add(1, id, 0, label).setCheckable(true).setChecked(id == MENU_SORT_DISTANCE);
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

            //TODO: add submenu/AlertDialog and use R.string.gpx_import_title
            subMenu.add(0, MENU_IMPORT_GPX, 0, res.getString(R.string.gpx_import_title));
            if (Settings.getWebDeviceCode() != null) {
                subMenu.add(0, MENU_IMPORT_WEB, 0, res.getString(R.string.web_import_title));
            }

            subMenu.add(0, MENU_EXPORT, 0, res.getString(R.string.export)); // export caches
        } else {
            if (type == CacheListType.HISTORY) {
                SubMenu subMenu = menu.addSubMenu(0, SUBMENU_MANAGE_HISTORY, 0, res.getString(R.string.caches_manage)).setIcon(android.R.drawable.ic_menu_save);
                subMenu.add(0, MENU_REMOVE_FROM_HISTORY, 0, res.getString(R.string.cache_clear_history)); // remove from history
                subMenu.add(0, MENU_EXPORT, 0, res.getString(R.string.export)); // export caches
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
                setMenuItemLabel(menu, MENU_DROP_CACHES, R.string.caches_drop_selected, R.string.caches_drop_all);
                menu.findItem(MENU_DROP_CACHES_AND_LIST).setVisible(!hasSelection && isNonDefaultList);
                setMenuItemLabel(menu, MENU_REFRESH_STORED, R.string.caches_refresh_selected, R.string.caches_refresh_all);
                setMenuItemLabel(menu, MENU_MOVE_TO_LIST, R.string.caches_move_selected, R.string.caches_move_all);
            } else { // search and history list (all other than offline)
                setMenuItemLabel(menu, MENU_REFRESH_STORED, R.string.caches_store_selected, R.string.caches_store_offline);
            }

            // Hide menus if cache-list is empty
            int[] hideIfEmptyList = new int[] {
                    MENU_SWITCH_SELECT_MODE,
                    SUBMENU_MANAGE_HISTORY,
                    SUBMENU_SHOW_MAP,
                    SUBMENU_SORT,
                    MENU_REFRESH_STORED,
                    MENU_DROP_CACHES,
                    MENU_DROP_CACHES_AND_LIST,
                    MENU_MOVE_TO_LIST,
                    MENU_EXPORT,
                    MENU_REMOVE_FROM_HISTORY
            };

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
                item.setVisible(multipleLists && cacheList.size() > 0);
            }

            setMenuItemLabel(menu, MENU_REMOVE_FROM_HISTORY, R.string.cache_remove_from_history, R.string.cache_clear_history);
            setMenuItemLabel(menu, MENU_EXPORT, R.string.export, R.string.export);
        } catch (Exception e) {
            Log.e("cgeocaches.onPrepareOptionsMenu", e);
        }

        return true;
    }

    private void setMenuItemLabel(final Menu menu, final int menuId, final int resIdSelection, final int resId) {
        final MenuItem menuItem = menu.findItem(menuId);
        if (menuItem == null) {
            return;
        }
        boolean hasSelection = adapter != null && adapter.getChecked() > 0;
        if (hasSelection) {
            menuItem.setTitle(res.getString(resIdSelection) + " (" + adapter.getChecked() + ")");
        } else {
            menuItem.setTitle(res.getString(resId));
        }
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
            case MENU_FILTER:
                new FilterUserInterface(this).selectFilter(new RunnableWithArgument<IFilter>() {
                    @Override
                    public void run(IFilter selectedFilter) {
                        if (selectedFilter != null) {
                            setFilter(selectedFilter);
                        }
                        else {
                            // clear filter
                            if (adapter != null) {
                                setFilter(null);
                            }
                        }
                    }
                });
                return true;
            case MENU_IMPORT_WEB:
                importWeb();
                return false;
            case MENU_EXPORT:
                exportCaches();
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

        return CacheListAppFactory.onMenuItemSelected(item, app.currentGeo(), cacheList, this, search);
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
            Log.w("cgeocaches.onCreateContextMenu: " + e.toString());
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
        else {
            menu.add(0, MENU_STORE_CACHE, 0, res.getString(R.string.cache_offline_store));
        }
    }

    private void moveCachesToOtherList() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.cache_menu_move_list, new RunnableWithArgument<Integer>() {

            @Override
            public void run(Integer newListId) {
                List<cgCache> selected;
                final boolean moveAll = adapter.getChecked() == 0;
                if (moveAll) {
                    selected = new ArrayList<cgCache>(cacheList);
                } else {
                    selected = adapter.getCheckedCaches();
                }
                app.moveToList(selected, newListId);
                adapter.resetChecks();

                refreshCurrentList();
            }
        });
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
            Log.w("cgeocaches.onContextItemSelected: " + e.toString());
        }

        final cgCache cache = adapterInfo != null ? getCacheFromAdapter(adapterInfo) : null;

        final int id = item.getItemId();
        switch (id) {
            case MENU_DEFAULT_NAVIGATION:
                NavigationAppFactory.startDefaultNavigationApplication(app.currentGeo(), this, cache, null, null);
                break;
            case MENU_NAVIGATION:
                NavigationAppFactory.showNavigationMenu(app.currentGeo(), this, cache, null, null);
                break;
            case MENU_LOG_VISIT:
                cache.logVisit(this);
                break;
            case MENU_CACHE_DETAILS:
                final Intent cachesIntent = new Intent(this, CacheDetailActivity.class);
                cachesIntent.putExtra("geocode", cache.getGeocode().toUpperCase());
                cachesIntent.putExtra("name", cache.getName());
                startActivity(cachesIntent);
                break;
            case MENU_DROP_CACHE:
                cache.drop(new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        refreshCurrentList();
                    }
                });
                break;
            case MENU_MOVE_TO_LIST:
                new StoredList.UserInterface(this).promptForListSelection(R.string.cache_menu_move_list, new RunnableWithArgument<Integer>() {

                    @Override
                    public void run(Integer newListId) {
                        app.moveToList(Collections.singletonList(cache), newListId);
                        adapter.resetChecks();
                        refreshCurrentList();
                    }
                });
                break;
            case MENU_STORE_CACHE:
                //FIXME: this must use the same handler like in the CacheDetailActivity. Will be done by moving the handler into the store method.
                cache.store(this, null);
                break;
            default:
                // we must remember the menu info for the sub menu, there is a bug
                // in Android:
                // https://code.google.com/p/android/issues/detail?id=7139
                lastMenuInfo = info;
                if (cache != null) {
                    // create a search for a single cache (as if in details view)
                    cache.logOffline(this, LogType.getById(id - MENU_LOG_VISIT_OFFLINE));
                }
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

        adapter.setActualCoordinates(app.currentGeo().getCoords());

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
        startGeoAndDir();

        if (dir != null) {
            dirUpdate.updateDirection(dir);
        }
    }

    // Sensor & geolocation manager. This must be called from the UI thread as it may
    // cause the system listeners to start if nobody else required them before.
    private void startGeoAndDir() {
        app.addGeoObserver(this);
        if (Settings.isLiveList() && Settings.isUseCompass() && dir == null) {
            dir = app.startDir(this, dirUpdate);
        }
    }

    private void removeGeoAndDir() {
        app.deleteGeoObserver(this);
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

        int etaTime = ((detailTotal * 25) / 60);
        String message;
        if (etaTime < 1) {
            message = res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm);
        } else if (etaTime == 1) {
            message = res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getString(R.string.caches_eta_min);
        } else {
            message = res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getString(R.string.caches_eta_mins);
        }

        progress.show(this, null, message, ProgressDialog.STYLE_HORIZONTAL, loadDetailsHandler.obtainMessage(MSG_CANCEL));
        progress.setMaxProgressAndReset(detailTotal);

        detailProgressTime = System.currentTimeMillis();

        threadDetails = new LoadDetailsThread(loadDetailsHandler, listId);
        threadDetails.start();
    }

    public void removeFromHistoryCheck() {
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

    public void removeFromHistory() {
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
        progress.show(this, null, res.getString(R.string.caches_removing_from_history), ProgressDialog.STYLE_HORIZONTAL, removeFromHistoryHandler.obtainMessage(MSG_CANCEL));
        progress.setMaxProgressAndReset(detailTotal);

        threadH = new RemoveFromHistoryThread(removeFromHistoryHandler);
        threadH.start();
    }

    public void exportCaches() {
        List<cgCache> caches;
        if (adapter != null && adapter.getChecked() > 0) {
            // there are some caches checked
            caches = adapter.getCheckedCaches();
        } else {
            // no caches checked, export all
            caches = cacheList;
        }

        ExportFactory.showExportMenu(caches, this);
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
        progress.show(this, null, res.getString(R.string.caches_drop_progress), true, dropDetailsHandler.obtainMessage(MSG_CANCEL));
        new DropDetailsThread(dropDetailsHandler).start();
    }

    @Override
    public void update(final IGeoData geo) {
        if (adapter == null) {
            return;
        }

        try {
            if (cacheList != null && geo.getCoords() != null) {
                adapter.setActualCoordinates(geo.getCoords());
            }

            if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                if (!Settings.isUseCompass()) {
                    adapter.setActualHeading(geo.getBearing());
                }
                if (northHeading != null) {
                    adapter.setActualHeading(northHeading);
                }
            }
        } catch (Exception e) {
            Log.w("Failed to UpdateLocation location.");
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
            if (northHeading != null && adapter != null && (app.currentGeo().getSpeed() <= 5)) { // use compass when speed is lower than 18 km/h) {
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
            search = cgeoapplication.getInstance().getBatchOfStoredCaches(true, coords, Settings.getCacheType(), listId);
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
            search = GCParser.searchByNextPage(this, search, Settings.isShowCaptcha());

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
            }
        }

        @Override
        public void run() {
            search = GCParser.searchByCoords(this, coords, cacheType, Settings.isShowCaptcha());

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
            }
        }

        @Override
        public void run() {
            search = GCParser.searchByKeyword(this, keyword, cacheType, Settings.isShowCaptcha());
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
            }
        }

        @Override
        public void run() {
            search = GCParser.searchByUsername(this, username, cacheType, Settings.isShowCaptcha());
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
            }
        }

        @Override
        public void run() {
            search = GCParser.searchByOwner(this, username, cacheType, Settings.isShowCaptcha());
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
            removeGeoAndDir();

            final List<cgCache> cacheListTemp = new ArrayList<cgCache>(cacheList);
            for (cgCache cache : cacheListTemp) {
                if (checked > 0 && !cache.isStatusChecked()) {
                    handler.sendEmptyMessage(0);

                    yield();
                    continue;
                }

                try {
                    if (needToStop) {
                        Log.i("Stopped storing process.");
                        break;
                    }

                    if ((System.currentTimeMillis() - last) < 1500) {
                        try {
                            int delay = 1000 + ((Double) (Math.random() * 1000)).intValue() - (int) (System.currentTimeMillis() - last);
                            if (delay < 0) {
                                delay = 500;
                            }

                            Log.i("Waiting for next cache " + delay + " ms");
                            sleep(delay);
                        } catch (Exception e) {
                            Log.e("cgeocaches.LoadDetailsThread.sleep: " + e.toString());
                        }
                    }

                    if (needToStop) {
                        Log.i("Stopped storing process.");
                        break;
                    }

                    detailProgress++;
                    cache.refresh(cgeocaches.this, listIdLD, null);

                    handler.sendEmptyMessage(cacheList.indexOf(cache));

                    yield();
                } catch (Exception e) {
                    Log.e("cgeocaches.LoadDetailsThread: " + e.toString());
                }

                last = System.currentTimeMillis();
            }
            cacheListTemp.clear();

            handler.sendEmptyMessage(MSG_RESTART_GEO_AND_DIR);
            handler.sendEmptyMessage(MSG_DONE);
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
            int ret = MSG_DONE;

            removeGeoAndDir();

            int delay = -1;
            int times = 0;

            while (!needToStop && times < 3 * 60 / 5) // maximum: 3 minutes, every 5 seconds
            {
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

                        cgCache.storeCache(cgeocaches.this, null, response, listIdLFW, false, null);

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
                    if (delay == 0)
                    {
                        sleep(5000); //No caches 5s
                        times++;
                    } else {
                        sleep(500); //Cache was loaded 0.5s
                        times = 0;
                    }
                } catch (InterruptedException e) {
                    Log.e("cgeocaches.LoadFromWebThread.sleep: " + e.toString());
                }
            }

            handler.sendEmptyMessage(ret);

            startGeoAndDir();
        }
    }

    private class DropDetailsThread extends Thread {

        final private Handler handler;
        private List<cgCache> selected = new ArrayList<cgCache>();

        public DropDetailsThread(Handler handlerIn) {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;

            int checked = adapter.getChecked();
            if (checked == 0) {
                selected = new ArrayList<cgCache>(cacheList);
            }
            else {
                selected = adapter.getCheckedCaches();
            }
        }

        @Override
        public void run() {
            removeGeoAndDir();
            app.markDropped(selected);
            handler.sendEmptyMessage(MSG_DONE);

            startGeoAndDir();
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
                        Log.i("Stopped removing process.");
                        break;
                    }

                    app.clearVisitDate(cache.getGeocode());

                    detailProgress++;
                    handler.sendEmptyMessage(cacheList.indexOf(cache));

                    yield();
                } catch (Exception e) {
                    Log.e("cgeocaches.RemoveFromHistoryThread: " + e.toString());
                }
            }

            handler.sendEmptyMessage(MSG_DONE);
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
        final View loading = findViewById(R.id.loading);

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

        StoredList list = app.getList(id);
        if (list == null) {
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
            final List<cgCache> caches = adapter.getCheckedCaches();
            app.moveToList(caches, listId);
            handler.sendEmptyMessage(listId);
        }
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
        cachesIntent.putExtra(EXTRAS_COORDS, coords);

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
        cachesIntent.putExtra(EXTRAS_COORDS, coordsNow);
        context.startActivity(cachesIntent);
    }

    public static void startActivityHistory(Context context) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.HISTORY);
        context.startActivity(cachesIntent);
    }

    public static void startActivityAddress(final Context context, final Geopoint coords, final String address) {
        Intent addressIntent = new Intent(context, cgeocaches.class);
        addressIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.ADDRESS);
        addressIntent.putExtra(EXTRAS_COORDS, coords);
        addressIntent.putExtra("address", address);
        context.startActivity(addressIntent);
    }

    public static void startActivityCoordinates(final Context context, final Geopoint coords) {
        final Intent cachesIntent = new Intent(context, cgeocaches.class);
        cachesIntent.putExtra(EXTRAS_LIST_TYPE, CacheListType.COORDINATE);
        cachesIntent.putExtra(EXTRAS_COORDS, coords);
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
