package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.Settings.mapSourceEnum;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgDirection;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgUpdateDir;
import cgeo.geocaching.cgUpdateLoc;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.cgeocaches;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnDragListener;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Class representing the Map in c:geo
 */
public class CGeoMap extends AbstractMap implements OnDragListener, ViewFactory {

    private static final String EXTRAS_GEOCODE = "geocode";
    private static final String EXTRAS_LONGITUDE = "longitude";
    private static final String EXTRAS_LATITUDE = "latitude";
    private static final String EXTRAS_WPTTYPE = "wpttype";
    private static final String EXTRAS_MAPSTATE = "mapstate";
    private static final String EXTRAS_SEARCHID = "searchid";
    private static final String EXTRAS_DETAIL = "detail";
    private static final int MENU_SELECT_MAPVIEW = 1;
    private static final int MENU_MAP_LIVE = 2;
    private static final int MENU_STORE_CACHES = 3;
    private static final int MENU_TRAIL_MODE = 4;
    private static final int MENU_CIRCLE_MODE = 5;
    private static final int MENU_AS_LIST = 6;

    private static final int SUBMENU_VIEW_GOOGLE_MAP = 10;
    private static final int SUBMENU_VIEW_GOOGLE_SAT = 11;
    private static final int SUBMENU_VIEW_MF_MAPNIK = 13;
    private static final int SUBMENU_VIEW_MF_OSMARENDER = 14;
    private static final int SUBMENU_VIEW_MF_CYCLEMAP = 15;
    private static final int SUBMENU_VIEW_MF_OFFLINE = 16;
    private static final String EXTRAS_MAP_TITLE = "mapTitle";

    private Resources res = null;
    private Activity activity = null;
    private MapViewImpl mapView = null;
    private MapControllerImpl mapController = null;
    private cgBase base = null;
    private cgeoapplication app = null;
    private cgGeo geo = null;
    private cgDirection dir = null;
    private cgUpdateLoc geoUpdate = new UpdateLoc();
    private cgUpdateDir dirUpdate = new UpdateDir();
    // from intent
    private boolean fromDetailIntent = false;
    private String searchIdIntent = null;
    private String geocodeIntent = null;
    private Geopoint coordsIntent = null;
    private WaypointType waypointTypeIntent = null;
    private int[] mapStateIntent = null;
    // status data
    private UUID searchId = null;
    private String token = null;
    private boolean noMapTokenShowed = false;
    // map status data
    private boolean followMyLocation = false;
    private Integer centerLatitude = null;
    private Integer centerLongitude = null;
    private Integer spanLatitude = null;
    private Integer spanLongitude = null;
    private Integer centerLatitudeUsers = null;
    private Integer centerLongitudeUsers = null;
    private Integer spanLatitudeUsers = null;
    private Integer spanLongitudeUsers = null;
    // threads
    private LoadTimer loadTimer = null;
    private UsersTimer usersTimer = null;
    //FIXME should be members of LoadTimer since started by it.
    private LoadThread loadThread = null;
    private DownloadThread downloadThread = null;
    private DisplayThread displayThread = null;
    //FIXME should be members of UsersTimer since started by it.
    private UsersThread usersThread = null;
    private DisplayUsersThread displayUsersThread = null;
    //FIXME move to OnOptionsItemSelected
    private LoadDetails loadDetailsThread = null;
    /** Time of last {@link LoadThread} run */
    private volatile long loadThreadRun = 0L;
    /** Time of last {@link UsersThread} run */
    private volatile long usersThreadRun = 0L;
    //Interthread communication flag
    private volatile boolean downloaded = false;
    // overlays
    private CachesOverlay overlayCaches = null;
    private OtherCachersOverlay overlayOtherCachers = null;
    private ScaleOverlay overlayScale = null;
    private PositionOverlay overlayPosition = null;
    // data for overlays
    private int cachesCnt = 0;
    private Map<Integer, Drawable> iconsCache = new HashMap<Integer, Drawable>();
    /** List of caches in the viewport */
    private List<cgCache> caches = new ArrayList<cgCache>();
    /** List of users in the viewport */
    private List<cgUser> users = new ArrayList<cgUser>();
    private List<cgCoord> coordinates = new ArrayList<cgCoord>();
    // storing for offline
    private ProgressDialog waitDialog = null;
    private int detailTotal = 0;
    private int detailProgress = 0;
    private Long detailProgressTime = 0L;
    // views
    private ImageSwitcher myLocSwitch = null;
    // other things
    private boolean live = true; // live map (live, dead) or rest (displaying caches on map)
    private boolean liveChanged = false; // previous state for loadTimer
    private boolean centered = false; // if map is already centered
    private boolean alreadyCentered = false; // -""- for setting my location
    // handlers
    /** Updates the titles */
    final private Handler displayHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            if (what == 0) {
                // set title
                final StringBuilder title = new StringBuilder();

                if (live) {
                    title.append(res.getString(R.string.map_live));
                } else {
                    title.append(mapTitle);
                }

                if (caches != null && cachesCnt > 0 && !mapTitle.contains("[")) {
                    title.append(" [");
                    title.append(caches.size());
                    title.append(']');
                }

                ActivityMixin.setTitle(activity, title.toString());
            } else if (what == 1 && mapView != null) {
                mapView.invalidate();
            }
        }
    };
    /** Updates the progress. */
    final private Handler showProgressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            if (what == 0) {
                ActivityMixin.showProgress(activity, false);
            } else if (what == 1) {
                ActivityMixin.showProgress(activity, true);
            }
        }
    };
    final private Handler loadDetailsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (waitDialog != null) {
                    int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                    int secondsRemaining;
                    if (detailProgress > 0) //DP can be zero and cause devisionByZero
                        secondsRemaining = (detailTotal - detailProgress) * secondsElapsed / detailProgress;
                    else
                        secondsRemaining = (detailTotal - detailProgress) * secondsElapsed;

                    waitDialog.setProgress(detailProgress);
                    if (secondsRemaining < 40) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                    } else if (secondsRemaining < 90) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + String.format(Locale.getDefault(), "%d", (secondsRemaining / 60)) + " " + res.getString(R.string.caches_eta_min));
                    } else {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + String.format(Locale.getDefault(), "%d", (secondsRemaining / 60)) + " " + res.getString(R.string.caches_eta_mins));
                    }
                }
            } else {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }

                if (geo == null) {
                    geo = app.startGeo(activity, geoUpdate, base, 0, 0);
                }
                if (Settings.isUseCompass() && dir == null) {
                    dir = app.startDir(activity, dirUpdate);
                }
            }
        }
    };
    final private Handler noMapTokenHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (!noMapTokenShowed) {
                ActivityMixin.showToast(activity, res.getString(R.string.map_token_err));

                noMapTokenShowed = true;
            }
        }
    };
    /**
     * calling activities can set the map title via extras
     */
    private String mapTitle;

    public CGeoMap(MapActivityImpl activity) {
        super(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // class init
        res = this.getResources();
        activity = this.getActivity();
        app = (cgeoapplication) activity.getApplication();
        app.setAction(null);
        base = new cgBase(app);
        MapFactory mapFactory = Settings.getMapFactory();

        // reset status
        noMapTokenShowed = false;

        // set layout
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set layout
        ActivityMixin.setTheme(activity);
        activity.setContentView(Settings.getMapFactory().getMapLayoutId());
        ActivityMixin.setTitle(activity, res.getString(R.string.map_map));

        if (geo == null) {
            geo = app.startGeo(activity, geoUpdate, base, 0, 0);
        }
        if (Settings.isUseCompass() && dir == null) {
            dir = app.startDir(activity, dirUpdate);
        }

        // initialize map
        mapView = (MapViewImpl) activity.findViewById(mapFactory.getMapViewId());
        mapView.setMapSource();
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        mapView.preLoad();
        mapView.setOnDragListener(this);

        // initialize overlays
        mapView.clearOverlays();

        if (overlayPosition == null) {
            overlayPosition = mapView.createAddPositionOverlay(activity);
        }

        if (Settings.isPublicLoc() && overlayOtherCachers == null) {
            overlayOtherCachers = mapView.createAddUsersOverlay(activity, getResources().getDrawable(R.drawable.user_location));
        }

        if (overlayCaches == null) {
            overlayCaches = mapView.createAddMapOverlay(mapView.getContext(), getResources().getDrawable(R.drawable.marker), fromDetailIntent);
        }

        if (overlayScale == null) {
            overlayScale = mapView.createAddScaleOverlay(activity);
        }

        mapView.invalidate();

        mapController = mapView.getMapController();
        mapController.setZoom(Settings.getMapZoom());

        // start location and directory services
        if (geo != null) {
            geoUpdate.updateLoc(geo);
        }
        if (dir != null) {
            dirUpdate.updateDir(dir);
        }

        // get parameters
        Bundle extras = activity.getIntent().getExtras();
        if (extras != null) {
            fromDetailIntent = extras.getBoolean(EXTRAS_DETAIL);
            searchIdIntent = extras.getString(EXTRAS_SEARCHID);
            geocodeIntent = extras.getString(EXTRAS_GEOCODE);
            final double latitudeIntent = extras.getDouble(EXTRAS_LATITUDE);
            final double longitudeIntent = extras.getDouble(EXTRAS_LONGITUDE);
            coordsIntent = new Geopoint(latitudeIntent, longitudeIntent);
            waypointTypeIntent = WaypointType.FIND_BY_ID.get(extras.getString(EXTRAS_WPTTYPE));
            mapStateIntent = extras.getIntArray(EXTRAS_MAPSTATE);
            mapTitle = extras.getString(EXTRAS_MAP_TITLE);

            if ("".equals(searchIdIntent)) {
                searchIdIntent = null;
            }
            if (coordsIntent.getLatitude() == 0.0 || coordsIntent.getLongitude() == 0.0) {
                coordsIntent = null;
            }
        }

        if (StringUtils.isBlank(mapTitle)) {
            mapTitle = res.getString(R.string.map_map);
        }

        // live map, if no arguments are given
        live = (searchIdIntent == null && geocodeIntent == null && coordsIntent == null);

        if (null == mapStateIntent) {
            followMyLocation = live;
        } else {
            followMyLocation = 1 == mapStateIntent[3] ? true : false;
        }
        if (geocodeIntent != null || searchIdIntent != null || coordsIntent != null || mapStateIntent != null) {
            centerMap(geocodeIntent, searchIdIntent, coordsIntent, mapStateIntent);
        }

        // prepare my location button
        myLocSwitch = (ImageSwitcher) activity.findViewById(R.id.my_position);
        myLocSwitch.setFactory(this);
        myLocSwitch.setInAnimation(activity, android.R.anim.fade_in);
        myLocSwitch.setOutAnimation(activity, android.R.anim.fade_out);
        myLocSwitch.setOnClickListener(new MyLocationListener());
        switchMyLocationButton();

        startTimer();

        // show the filter warning bar if the filter is set
        if (Settings.getCacheType() != null) {
            String cacheType = cgBase.cacheTypesInv.get(Settings.getCacheType());
            ((TextView) activity.findViewById(R.id.filter_text)).setText(cacheType);
            activity.findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        app.setAction(null);
        if (geo == null) {
            geo = app.startGeo(activity, geoUpdate, base, 0, 0);
        }
        if (Settings.isUseCompass() && dir == null) {
            dir = app.startDir(activity, dirUpdate);
        }

        if (geo != null) {
            geoUpdate.updateLoc(geo);
        }
        if (dir != null) {
            dirUpdate.updateDir(dir);
        }

        startTimer();
    }

    @Override
    public void onStop() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
        }

        if (usersTimer != null) {
            usersTimer.stopIt();
            usersTimer = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
        }

        if (usersTimer != null) {
            usersTimer.stopIt();
            usersTimer = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
        }

        if (usersTimer != null) {
            usersTimer.stopIt();
            usersTimer = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        SubMenu submenu = menu.addSubMenu(1, MENU_SELECT_MAPVIEW, 0, res.getString(R.string.map_view_map)).setIcon(android.R.drawable.ic_menu_mapmode);
        addMapViewMenuItems(submenu);

        menu.add(0, MENU_MAP_LIVE, 0, res.getString(R.string.map_live_disable)).setIcon(R.drawable.ic_menu_notifications);
        menu.add(0, MENU_STORE_CACHES, 0, res.getString(R.string.caches_store_offline)).setIcon(android.R.drawable.ic_menu_set_as).setEnabled(false);
        menu.add(0, MENU_TRAIL_MODE, 0, res.getString(R.string.map_trail_hide)).setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(0, MENU_CIRCLE_MODE, 0, res.getString(R.string.map_circles_hide)).setIcon(R.drawable.ic_menu_circle);
        menu.add(0, MENU_AS_LIST, 0, res.getString(R.string.map_as_list)).setIcon(android.R.drawable.ic_menu_agenda);

        return true;
    }

    private void addMapViewMenuItems(final Menu menu) {
        String[] mapViews = res.getStringArray(R.array.map_sources);
        mapSourceEnum mapSource = Settings.getMapSource();

        menu.add(1, SUBMENU_VIEW_GOOGLE_MAP, 0, mapViews[0]).setCheckable(true).setChecked(mapSource == mapSourceEnum.googleMap);
        menu.add(1, SUBMENU_VIEW_GOOGLE_SAT, 0, mapViews[1]).setCheckable(true).setChecked(mapSource == mapSourceEnum.googleSat);
        menu.add(1, SUBMENU_VIEW_MF_MAPNIK, 0, mapViews[2]).setCheckable(true).setChecked(mapSource == mapSourceEnum.mapsforgeMapnik);
        menu.add(1, SUBMENU_VIEW_MF_OSMARENDER, 0, mapViews[3]).setCheckable(true).setChecked(mapSource == mapSourceEnum.mapsforgeOsmarender);
        menu.add(1, SUBMENU_VIEW_MF_CYCLEMAP, 0, mapViews[4]).setCheckable(true).setChecked(mapSource == mapSourceEnum.mapsforgeCycle);
        menu.add(1, SUBMENU_VIEW_MF_OFFLINE, 0, mapViews[5]).setCheckable(true).setChecked(mapSource == mapSourceEnum.mapsforgeOffline);
        menu.setGroupCheckable(1, true, true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item;
        try {
            item = menu.findItem(MENU_TRAIL_MODE); // show trail
            if (Settings.isMapTrail()) {
                item.setTitle(res.getString(R.string.map_trail_hide));
            } else {
                item.setTitle(res.getString(R.string.map_trail_show));
            }

            item = menu.findItem(MENU_MAP_LIVE); // live map
            if (live) {
                if (Settings.isLiveMap()) {
                    item.setTitle(res.getString(R.string.map_live_disable));
                } else {
                    item.setTitle(res.getString(R.string.map_live_enable));
                }
            } else {
                item.setEnabled(false);
                item.setTitle(res.getString(R.string.map_live_enable));
            }

            menu.findItem(MENU_STORE_CACHES).setEnabled(live && !isLoading() && CollectionUtils.isNotEmpty(caches) && app.hasUnsavedCaches(searchId));

            item = menu.findItem(MENU_CIRCLE_MODE); // show circles
            if (overlayCaches != null && overlayCaches.getCircles()) {
                item.setTitle(res.getString(R.string.map_circles_hide));
            } else {
                item.setTitle(res.getString(R.string.map_circles_show));
            }

            menu.findItem(SUBMENU_VIEW_MF_OFFLINE).setEnabled(Settings.isValidMapFile());

            item = menu.findItem(MENU_AS_LIST);
            item.setVisible(live);
            item.setEnabled(CollectionUtils.isNotEmpty(caches));
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeomap.onPrepareOptionsMenu: " + e.toString());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case MENU_TRAIL_MODE:
                Settings.setMapTrail(!Settings.isMapTrail());
                return true;
            case MENU_MAP_LIVE:
                Settings.setLiveMap(!Settings.isLiveMap());
                liveChanged = true;
                searchId = null;
                searchIdIntent = null;
                return true;
            case MENU_STORE_CACHES:
                if (live && !isLoading() && CollectionUtils.isNotEmpty(caches)) {
                    final List<String> geocodes = new ArrayList<String>();

                    List<cgCache> cachesProtected = new ArrayList<cgCache>(caches);
                    try {
                        if (cachesProtected.size() > 0) {
                            final GeoPointImpl mapCenter = mapView.getMapViewCenter();
                            final int mapCenterLat = mapCenter.getLatitudeE6();
                            final int mapCenterLon = mapCenter.getLongitudeE6();
                            final int mapSpanLat = mapView.getLatitudeSpan();
                            final int mapSpanLon = mapView.getLongitudeSpan();

                            for (cgCache oneCache : cachesProtected) {
                                if (oneCache != null && oneCache.coords != null) {
                                    if (cgBase.isCacheInViewPort(mapCenterLat, mapCenterLon, mapSpanLat, mapSpanLon, oneCache.coords) && app.isOffline(oneCache.geocode, null) == false) {
                                        geocodes.add(oneCache.geocode);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(Settings.tag, "cgeomap.onOptionsItemSelected.#4: " + e.toString());
                    }

                    detailTotal = geocodes.size();
                    detailProgress = 0;

                    if (detailTotal == 0) {
                        ActivityMixin.showToast(activity, res.getString(R.string.warn_save_nothing));

                        return true;
                    }

                    waitDialog = new ProgressDialog(activity);
                    waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    waitDialog.setCancelable(true);
                    waitDialog.setMax(detailTotal);
                    waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                        public void onCancel(DialogInterface arg0) {
                            try {
                                if (loadDetailsThread != null) {
                                    loadDetailsThread.stopIt();
                                }

                                if (geo == null) {
                                    geo = app.startGeo(activity, geoUpdate, base, 0, 0);
                                }
                                if (Settings.isUseCompass() && dir == null) {
                                    dir = app.startDir(activity, dirUpdate);
                                }
                            } catch (Exception e) {
                                Log.e(Settings.tag, "cgeocaches.onPrepareOptionsMenu.onCancel: " + e.toString());
                            }
                        }
                    });

                    Float etaTime = Float.valueOf((detailTotal * (float) 7) / 60);
                    if (etaTime < 0.4) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                    } else if (etaTime < 1.5) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + String.format(Locale.getDefault(), "%.0f", etaTime) + " " + res.getString(R.string.caches_eta_min));
                    } else {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + String.format(Locale.getDefault(), "%.0f", etaTime) + " " + res.getString(R.string.caches_eta_mins));
                    }
                    waitDialog.show();

                    detailProgressTime = System.currentTimeMillis();

                    loadDetailsThread = new LoadDetails(loadDetailsHandler, geocodes);
                    loadDetailsThread.start();
                }
                return true;
            case MENU_CIRCLE_MODE:
                if (overlayCaches == null) {
                    return false;
                }

                overlayCaches.switchCircles();
                mapView.invalidate();
                return true;
            case MENU_AS_LIST:
                final cgSearch search = new cgSearch();
                search.totalCnt = caches.size();
                for (cgCache cache : caches) {
                    search.addGeocode(cache.geocode);
                }
                cgeocaches.startActivityMap(activity, app.addSearch(search, caches, true, 0));
                return true;
            default:
                if (SUBMENU_VIEW_GOOGLE_MAP <= id && SUBMENU_VIEW_MF_OFFLINE >= id) {
                    item.setChecked(true);
                    mapSourceEnum mapSource = getMapSourceFromMenuId(id);

                    boolean mapRestartRequired = switchMapSource(mapSource);

                    if (mapRestartRequired) {
                        // close old mapview
                        activity.finish();

                        // prepare information to restart a similar view
                        Intent mapIntent = new Intent(activity, Settings.getMapFactory().getMapClass());

                        mapIntent.putExtra(EXTRAS_DETAIL, fromDetailIntent);
                        mapIntent.putExtra(EXTRAS_SEARCHID, searchIdIntent);
                        mapIntent.putExtra(EXTRAS_GEOCODE, geocodeIntent);
                        if (coordsIntent != null) {
                            mapIntent.putExtra(EXTRAS_LATITUDE, coordsIntent.getLatitude());
                            mapIntent.putExtra(EXTRAS_LONGITUDE, coordsIntent.getLongitude());
                        }
                        mapIntent.putExtra(EXTRAS_WPTTYPE, waypointTypeIntent != null ? waypointTypeIntent.id : null);
                        int[] mapState = new int[4];
                        GeoPointImpl mapCenter = mapView.getMapViewCenter();
                        mapState[0] = mapCenter.getLatitudeE6();
                        mapState[1] = mapCenter.getLongitudeE6();
                        mapState[2] = mapView.getMapZoomLevel();
                        mapState[3] = followMyLocation ? 1 : 0;
                        mapIntent.putExtra(EXTRAS_MAPSTATE, mapState);

                        // start the new map
                        activity.startActivity(mapIntent);

                    }

                    return true;
                }
                break;
        }
        return false;
    }

    private static mapSourceEnum getMapSourceFromMenuId(int menuItemId) {

        switch (menuItemId) {
            case SUBMENU_VIEW_GOOGLE_MAP:
                return mapSourceEnum.googleMap;
            case SUBMENU_VIEW_GOOGLE_SAT:
                return mapSourceEnum.googleSat;
            case SUBMENU_VIEW_MF_OSMARENDER:
                return mapSourceEnum.mapsforgeOsmarender;
            case SUBMENU_VIEW_MF_MAPNIK:
                return mapSourceEnum.mapsforgeMapnik;
            case SUBMENU_VIEW_MF_CYCLEMAP:
                return mapSourceEnum.mapsforgeCycle;
            case SUBMENU_VIEW_MF_OFFLINE:
                return mapSourceEnum.mapsforgeOffline;
            default:
                return mapSourceEnum.googleMap;
        }
    }

    private boolean switchMapSource(mapSourceEnum mapSource) {
        boolean oldIsGoogle = Settings.getMapSource().isGoogleMapSource();

        Settings.setMapSource(mapSource);

        boolean mapRestartRequired = mapSource.isGoogleMapSource() != oldIsGoogle;

        if (!mapRestartRequired) {
            mapView.setMapSource();
        }

        return mapRestartRequired;
    }

    private void savePrefs() {
        if (mapView == null) {
            return;
        }

        Settings.setMapZoom(mapView.getMapZoomLevel());
    }

    // set center of map to my location
    private void myLocationInMiddle() {
        if (geo == null) {
            return;
        }
        if (!followMyLocation) {
            return;
        }

        centerMap(geo.coordsNow);
    }

    // class: update location
    private class UpdateLoc extends cgUpdateLoc {

        @Override
        public void updateLoc(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                boolean repaintRequired = false;

                if (overlayPosition == null && mapView != null) {
                    overlayPosition = mapView.createAddPositionOverlay(activity);
                }

                if (overlayPosition != null && geo.location != null) {
                    overlayPosition.setCoordinates(geo.location);
                }

                if (geo.coordsNow != null) {
                    if (followMyLocation) {
                        myLocationInMiddle();
                    } else {
                        repaintRequired = true;
                    }
                }

                if (!Settings.isUseCompass() || (geo.speedNow != null && geo.speedNow > 5)) { // use GPS when speed is higher than 18 km/h
                    if (geo.bearingNow != null) {
                        overlayPosition.setHeading(geo.bearingNow);
                    } else {
                        overlayPosition.setHeading(0f);
                    }
                    repaintRequired = true;
                }

                if (repaintRequired && mapView != null) {
                    mapView.repaintRequired(overlayPosition);
                }

            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
            }
        }
    }

    // class: update direction
    private class UpdateDir extends cgUpdateDir {

        @Override
        public void updateDir(cgDirection dir) {
            if (dir == null || dir.directionNow == null) {
                return;
            }

            if (overlayPosition != null && mapView != null && (geo == null || geo.speedNow == null || geo.speedNow <= 5)) { // use compass when speed is lower than 18 km/h
                overlayPosition.setHeading(dir.directionNow);
                mapView.invalidate();
            }
        }
    }

    /**
     * Starts the {@link LoadTimer} and {@link UsersTimer}.
     */

    public synchronized void startTimer() {
        if (coordsIntent != null) {
            // display just one point
            (new DisplayPointThread()).start();
        } else {
            // start timer
            if (loadTimer != null) {
                loadTimer.stopIt();
                loadTimer = null;
            }
            loadTimer = new LoadTimer();
            loadTimer.start();
        }

        if (Settings.isPublicLoc()) {
            if (usersTimer != null) {
                usersTimer.stopIt();
                usersTimer = null;
            }
            usersTimer = new UsersTimer();
            usersTimer.start();
        }
    }

    /**
     * loading timer Triggers every 250ms and checks for viewport change and starts a {@link LoadThread}.
     */
    private class LoadTimer extends Thread {

        public LoadTimer() {
            super("Load Timer");
        }

        private volatile boolean stop = false;

        public void stopIt() {
            stop = true;

            if (loadThread != null) {
                loadThread.stopIt();
                loadThread = null;
            }

            if (downloadThread != null) {
                downloadThread.stopIt();
                downloadThread = null;
            }

            if (displayThread != null) {
                displayThread.stopIt();
                displayThread = null;
            }
        }

        @Override
        public void run() {
            GeoPointImpl mapCenterNow;
            int centerLatitudeNow;
            int centerLongitudeNow;
            int spanLatitudeNow;
            int spanLongitudeNow;
            boolean moved = false;
            boolean force = false;
            long currentTime = 0;

            while (!stop) {
                try {
                    sleep(250);

                    if (mapView != null) {
                        // get current viewport
                        mapCenterNow = mapView.getMapViewCenter();
                        centerLatitudeNow = mapCenterNow.getLatitudeE6();
                        centerLongitudeNow = mapCenterNow.getLongitudeE6();
                        spanLatitudeNow = mapView.getLatitudeSpan();
                        spanLongitudeNow = mapView.getLongitudeSpan();

                        // check if map moved or zoomed
                        //TODO Portree Use Rectangle inside with bigger search window. That will stop reloading on every move
                        moved = false;
                        force = false;

                        if (liveChanged) {
                            moved = true;
                            force = true;
                        } else if (live && Settings.isLiveMap() && downloaded == false) {
                            moved = true;
                        } else if (centerLatitude == null || centerLongitude == null) {
                            moved = true;
                        } else if (spanLatitude == null || spanLongitude == null) {
                            moved = true;
                        } else if (((Math.abs(spanLatitudeNow - spanLatitude) > 50) || (Math.abs(spanLongitudeNow - spanLongitude) > 50) || // changed zoom
                                (Math.abs(centerLatitudeNow - centerLatitude) > (spanLatitudeNow / 4)) || (Math.abs(centerLongitudeNow - centerLongitude) > (spanLongitudeNow / 4)) // map moved
                        ) && (cachesCnt <= 0 || CollectionUtils.isEmpty(caches)
                                || !cgBase.isInViewPort(centerLatitude, centerLongitude, centerLatitudeNow, centerLongitudeNow, spanLatitude, spanLongitude, spanLatitudeNow, spanLongitudeNow))) {
                            moved = true;
                        }

                        if (moved && caches != null && centerLatitude != null && centerLongitude != null && ((Math.abs(centerLatitudeNow - centerLatitude) > (spanLatitudeNow * 1.2)) || (Math.abs(centerLongitudeNow - centerLongitude) > (spanLongitudeNow * 1.2)))) {
                            force = true;
                        }

                        //LeeB
                        // save new values
                        if (moved) {
                            liveChanged = false;

                            currentTime = System.currentTimeMillis();

                            if (1000 < (currentTime - loadThreadRun)) {
                                // from web
                                if (20000 < (currentTime - loadThreadRun)) {
                                    force = true; // probably stucked thread
                                }

                                if (force && loadThread != null && loadThread.isWorking()) {
                                    loadThread.stopIt();

                                    try {
                                        sleep(100);
                                    } catch (Exception e) {
                                        // nothing
                                    }
                                }

                                if (loadThread != null && loadThread.isWorking()) {
                                    continue;
                                }

                                centerLatitude = centerLatitudeNow;
                                centerLongitude = centerLongitudeNow;
                                spanLatitude = spanLatitudeNow;
                                spanLongitude = spanLongitudeNow;

                                showProgressHandler.sendEmptyMessage(1); // show progress

                                loadThread = new LoadThread(centerLatitude, centerLongitude, spanLatitude, spanLongitude);
                                loadThread.start(); //loadThread will kick off downloadThread once it's done
                            }
                        }
                    }

                    if (!isLoading()) {
                        showProgressHandler.sendEmptyMessage(0); // hide progress
                    }

                    yield();
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgeomap.LoadTimer.run: " + e.toString());
                }
            }
        }
    }

    /**
     * Timer triggering every 250 ms to start the {@link UsersThread} for displaying user.
     */

    private class UsersTimer extends Thread {

        public UsersTimer() {
            super("Users Timer");
        }

        private volatile boolean stop = false;

        public void stopIt() {
            stop = true;

            if (usersThread != null) {
                usersThread.stopIt();
                usersThread = null;
            }

            if (displayUsersThread != null) {
                displayUsersThread.stopIt();
                displayUsersThread = null;
            }
        }

        @Override
        public void run() {
            GeoPointImpl mapCenterNow;
            int centerLatitudeNow;
            int centerLongitudeNow;
            int spanLatitudeNow;
            int spanLongitudeNow;
            boolean moved = false;
            long currentTime = 0;

            while (!stop) {
                try {
                    sleep(250);

                    if (mapView != null) {
                        // get current viewport
                        mapCenterNow = mapView.getMapViewCenter();
                        centerLatitudeNow = mapCenterNow.getLatitudeE6();
                        centerLongitudeNow = mapCenterNow.getLongitudeE6();
                        spanLatitudeNow = mapView.getLatitudeSpan();
                        spanLongitudeNow = mapView.getLongitudeSpan();

                        // check if map moved or zoomed
                        moved = false;

                        currentTime = System.currentTimeMillis();

                        if (60000 < (currentTime - usersThreadRun)) {
                            moved = true;
                        } else if (centerLatitudeUsers == null || centerLongitudeUsers == null) {
                            moved = true;
                        } else if (spanLatitudeUsers == null || spanLongitudeUsers == null) {
                            moved = true;
                        } else if (((Math.abs(spanLatitudeNow - spanLatitudeUsers) > 50) || (Math.abs(spanLongitudeNow - spanLongitudeUsers) > 50) || // changed zoom
                                (Math.abs(centerLatitudeNow - centerLatitudeUsers) > (spanLatitudeNow / 4)) || (Math.abs(centerLongitudeNow - centerLongitudeUsers) > (spanLongitudeNow / 4)) // map moved
                        ) && !cgBase.isInViewPort(centerLatitudeUsers, centerLongitudeUsers, centerLatitudeNow, centerLongitudeNow, spanLatitudeUsers, spanLongitudeUsers, spanLatitudeNow, spanLongitudeNow)) {
                            moved = true;
                        }

                        // save new values
                        if (moved && (1000 < (currentTime - usersThreadRun))) {
                            if (usersThread != null && usersThread.isWorking()) {
                                continue;
                            }

                            centerLatitudeUsers = centerLatitudeNow;
                            centerLongitudeUsers = centerLongitudeNow;
                            spanLatitudeUsers = spanLatitudeNow;
                            spanLongitudeUsers = spanLongitudeNow;

                            usersThread = new UsersThread(centerLatitude, centerLongitude, spanLatitude, spanLongitude);
                            usersThread.start();
                        }
                    }

                    yield();
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgeomap.LoadUsersTimer.run: " + e.toString());
                }
            }
        }
    }

    /**
     * Worker thread that loads caches and waypoints from the database and then spawns the {@link DownloadThread}.
     * started by {@link LoadTimer}
     */

    private class LoadThread extends DoThread {

        public LoadThread(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
            setName("Load Thread");
        }

        @Override
        public void run() {
            try {
                stop = false;
                working = true;
                loadThreadRun = System.currentTimeMillis();

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                //LeeB - I think this can be done better:
                //1. fetch and draw(in another thread) caches from the db (fast? db read will be the slow bit)
                //2. fetch and draw(in another thread) and then insert into the db caches from geocaching.com - dont draw/insert if exist in memory?

                // stage 1 - pull and render from the DB only

                if (fromDetailIntent || StringUtils.isNotEmpty(searchIdIntent)) {
                    searchId = UUID.fromString(searchIdIntent);
                } else {
                    if (!live || !Settings.isLiveMap()) {
                        searchId = app.getStoredInViewport(centerLat, centerLon, spanLat, spanLon, Settings.getCacheType());
                    } else {
                        searchId = app.getCachedInViewport(centerLat, centerLon, spanLat, spanLon, Settings.getCacheType());
                    }
                }

                if (searchId != null) {
                    downloaded = true;
                }

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                caches = app.getCaches(searchId);

                //if in live map and stored caches are found / disables are also shown.
                if (live && Settings.isLiveMap()) {
                    final boolean excludeMine = Settings.isExcludeMyCaches();
                    final boolean excludeDisabled = Settings.isExcludeDisabledCaches();

                    for (int i = caches.size() - 1; i >= 0; i--) {
                        cgCache cache = caches.get(i);
                        if ((cache.found && excludeMine) || (cache.own && excludeMine) || (cache.disabled && excludeDisabled)) {
                            caches.remove(i);
                        }
                    }

                }

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                //render
                if (displayThread != null && displayThread.isWorking()) {
                    displayThread.stopIt();
                }
                displayThread = new DisplayThread(centerLat, centerLon, spanLat, spanLon);
                displayThread.start();

                if (stop) {
                    displayThread.stopIt();
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                //*** this needs to be in it's own thread
                // stage 2 - pull and render from geocaching.com
                //this should just fetch and insert into the db _and_ be cancel-able if the viewport changes

                if (live && Settings.isLiveMap()) {
                    if (downloadThread != null && downloadThread.isWorking()) {
                        downloadThread.stopIt();
                    }
                    downloadThread = new DownloadThread(centerLat, centerLon, spanLat, spanLon);
                    downloadThread.setName("downloadThread");
                    downloadThread.start();
                }
            } finally {
                working = false;
            }
        }
    }

    /**
     * Worker thread downloading caches from the internet.
     * Started by {@link LoadThread}. Duplicate Code with {@link UsersThread}
     */

    private class DownloadThread extends DoThread {

        public DownloadThread(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
        }

        @Override
        public void run() { //first time we enter we have crappy long/lat....
            try {
                stop = false;
                working = true;

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                double lat1 = (centerLat / 1e6) - ((spanLat / 1e6) / 2) - ((spanLat / 1e6) / 4);
                double lat2 = (centerLat / 1e6) + ((spanLat / 1e6) / 2) + ((spanLat / 1e6) / 4);
                double lon1 = (centerLon / 1e6) - ((spanLon / 1e6) / 2) - ((spanLon / 1e6) / 4);
                double lon2 = (centerLon / 1e6) + ((spanLon / 1e6) / 2) + ((spanLon / 1e6) / 4);

                double latMin = Math.min(lat1, lat2);
                double latMax = Math.max(lat1, lat2);
                double lonMin = Math.min(lon1, lon2);
                double lonMax = Math.max(lon1, lon2);


                //*** this needs to be in it's own thread
                // stage 2 - pull and render from geocaching.com
                //this should just fetch and insert into the db _and_ be cancel-able if the viewport changes

                if (token == null) {
                    token = cgBase.getMapUserToken(noMapTokenHandler);
                }

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                searchId = base.searchByViewport(token, latMin, latMax, lonMin, lonMax, 0);
                if (searchId != null) {
                    downloaded = true;
                }

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                //TODO Portree Only overwrite if we got some. Otherwise maybe error icon
                //TODO Merge not to show locally found caches
                caches = app.getCaches(searchId, centerLat, centerLon, spanLat, spanLon);

                if (stop) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                //render
                if (displayThread != null && displayThread.isWorking()) {
                    displayThread.stopIt();
                }
                displayThread = new DisplayThread(centerLat, centerLon, spanLat, spanLon);
                displayThread.start();

            } finally {
                working = false;
            }
        }
    }

    /**
     * Thread to Display (down)loaded caches. Started by {@link LoadThread} and {@link DownloadThread}
     */
    private class DisplayThread extends DoThread {

        public DisplayThread(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
            setName("Display Thread");
        }

        @Override
        public void run() {
            try {
                stop = false;
                working = true;

                if (mapView == null || caches == null) {
                    displayHandler.sendEmptyMessage(0);
                    working = false;

                    return;
                }

                // display caches
                final List<cgCache> cachesProtected = new ArrayList<cgCache>(caches);
                final List<CachesOverlayItemImpl> items = new ArrayList<CachesOverlayItemImpl>();

                if (!cachesProtected.isEmpty()) {
                    for (cgCache cacheOne : cachesProtected) {
                        if (stop) {
                            displayHandler.sendEmptyMessage(0);
                            working = false;

                            return;
                        }

                        if (cacheOne.coords == null) {
                            continue;
                        }

                        // display cache waypoints
                        if (cacheOne.waypoints != null
                                // Only show waypoints for single view or setting
                                // when less than showWaypointsthreshold Caches shown
                                && (cachesProtected.size() == 1 || (cachesProtected.size() < Settings.getWayPointsThreshold()))
                                && !cacheOne.waypoints.isEmpty()) {
                            for (cgWaypoint oneWaypoint : cacheOne.waypoints) {
                                if (oneWaypoint.coords == null) {
                                    continue;
                                }

                                items.add(getWaypointItem(new cgCoord(oneWaypoint), oneWaypoint.type));
                            }
                        }
                        items.add(getCacheItem(new cgCoord(cacheOne), cacheOne.type, cacheOne.own, cacheOne.found, cacheOne.disabled));
                    }

                    overlayCaches.updateItems(items);
                    displayHandler.sendEmptyMessage(1);

                    cachesCnt = cachesProtected.size();

                    if (stop) {
                        displayHandler.sendEmptyMessage(0);
                        working = false;

                        return;
                    }

                } else {
                    overlayCaches.updateItems(items);
                    displayHandler.sendEmptyMessage(1);
                }

                cachesProtected.clear();

                displayHandler.sendEmptyMessage(0);
            } finally {
                working = false;
            }
        }

        /**
         * Returns a OverlayItem representing the cache
         *
         * @param cgCoord
         *            The coords
         * @param type
         *            String name
         * @param own
         *            true for own caches
         * @param found
         *            true for found
         * @param disabled
         *            true for disabled
         * @return
         */
        private CachesOverlayItemImpl getCacheItem(cgCoord cgCoord, String type, boolean own, boolean found, boolean disabled) {
            return getItem(cgCoord, cgBase.getCacheMarkerIcon(type, own, found, disabled));
        }

        /**
         * Returns a OverlayItem representing the waypoint
         *
         * @param cgCoord
         *            The coords
         * @param type
         *            The waypoint's type
         * @return
         */
        private CachesOverlayItemImpl getWaypointItem(cgCoord cgCoord, WaypointType type) {
            return getItem(cgCoord, type != null ? type.markerId : WaypointType.WAYPOINT.markerId);
        }

        /**
         * Returns a OverlayItem represented by an icon
         *
         * @param cgCoord
         *            The coords
         * @param icon
         *            The icon
         * @return
         */
        private CachesOverlayItemImpl getItem(cgCoord cgCoord, int icon) {
            coordinates.add(cgCoord);
            CachesOverlayItemImpl item = Settings.getMapFactory().getCachesOverlayItem(cgCoord, null);

            Drawable pin = null;
            if (iconsCache.containsKey(icon)) {
                pin = iconsCache.get(icon);
            } else {
                pin = getResources().getDrawable(icon);
                pin.setBounds(0, 0, pin.getIntrinsicWidth(), pin.getIntrinsicHeight());
                iconsCache.put(icon, pin);
            }
            item.setMarker(pin);

            return item;
        }
    }

    /**
     * Thread to load users from Go 4 Cache
     * Duplicate Code with {@link DownloadThread}
     */

    private class UsersThread extends DoThread {

        public UsersThread(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
            setName("UsersThread");
        }

        @Override
        public void run() {
            try {
                stop = false;
                working = true;
                usersThreadRun = System.currentTimeMillis();

                if (stop) {
                    return;
                }

                double latMin = (centerLat / 1e6) - ((spanLat / 1e6) / 2) - ((spanLat / 1e6) / 4);
                double latMax = (centerLat / 1e6) + ((spanLat / 1e6) / 2) + ((spanLat / 1e6) / 4);
                double lonMin = (centerLon / 1e6) - ((spanLon / 1e6) / 2) - ((spanLon / 1e6) / 4);
                double lonMax = (centerLon / 1e6) + ((spanLon / 1e6) / 2) + ((spanLon / 1e6) / 4);
                double llCache;

                if (latMin > latMax) {
                    llCache = latMax;
                    latMax = latMin;
                    latMin = llCache;
                }
                if (lonMin > lonMax) {
                    llCache = lonMax;
                    lonMax = lonMin;
                    lonMin = llCache;
                }

                users = cgBase.getGeocachersInViewport(Settings.getUsername(), latMin, latMax, lonMin, lonMax);

                if (stop) {
                    return;
                }

                if (displayUsersThread != null && displayUsersThread.isWorking()) {
                    displayUsersThread.stopIt();
                }
                displayUsersThread = new DisplayUsersThread(users, centerLat, centerLon, spanLat, spanLon);
                displayUsersThread.start();
            } finally {
                working = false;
            }
        }
    }

    /**
     * Thread to display users of Go 4 Cache started from {@link UsersThread}
     */
    private class DisplayUsersThread extends DoThread {

        private List<cgUser> users = null;

        public DisplayUsersThread(List<cgUser> usersIn, long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
            setName("DisplayUsersThread");
            users = usersIn;
        }

        @Override
        public void run() {
            try {
                stop = false;
                working = true;

                if (mapView == null || CollectionUtils.isEmpty(users)) {
                    return;
                }

                // display users
                List<OtherCachersOverlayItemImpl> items = new ArrayList<OtherCachersOverlayItemImpl>();

                int counter = 0;
                OtherCachersOverlayItemImpl item = null;

                for (cgUser userOne : users) {
                    if (stop) {
                        return;
                    }

                    if (userOne.coords == null) {
                        continue;
                    }

                    item = Settings.getMapFactory().getOtherCachersOverlayItemBase(activity, userOne);
                    items.add(item);

                    counter++;
                    if ((counter % 10) == 0) {
                        overlayOtherCachers.updateItems(items);
                        displayHandler.sendEmptyMessage(1);
                    }
                }

                overlayOtherCachers.updateItems(items);
            } finally {
                working = false;
            }
        }
    }

    /**
     * Thread to display one point. Started on opening if in single mode.
     */
    private class DisplayPointThread extends Thread {

        @Override
        public void run() {
            if (mapView == null || caches == null) {
                return;
            }

            if (coordsIntent != null) {
                cgCoord coord = new cgCoord();
                coord.type = "waypoint";
                coord.coords = coordsIntent;
                coord.name = "some place";

                coordinates.add(coord);
                CachesOverlayItemImpl item = Settings.getMapFactory().getCachesOverlayItem(coord, null);

                final int icon = waypointTypeIntent != null ? waypointTypeIntent.markerId : null;
                Drawable pin = null;
                if (iconsCache.containsKey(icon)) {
                    pin = iconsCache.get(icon);
                } else {
                    pin = getResources().getDrawable(icon);
                    pin.setBounds(0, 0, pin.getIntrinsicWidth(), pin.getIntrinsicHeight());
                    iconsCache.put(icon, pin);
                }
                item.setMarker(pin);

                overlayCaches.updateItems(item);
                displayHandler.sendEmptyMessage(1);

                cachesCnt = 1;
            } else {
                cachesCnt = 0;
            }

            displayHandler.sendEmptyMessage(0);
        }
    }

    /**
     * Abstract Base Class for the worker threads.
     */

    private abstract class DoThread extends Thread {

        protected boolean working = true;
        protected boolean stop = false;
        protected long centerLat = 0L;
        protected long centerLon = 0L;
        protected long spanLat = 0L;
        protected long spanLon = 0L;

        public DoThread(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            centerLat = centerLatIn;
            centerLon = centerLonIn;
            spanLat = spanLatIn;
            spanLon = spanLonIn;
        }

        public synchronized boolean isWorking() {
            return working;
        }

        public synchronized void stopIt() {
            stop = true;
        }
    }

    /**
     * get if map is loading something
     *
     * @return
     */
    private synchronized boolean isLoading() {
        boolean loading = false;

        if (loadThread != null && loadThread.isWorking()) {
            loading = true;
        } else if (downloadThread != null && downloadThread.isWorking()) {
            loading = true;
        } else if (displayThread != null && displayThread.isWorking()) {
            loading = true;
        }

        return loading;
    }

    /**
     * Thread to store the caches in the viewport. Started by Activity.
     */

    private class LoadDetails extends Thread {

        private Handler handler = null;
        private List<String> geocodes = null;
        private volatile boolean stop = false;
        private long last = 0L;

        public LoadDetails(Handler handlerIn, List<String> geocodesIn) {
            handler = handlerIn;
            geocodes = geocodesIn;
        }

        public void stopIt() {
            stop = true;
        }

        @Override
        public void run() {
            if (CollectionUtils.isEmpty(geocodes)) {
                return;
            }

            if (dir != null) {
                dir = app.removeDir();
            }
            if (geo != null) {
                geo = app.removeGeo();
            }

            for (String geocode : geocodes) {
                try {
                    if (stop) {
                        break;
                    }

                    if (!app.isOffline(geocode, null)) {
                        if ((System.currentTimeMillis() - last) < 1500) {
                            try {
                                int delay = 1000 + ((Double) (Math.random() * 1000)).intValue() - (int) (System.currentTimeMillis() - last);
                                if (delay < 0) {
                                    delay = 500;
                                }

                                sleep(delay);
                            } catch (Exception e) {
                                // nothing
                            }
                        }

                        if (stop) {
                            Log.i(Settings.tag, "Stopped storing process.");

                            break;
                        }

                        base.storeCache(app, activity, null, geocode, 1, handler);
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.LoadDetails.run: " + e.toString());
                } finally {
                    // one more cache over
                    detailProgress++;
                    handler.sendEmptyMessage(0);
                }

                yield();

                last = System.currentTimeMillis();
            }

            // we're done
            handler.sendEmptyMessage(1);
        }
    }

    // center map to desired location
    private void centerMap(final Geopoint coords) {
        if (coords == null) {
            return;
        }
        if (mapView == null) {
            return;
        }

        if (!alreadyCentered) {
            alreadyCentered = true;

            mapController.setCenter(makeGeoPoint(coords));
        } else {
            mapController.animateTo(makeGeoPoint(coords));
        }
    }

    // move map to view results of searchIdIntent
    private void centerMap(String geocodeCenter, String searchIdCenter, final Geopoint coordsCenter, int[] mapState) {

        if (!centered && mapState != null) {
            try {
                mapController.setCenter(Settings.getMapFactory().getGeoPointBase(new Geopoint(mapState[0] / 1.0e6, mapState[1] / 1.0e6)));
                mapController.setZoom(mapState[2]);
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && (geocodeCenter != null || searchIdIntent != null)) {
            try {
                List<Object> viewport = null;

                if (geocodeCenter != null) {
                    viewport = app.getBounds(geocodeCenter);
                } else {
                    viewport = app.getBounds(UUID.fromString(searchIdCenter));
                }

                if (viewport == null)
                    return;

                Integer cnt = (Integer) viewport.get(0);
                Integer minLat = null;
                Integer maxLat = null;
                Integer minLon = null;
                Integer maxLon = null;

                if (viewport.get(1) != null) {
                    minLat = (int) ((Double) viewport.get(1) * 1e6);
                }
                if (viewport.get(2) != null) {
                    maxLat = (int) ((Double) viewport.get(2) * 1e6);
                }
                if (viewport.get(3) != null) {
                    maxLon = (int) ((Double) viewport.get(3) * 1e6);
                }
                if (viewport.get(4) != null) {
                    minLon = (int) ((Double) viewport.get(4) * 1e6);
                }

                if (cnt == null || cnt <= 0 || minLat == null || maxLat == null || minLon == null || maxLon == null) {
                    return;
                }

                int centerLat = 0;
                int centerLon = 0;

                if ((Math.abs(maxLat) - Math.abs(minLat)) != 0) {
                    centerLat = minLat + ((maxLat - minLat) / 2);
                } else {
                    centerLat = maxLat;
                }
                if ((Math.abs(maxLon) - Math.abs(minLon)) != 0) {
                    centerLon = minLon + ((maxLon - minLon) / 2);
                } else {
                    centerLon = maxLon;
                }

                if (cnt > 0) {
                    mapController.setCenter(Settings.getMapFactory().getGeoPointBase(new Geopoint(centerLat, centerLon)));
                    if (Math.abs(maxLat - minLat) != 0 && Math.abs(maxLon - minLon) != 0) {
                        mapController.zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
                    }
                }
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && coordsCenter != null) {
            try {
                mapController.setCenter(makeGeoPoint(coordsCenter));
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        }
    }

    // switch My Location button image
    private void switchMyLocationButton() {
        if (followMyLocation) {
            myLocSwitch.setImageResource(R.drawable.actionbar_mylocation_on);
            myLocationInMiddle();
        } else {
            myLocSwitch.setImageResource(R.drawable.actionbar_mylocation_off);
        }
    }

    // set my location listener
    private class MyLocationListener implements View.OnClickListener {
        public void onClick(View view) {
            followMyLocation = !followMyLocation;
            switchMyLocationButton();
        }
    }

    @Override
    public void onDrag() {
        if (followMyLocation) {
            followMyLocation = false;
            switchMyLocationButton();
        }
    }

    // make geopoint
    private static GeoPointImpl makeGeoPoint(final Geopoint coords) {
        return Settings.getMapFactory().getGeoPointBase(coords);
    }

    // close activity and open homescreen
    public void goHome(View view) {
        ActivityMixin.goHome(activity);
    }

    // open manual entry
    public void goManual(View view) {
        ActivityMixin.goManual(activity, "c:geo-live-map");
    }

    @Override
    public View makeView() {
        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ScaleType.CENTER);
        imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        return imageView;
    }

    public static void startActivitySearch(final Activity fromActivity, final UUID searchId, final String title, boolean detail) {
        Intent mapIntent = new Intent(fromActivity, Settings.getMapFactory().getMapClass());
        mapIntent.putExtra(EXTRAS_DETAIL, detail);
        mapIntent.putExtra(EXTRAS_SEARCHID, searchId.toString());
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(CGeoMap.EXTRAS_MAP_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityLiveMap(final Context context) {
        context.startActivity(new Intent(context, Settings.getMapFactory().getMapClass()));
    }

    public static void startActivityCoords(final Context context, final Geopoint coords, final WaypointType type) {
        Intent mapIntent = new Intent(context, Settings.getMapFactory().getMapClass());
        mapIntent.putExtra(EXTRAS_DETAIL, false);
        mapIntent.putExtra(EXTRAS_LATITUDE, coords.getLatitude());
        mapIntent.putExtra(EXTRAS_LONGITUDE, coords.getLongitude());
        if (type != null) {
            mapIntent.putExtra(EXTRAS_WPTTYPE, type.id);
        }
        context.startActivity(mapIntent);
    }

    public static void startActivityGeoCode(final Context context, final String geocode) {
        Intent mapIntent = new Intent(context, Settings.getMapFactory().getMapClass());
        mapIntent.putExtra(EXTRAS_DETAIL, false);
        mapIntent.putExtra(EXTRAS_GEOCODE, geocode);
        mapIntent.putExtra(EXTRAS_MAP_TITLE, geocode);
        context.startActivity(mapIntent);
    }
}
