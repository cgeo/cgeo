package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.AbstractDialogFragment;
import cgeo.geocaching.AbstractDialogFragment.TargetInfo;
import cgeo.geocaching.CachePopup;
import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.GCMap;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CGeoMap.MapMode;
import cgeo.geocaching.maps.LivemapStrategy;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.maps.mapsforge.v6.caches.CachesBundle;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.maps.mapsforge.v6.layers.HistoryLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.NavigationLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.PositionLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.TapHandlerLayer;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import butterknife.ButterKnife;
import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.graphics.AndroidResourceBitmap;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.xmlpull.v1.XmlPullParserException;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

@SuppressLint("ClickableViewAccessibility")
public class NewMap extends AbstractActionBarActivity {

    private MfMapView mapView;
    private TileCache tileCache;
    private TileRendererLayer tileRendererLayer;
    private HistoryLayer historyLayer;
    private PositionLayer positionLayer;
    private NavigationLayer navigationLayer;
    private CachesBundle caches;
    private final MapHandlers mapHandlers = new MapHandlers(new TapHandler(this), new DisplayHandler(this), new ShowProgressHandler(this));

    private DistanceView distanceView;

    private String mapTitle;
    private String geocodeIntent;
    private Geopoint coordsIntent;
    private SearchResult searchIntent;
    private WaypointType waypointTypeIntent = null;
    private MapState mapStateIntent = null;
    private ArrayList<Location> trailHistory = null;

    private String targetGeocode = null;
    private Geopoint lastNavTarget = null;
    private final Queue<String> popupGeocodes = new ConcurrentLinkedQueue<>();

    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    /**
     * initialization with an empty subscription to make static code analysis tools more happy
     */
    private Subscription resumeSubscription = Subscriptions.empty();
    private CheckBox myLocSwitch;
    private MapMode mapMode;
    private boolean isLiveEnabled = false;
    private TargetView targetView;

    private static boolean followMyLocation;

    private static final String BUNDLE_MAP_STATE = "mapState";
    private static final String BUNDLE_TRAIL_HISTORY = "trailHistory";

    // Handler messages
    public static final int UPDATE_TITLE = 0;
    public static final int INVALIDATE_MAP = 1;
    public static final int HIDE_PROGRESS = 0;
    public static final int SHOW_PROGRESS = 1;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(this.getApplication());

        // Get parameters from the intent
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mapMode = (MapMode) extras.get(Intents.EXTRA_MAP_MODE);
            isLiveEnabled = extras.getBoolean(Intents.EXTRA_LIVE_ENABLED, false);
            geocodeIntent = extras.getString(Intents.EXTRA_GEOCODE);
            searchIntent = extras.getParcelable(Intents.EXTRA_SEARCH);
            coordsIntent = extras.getParcelable(Intents.EXTRA_COORDS);
            waypointTypeIntent = WaypointType.findById(extras.getString(Intents.EXTRA_WPTTYPE));
            mapTitle = extras.getString(Intents.EXTRA_TITLE);
            mapStateIntent = extras.getParcelable(Intents.EXTRA_MAPSTATE);
        } else {
            mapMode = MapMode.LIVE;
            isLiveEnabled = Settings.isLiveMap();
        }

        if (StringUtils.isBlank(mapTitle)) {
            mapTitle = res.getString(R.string.map_map);
        }

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            mapStateIntent = savedInstanceState.getParcelable(BUNDLE_MAP_STATE);
            trailHistory = savedInstanceState.getParcelableArrayList(BUNDLE_TRAIL_HISTORY);
            followMyLocation = mapStateIntent.followsMyLocation();
        } else {
            followMyLocation = followMyLocation && mapMode == MapMode.LIVE;
        }

        ActivityMixin.onCreate(this, true);

        // set layout
        ActivityMixin.setTheme(this);

        setContentView(R.layout.map_mapsforge_v6);
        setTitle();

        // initialize map
        mapView = (MfMapView) findViewById(R.id.mfmapv5);

        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
        mapView.getMapZoomControls().setZoomLevelMax((byte) 20);

        // create a tile cache of suitable size
        tileCache = AndroidUtil.createTileCache(this, "mapcache", mapView.getModel().displayModel.getTileSize(), 1f, this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        // attach drag handler
        final DragHandler dragHandler = new DragHandler(this);
        mapView.setOnMapDragListener(dragHandler);

        // prepare initial settings of mapview
        if (mapStateIntent != null) {
            this.mapView.getModel().mapViewPosition.setCenter(mapStateIntent.getCenter());
            this.mapView.getModel().mapViewPosition.setZoomLevel((byte) mapStateIntent.getZoomLevel());
            this.targetGeocode = mapStateIntent.getTargetGeocode();
            this.lastNavTarget = mapStateIntent.getLastNavTarget();
            this.isLiveEnabled = mapStateIntent.isLiveEnabled();
        } else if (searchIntent != null) {
            final Viewport viewport = DataStore.getBounds(searchIntent.getGeocodes());

            if (viewport != null) {
                mapView.zoomToViewport(viewport);
            }
        } else if (StringUtils.isNotEmpty(geocodeIntent)) {
            final Viewport viewport = DataStore.getBounds(geocodeIntent);

            if (viewport != null) {
                mapView.zoomToViewport(viewport);
            }
            targetGeocode = geocodeIntent;
        } else if (coordsIntent != null) {
            mapView.zoomToViewport(new Viewport(coordsIntent, 0, 0));
        } else {
            mapView.zoomToViewport(new Viewport(Settings.getMapCenter().getCoords(), 0, 0));
        }
        prepareFilterBar();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);

        MapProviderFactory.addMapviewMenuItems(menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            /* if we have an Actionbar find the my position toggle */
            final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
            myLocSwitch = new CheckBox(this);
            myLocSwitch.setButtonDrawable(R.drawable.ic_menu_myposition);
            item.setActionView(myLocSwitch);
            initMyLocationSwitchButton(myLocSwitch);
        } else {
            // Already on the fake Actionbar
            menu.removeItem(R.id.menu_toggle_mypos);
        }

        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (final MapSource mapSource : MapProviderFactory.getMapSources()) {
            final MenuItem menuItem = menu.findItem(mapSource.getNumericalId());
            if (menuItem != null) {
                if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource) {
                    menuItem.setVisible(mapSource.isAvailable());
                } else {
                    menuItem.setVisible(false);
                }
            }
        }

        try {
            final MenuItem itemMapLive = menu.findItem(R.id.menu_map_live);
            if (isLiveEnabled) {
                itemMapLive.setTitle(res.getString(R.string.map_live_disable));
            } else {
                itemMapLive.setTitle(res.getString(R.string.map_live_enable));
            }
            itemMapLive.setVisible(coordsIntent == null);

            //TODO: menu_store_caches
            menu.findItem(R.id.menu_store_caches).setVisible(false);
            //final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
            //menu.findItem(R.id.menu_store_caches).setVisible(!isLoading() && CollectionUtils.isNotEmpty(geocodesInViewport) && new SearchResult(geocodesInViewport).hasUnsavedCaches());

            menu.findItem(R.id.menu_mycaches_mode).setChecked(Settings.isExcludeMyCaches());
            menu.findItem(R.id.menu_disabled_mode).setChecked(Settings.isExcludeDisabledCaches());
            menu.findItem(R.id.menu_direction_line).setChecked(Settings.isMapDirection());
            //TODO: circles            menu.findItem(R.id.menu_circle_mode).setChecked(this.searchOverlay.getCircles());
            menu.findItem(R.id.menu_circle_mode).setVisible(false);
            menu.findItem(R.id.menu_trail_mode).setChecked(Settings.isMapTrail());

            menu.findItem(R.id.menu_theme_mode).setVisible(true);

            //TODO: menu_as_list
            menu.findItem(R.id.menu_as_list).setVisible(false);
            //menu.findItem(R.id.menu_as_list).setVisible(!isLoading() && caches.size() > 1);

            menu.findItem(R.id.submenu_strategy).setVisible(isLiveEnabled);

            switch (Settings.getLiveMapStrategy()) {
                case FASTEST:
                    menu.findItem(R.id.menu_strategy_fastest).setChecked(true);
                    break;
                case FAST:
                    menu.findItem(R.id.menu_strategy_fast).setChecked(true);
                    break;
                case AUTO:
                    menu.findItem(R.id.menu_strategy_auto).setChecked(true);
                    break;
                default: // DETAILED
                    menu.findItem(R.id.menu_strategy_detailed).setChecked(true);
                    break;
            }

            menu.findItem(R.id.menu_hint).setVisible(mapMode == MapMode.SINGLE);
            menu.findItem(R.id.menu_compass).setVisible(mapMode == MapMode.SINGLE);

        } catch (final RuntimeException e) {
            Log.e("NewMap.onPrepareOptionsMenu", e);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                ActivityMixin.navigateUp(this);
                return true;
            case R.id.menu_trail_mode:
                Settings.setMapTrail(!Settings.isMapTrail());
                historyLayer.requestRedraw();
                ActivityMixin.invalidateOptionsMenu(this);
                return true;
            case R.id.menu_direction_line:
                Settings.setMapDirection(!Settings.isMapDirection());
                navigationLayer.requestRedraw();
                ActivityMixin.invalidateOptionsMenu(this);
                return true;
            case R.id.menu_map_live:
                isLiveEnabled = !isLiveEnabled;
                if (mapMode == MapMode.LIVE) {
                    Settings.setLiveMap(isLiveEnabled);
                }
                caches.handleLiveLayers(isLiveEnabled);
                ActivityMixin.invalidateOptionsMenu(this);
                if (mapMode != MapMode.SINGLE) {
                    mapTitle = StringUtils.EMPTY;
                } else {
                    // reset target cache on single mode map
                    targetGeocode = geocodeIntent;
                }
                return true;
            case R.id.menu_store_caches:
                //TODO: menu_store_caches
                return true;
            case R.id.menu_circle_mode:
                //                overlayCaches.switchCircles();
                //                mapView.repaintRequired(overlayCaches);
                //                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_mycaches_mode:
                Settings.setExcludeMine(!Settings.isExcludeMyCaches());
                caches.invalidate();
                ActivityMixin.invalidateOptionsMenu(this);
                if (!Settings.isExcludeMyCaches()) {
                    Tile.cache.clear();
                }
                return true;
            case R.id.menu_disabled_mode:
                Settings.setExcludeDisabled(!Settings.isExcludeDisabledCaches());
                caches.invalidate();
                ActivityMixin.invalidateOptionsMenu(this);
                if (!Settings.isExcludeDisabledCaches()) {
                    Tile.cache.clear();
                }
                return true;
            case R.id.menu_theme_mode:
                selectMapTheme();
                return true;
            case R.id.menu_as_list: {
                //TODO: menu_as_list
                //CacheListActivity.startActivityMap(activity, new SearchResult(getGeocodesForCachesInViewport()));
                return true;
            }
            case R.id.menu_strategy_fastest: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.FASTEST);
                return true;
            }
            case R.id.menu_strategy_fast: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.FAST);
                return true;
            }
            case R.id.menu_strategy_auto: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.AUTO);
                return true;
            }
            case R.id.menu_strategy_detailed: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.DETAILED);
                return true;
            }
            case R.id.menu_hint:
                menuShowHint();
                return true;
            case R.id.menu_compass:
                menuCompass();
                return true;
            default:
                final MapSource mapSource = MapProviderFactory.getMapSource(id);
                if (mapSource != null) {
                    item.setChecked(true);
                    setMapSource(mapSource);
                    return true;
                }
        }
        return false;
    }

    private void menuCompass() {
        final Geocache cache = getCurrentTargetCache();
        if (cache != null) {
            CompassActivity.startActivityCache(this, cache);
        }
    }

    private void menuShowHint() {
        final Geocache cache = getCurrentTargetCache();
        if (cache != null) {
            cache.showHintToast(this);
        }
    }

    private void prepareFilterBar() {
        // show the filter warning bar if the filter is set
        if (Settings.getCacheType() != CacheType.ALL) {
            final String cacheType = Settings.getCacheType().getL10n();
            final TextView filterTitleView = ButterKnife.findById(this, R.id.filter_text);
            filterTitleView.setText(cacheType);
            findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.filter_bar).setVisibility(View.GONE);
        }
    }

    /**
     * @param view
     *            Not used here, required by layout
     */
    public void showFilterMenu(final View view) {
        // do nothing, the filter bar only shows the global filter
    }

    private void selectMapTheme() {

        final File[] themeFiles = Settings.getMapThemeFiles();

        String currentTheme = StringUtils.EMPTY;
        final String currentThemePath = Settings.getCustomRenderThemeFilePath();
        if (StringUtils.isNotEmpty(currentThemePath)) {
            final File currentThemeFile = new File(currentThemePath);
            currentTheme = currentThemeFile.getName();
        }

        final List<String> names = new ArrayList<>();
        names.add(res.getString(R.string.map_theme_builtin));
        int currentItem = 0;
        for (final File file : themeFiles) {
            if (currentTheme.equalsIgnoreCase(file.getName())) {
                currentItem = names.size();
            }
            names.add(file.getName());
        }

        final int selectedItem = currentItem;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.map_theme_select);

        builder.setSingleChoiceItems(names.toArray(new String[names.size()]), selectedItem, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int newItem) {
                if (newItem != selectedItem) {
                    // Adjust index because of <default> selection
                    if (newItem > 0) {
                        Settings.setCustomRenderThemeFile(themeFiles[newItem - 1].getPath());
                    } else {
                        Settings.setCustomRenderThemeFile(StringUtils.EMPTY);
                    }
                    setMapTheme();
                }
                dialog.cancel();
            }
        });

        builder.show();
    }

    protected void setMapTheme() {

        if (tileRendererLayer == null) {
            return;
        }

        final String themePath = Settings.getCustomRenderThemeFilePath();

        if (StringUtils.isEmpty(themePath)) {
            tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        } else {
            try {
                final XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(new File(themePath));
                // Validate the theme file
                RenderThemeHandler.getRenderTheme(AndroidGraphicFactory.INSTANCE, new DisplayModel(), xmlRenderTheme);
                tileRendererLayer.setXmlRenderTheme(xmlRenderTheme);
            } catch (final IOException e) {
                Log.w("Failed to set render theme", e);
                ActivityMixin.showApplicationToast(getString(R.string.err_rendertheme_file_unreadable));
                tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
            } catch (final XmlPullParserException e) {
                Log.w("render theme invalid", e);
                ActivityMixin.showApplicationToast(getString(R.string.err_rendertheme_invalid));
                tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
            }
        }
        tileCache.destroy();
        tileRendererLayer.requestRedraw();
    }

    private void setMapSource(@NonNull final MapSource mapSource) {
        // Update mapsource in settings
        Settings.setMapSource(mapSource);

        // Create new render layer, if mapfile exists
        final TileRendererLayer oldLayer = this.tileRendererLayer;
        final File mapFile = NewMap.getMapFile();
        if (mapFile != null && mapFile.exists()) {
            final TileRendererLayer newLayer = new TileRendererLayer(tileCache, new MapFile(mapFile), this.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);

            // Exchange layer
            final Layers layers = this.mapView.getLayerManager().getLayers();
            final int index = layers.indexOf(oldLayer) + 1;
            layers.add(index, newLayer);
            this.tileRendererLayer = newLayer;
            this.setMapTheme();
        } else {
            this.tileRendererLayer = null;
        }

        // Cleanup
        this.mapView.getLayerManager().getLayers().remove(oldLayer);
        oldLayer.onDestroy();
        tileCache.destroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        initializeLayers();
    }

    private void initializeLayers() {

        // tile renderer layer (if map file is defined)
        final File mapFile = NewMap.getMapFile();
        if (mapFile != null && mapFile.exists()) {
            this.tileRendererLayer = new TileRendererLayer(tileCache, new MapFile(mapFile), this.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            this.setMapTheme();

            // only once a layer is associated with a mapView the rendering starts
            this.mapView.getLayerManager().getLayers().add(this.tileRendererLayer);
        }

        // History Layer
        this.historyLayer = new HistoryLayer(trailHistory);
        this.mapView.getLayerManager().getLayers().add(this.historyLayer);

        // NavigationLayer
        Geopoint navTarget = lastNavTarget;
        if (navTarget == null) {
            navTarget = this.coordsIntent;
            if (navTarget == null && StringUtils.isNotEmpty(this.geocodeIntent)) {
                final Viewport bounds = DataStore.getBounds(this.geocodeIntent);
                if (bounds != null) {
                    navTarget = bounds.center;
                }
            }
        }
        this.navigationLayer = new NavigationLayer(navTarget);
        this.mapView.getLayerManager().getLayers().add(this.navigationLayer);

        // TapHandler
        final TapHandlerLayer tapHandlerLayer = new TapHandlerLayer(this.mapHandlers.getTapHandler());
        this.mapView.getLayerManager().getLayers().add(tapHandlerLayer);

        // Caches bundle
        if (this.searchIntent != null) {
            this.caches = new CachesBundle(this.searchIntent, this.mapView, this.mapHandlers);
        } else if (StringUtils.isNotEmpty(this.geocodeIntent)) {
            this.caches = new CachesBundle(this.geocodeIntent, this.mapView, this.mapHandlers);
        } else if (this.coordsIntent != null) {
            this.caches = new CachesBundle(coordsIntent, waypointTypeIntent, this.mapView, this.mapHandlers);
        } else {
            caches = new CachesBundle(this.mapView, this.mapHandlers);
        }

        // Live map
        caches.handleLiveLayers(isLiveEnabled);

        // Position layer
        this.positionLayer = new PositionLayer();
        this.mapView.getLayerManager().getLayers().add(positionLayer);

        //Distance view
        this.distanceView = new DistanceView(navTarget, (TextView) findViewById(R.id.distance));

        //Target view
        this.targetView = new TargetView((TextView) findViewById(R.id.target), StringUtils.EMPTY, StringUtils.EMPTY);
        final Geocache target = getCurrentTargetCache();
        if (target != null) {
            targetView.setTarget(target.getGeocode(), target.getName());
        }

        this.resumeSubscription = Subscriptions.from(this.geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR));
    }

    @Override
    public void onPause() {

        savePrefs();

        super.onPause();
    }

    @Override
    protected void onStop() {

        terminateLayers();

        super.onStop();
    }

    private void terminateLayers() {

        this.resumeSubscription.unsubscribe();
        this.resumeSubscription = Subscriptions.empty();

        this.caches.onDestroy();
        this.caches = null;

        this.mapView.getLayerManager().getLayers().remove(this.positionLayer);
        this.positionLayer = null;
        this.mapView.getLayerManager().getLayers().remove(this.navigationLayer);
        this.navigationLayer = null;
        this.mapView.getLayerManager().getLayers().remove(this.historyLayer);
        this.historyLayer = null;

        if (this.tileRendererLayer != null) {
            this.mapView.getLayerManager().getLayers().remove(this.tileRendererLayer);
            this.tileRendererLayer.onDestroy();
            this.tileRendererLayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        this.tileCache.destroy();
        this.mapView.getModel().mapViewPosition.destroy();
        this.mapView.destroy();
        AndroidResourceBitmap.clearResourceBitmaps();

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        final MapState state = prepareMapState();
        outState.putParcelable(BUNDLE_MAP_STATE, state);
        if (historyLayer != null) {
            outState.putParcelableArrayList(BUNDLE_TRAIL_HISTORY, historyLayer.getHistory());
        }
    }

    private MapState prepareMapState() {
        return new MapState(mapView.getModel().mapViewPosition.getCenter(), mapView.getModel().mapViewPosition.getZoomLevel(), followMyLocation, false, targetGeocode, lastNavTarget, isLiveEnabled);
    }

    private void centerMap(final Geopoint geopoint) {
        mapView.getModel().mapViewPosition.setCenter(new LatLong(geopoint.getLatitude(), geopoint.getLongitude()));
    }

    public Location getCoordinates() {
        final LatLong center = mapView.getModel().mapViewPosition.getCenter();
        final Location loc = new Location("newmap");
        loc.setLatitude(center.latitude);
        loc.setLongitude(center.longitude);
        return loc;
    }

    private void initMyLocationSwitchButton(final CheckBox locSwitch) {
        myLocSwitch = locSwitch;
        /*
         * TODO: Switch back to ImageSwitcher for animations?
         * myLocSwitch.setFactory(this);
         * myLocSwitch.setInAnimation(activity, android.R.anim.fade_in);
         * myLocSwitch.setOutAnimation(activity, android.R.anim.fade_out);
         */
        myLocSwitch.setOnClickListener(new MyLocationListener(this));
        switchMyLocationButton();
    }

    // switch My Location button image
    private void switchMyLocationButton() {
        myLocSwitch.setChecked(followMyLocation);
        if (followMyLocation) {
            myLocationInMiddle(Sensors.getInstance().currentGeo());
        }
    }

    public void showAddWaypoint(final LatLong tapLatLong) {
        final Geocache cache = getCurrentTargetCache();
        if (cache != null) {
            EditWaypointActivity.startActivityAddWaypoint(this, cache, new Geopoint(tapLatLong.latitude, tapLatLong.longitude));
        }
    }

    // set my location listener
    private static class MyLocationListener implements View.OnClickListener {

        @NonNull
        private final WeakReference<NewMap> mapRef;

        private void onFollowMyLocationClicked() {
            followMyLocation = !followMyLocation;
            final NewMap map = mapRef.get();
            if (map != null) {
                map.switchMyLocationButton();
            }
        }

        MyLocationListener(@NonNull final NewMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void onClick(final View view) {
            onFollowMyLocationClicked();
        }
    }

    // Set center of map to my location if appropriate.
    private void myLocationInMiddle(final GeoData geo) {
        if (followMyLocation) {
            centerMap(geo.getCoords());
        }
    }

    @Nullable
    private static File getMapFile() {
        final String mapFileName = Settings.getMapFile();
        if (StringUtils.isNotEmpty(mapFileName)) {
            return new File(mapFileName);
        }

        return null;
    }

    private static final class DisplayHandler extends Handler {

        @NonNull
        private final WeakReference<NewMap> mapRef;

        DisplayHandler(@NonNull final NewMap map) {
            this.mapRef = new WeakReference<>(map);
        }

        @Override
        public void handleMessage(final Message msg) {
            final NewMap map = mapRef.get();
            if (map == null) {
                return;
            }

            final int what = msg.what;
            switch (what) {
                case UPDATE_TITLE:
                    map.setTitle();
                    map.setSubtitle();

                    break;
                case INVALIDATE_MAP:
                    map.mapView.repaint();
                    break;

                default:
                    break;
            }
        }

    }

    private void setTitle() {
        final String title = calculateTitle();

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @NonNull
    private String calculateTitle() {
        if (isLiveEnabled) {
            return res.getString(R.string.map_live);
        }
        if (mapMode == MapMode.SINGLE) {
            final Geocache cache = getSingleModeCache();
            if (cache != null) {
                return cache.getName();
            }
        }
        return StringUtils.defaultIfEmpty(mapTitle, res.getString(R.string.map_map));
    }

    private void setSubtitle() {
        final String subtitle = calculateSubtitle();
        if (StringUtils.isEmpty(subtitle)) {
            return;
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @NonNull
    private String calculateSubtitle() {
        if (!isLiveEnabled && mapMode == MapMode.SINGLE) {
            final Geocache cache = getSingleModeCache();
            if (cache != null) {
                return Formatter.formatMapSubtitle(cache);
            }
        }

        // count caches in the sub title
        final int visible = countVisibleCaches();
        final int total = countTotalCaches();

        final StringBuilder subtitle = new StringBuilder();
        if (total != 0) {
            if (visible != total && Settings.isDebug()) {
                subtitle.append(visible).append('/').append(res.getQuantityString(R.plurals.cache_counts, total, total));
            } else {
                subtitle.append(res.getQuantityString(R.plurals.cache_counts, visible, visible));
            }
        }

        //        if (Settings.isDebug() && lastSearchResult != null && StringUtils.isNotBlank(lastSearchResult.getUrl())) {
        //            subtitle.append(" [").append(lastSearchResult.getUrl()).append(']');
        //        }

        return subtitle.toString();
    }

    private int countVisibleCaches() {
        return caches != null ? caches.getVisibleItemsCount() : 0;
    }

    private int countTotalCaches() {
        return caches != null ? caches.getItemsCount() : 0;
    }

    /** Updates the progress. */
    private static final class ShowProgressHandler extends Handler {
        private int counter = 0;

        @NonNull private final WeakReference<NewMap> mapRef;

        ShowProgressHandler(@NonNull final NewMap map) {
            this.mapRef = new WeakReference<>(map);
        }

        @Override
        public void handleMessage(final Message msg) {
            final int what = msg.what;

            if (what == HIDE_PROGRESS) {
                if (--counter == 0) {
                    showProgress(false);
                }
            } else if (what == SHOW_PROGRESS) {
                showProgress(true);
                counter++;
            }
        }

        private void showProgress(final boolean show) {
            final NewMap map = mapRef.get();
            if (map == null) {
                return;
            }
            map.setProgressBarIndeterminateVisibility(show);
        }

    }

    @NonNull
    public static Intent getLiveMapIntent(final Activity fromActivity) {
        return new Intent(fromActivity, NewMap.class).putExtra(Intents.EXTRA_MAP_MODE, MapMode.LIVE).putExtra(Intents.EXTRA_LIVE_ENABLED, Settings.isLiveMap());
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords, final WaypointType type, final String title) {
        final Intent mapIntent = new Intent(fromActivity, NewMap.class);
        mapIntent.putExtra(Intents.EXTRA_MAP_MODE, MapMode.COORDS);
        mapIntent.putExtra(Intents.EXTRA_LIVE_ENABLED, false);
        mapIntent.putExtra(Intents.EXTRA_COORDS, coords);
        if (type != null) {
            mapIntent.putExtra(Intents.EXTRA_WPTTYPE, type.id);
        }
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(Intents.EXTRA_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        final Intent mapIntent = new Intent(fromActivity, NewMap.class);
        mapIntent.putExtra(Intents.EXTRA_MAP_MODE, MapMode.SINGLE);
        mapIntent.putExtra(Intents.EXTRA_LIVE_ENABLED, false);
        mapIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        mapIntent.putExtra(Intents.EXTRA_TITLE, geocode);
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title) {
        final Intent mapIntent = new Intent(fromActivity, NewMap.class);
        mapIntent.putExtra(Intents.EXTRA_SEARCH, search);
        mapIntent.putExtra(Intents.EXTRA_MAP_MODE, MapMode.LIST);
        mapIntent.putExtra(Intents.EXTRA_LIVE_ENABLED, false);
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(Intents.EXTRA_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    // class: update location
    private static class UpdateLoc extends GeoDirHandler {
        // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

        // minimum time in milliseconds between position overlay updates
        private static final long MIN_UPDATE_INTERVAL = 500;
        // minimum change of heading in grad for position overlay update
        private static final float MIN_HEADING_DELTA = 15f;
        // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
        private static final float MIN_LOCATION_DELTA = 0.01f;

        @NonNull
        Location currentLocation = Sensors.getInstance().currentGeo();
        float currentHeading;

        private long timeLastPositionOverlayCalculation = 0;
        /**
         * weak reference to the outer class
         */
        @NonNull private final WeakReference<NewMap> mapRef;

        UpdateLoc(@NonNull final NewMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void updateGeoDir(@NonNull final GeoData geo, final float dir) {
            currentLocation = geo;
            currentHeading = AngleUtils.getDirectionNow(dir);
            repaintPositionOverlay();
        }

        @NonNull
        public Location getCurrenLocation() {
            return currentLocation;
        }

        /**
         * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
         */
        void repaintPositionOverlay() {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > timeLastPositionOverlayCalculation + MIN_UPDATE_INTERVAL) {
                timeLastPositionOverlayCalculation = currentTimeMillis;

                try {
                    final NewMap map = mapRef.get();
                    if (map != null) {
                        final boolean needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy();
                        final boolean needsRepaintForHeading = needsRepaintForHeading();

                        if (needsRepaintForDistanceOrAccuracy && NewMap.followMyLocation) {
                            map.centerMap(new Geopoint(currentLocation));
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {

                            map.historyLayer.setCoordinates(currentLocation);
                            map.navigationLayer.setCoordinates(currentLocation);
                            map.distanceView.setCoordinates(currentLocation);
                            map.positionLayer.setCoordinates(currentLocation);
                            map.positionLayer.setHeading(currentHeading);
                            map.positionLayer.requestRedraw();
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.w("Failed to update location", e);
                }
            }
        }

        boolean needsRepaintForHeading() {
            final NewMap map = mapRef.get();
            if (map == null) {
                return false;
            }
            return Math.abs(AngleUtils.difference(currentHeading, map.positionLayer.getHeading())) > MIN_HEADING_DELTA;
        }

        boolean needsRepaintForDistanceOrAccuracy() {
            final NewMap map = mapRef.get();
            if (map == null) {
                return false;
            }
            final Location lastLocation = map.getCoordinates();

            float dist = Float.MAX_VALUE;
            if (lastLocation != null) {
                if (lastLocation.getAccuracy() != currentLocation.getAccuracy()) {
                    return true;
                }
                dist = currentLocation.distanceTo(lastLocation);
            }

            final float[] mapDimension = new float[1];
            if (map.mapView.getWidth() < map.mapView.getHeight()) {
                final double span = map.mapView.getLongitudeSpan() / 1e6;
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude() + span, mapDimension);
            } else {
                final double span = map.mapView.getLatitudeSpan() / 1e6;
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude() + span, currentLocation.getLongitude(), mapDimension);
            }

            return dist > (mapDimension[0] * MIN_LOCATION_DELTA);
        }
    }

    private static class DragHandler implements OnMapDragListener {

        @NonNull
        private final WeakReference<NewMap> mapRef;

        DragHandler(@NonNull final NewMap parent) {
            mapRef = new WeakReference<>(parent);
        }

        @Override
        public void onDrag() {
            final NewMap map = mapRef.get();
            if (map != null && NewMap.followMyLocation) {
                NewMap.followMyLocation = false;
                map.switchMyLocationButton();
            }
        }
    }

    public void showSelection(@NonNull final ArrayList<GeoitemRef> items) {
        if (items.isEmpty()) {
            return;
        }

        if (items.size() == 1) {
            showPopup(items.get(0));
            return;
        }
        try {
            final ArrayList<GeoitemRef> sorted = new ArrayList<>(items);
            Collections.sort(sorted, GeoitemRef.NAME_COMPARATOR);

            final LayoutInflater inflater = LayoutInflater.from(this);
            final ListAdapter adapter = new ArrayAdapter<GeoitemRef>(this, R.layout.cacheslist_item_select, sorted) {
                @Override
                public View getView(final int position, final View convertView, final ViewGroup parent) {

                    final View view = convertView == null ? inflater.inflate(R.layout.cacheslist_item_select, parent, false) : convertView;
                    final TextView tv = (TextView) view.findViewById(R.id.text);

                    final GeoitemRef item = getItem(position);
                    tv.setText(item.getName());

                    //Put the image on the TextView
                    tv.setCompoundDrawablesWithIntrinsicBounds(item.getMarkerId(), 0, 0, 0);

                    final TextView infoView = (TextView) view.findViewById(R.id.info);
                    infoView.setText(item.getItemCode());

                    return view;
                }
            };

            new AlertDialog.Builder(this).setTitle(res.getString(R.string.map_select_multiple_items)).setAdapter(adapter, new SelectionClickListener(sorted)).show();

        } catch (final NotFoundException e) {
            Log.e("NewMap.showSelection", e);
        }
    }

    private class SelectionClickListener implements DialogInterface.OnClickListener {

        @NonNull
        private final ArrayList<GeoitemRef> items;

        SelectionClickListener(@NonNull final ArrayList<GeoitemRef> items) {
            this.items = items;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            if (which >= 0 && which < items.size()) {
                final GeoitemRef item = items.get(which);
                showPopup(item);
            }
        }

    }

    private void showPopup(final GeoitemRef item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return;
        }

        try {
            if (item.getType() == CoordinatesType.CACHE) {
                final Geocache cache = DataStore.loadCache(item.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    final RequestDetailsThread requestDetailsThread = new RequestDetailsThread(cache, this);
                    requestDetailsThread.start();
                    return;
                }
                return;
            }

            if (item.getType() == CoordinatesType.WAYPOINT && item.getId() >= 0) {
                popupGeocodes.add(item.getGeocode());
                WaypointPopup.startActivityAllowTarget(this, item.getId(), item.getGeocode());
            }

        } catch (final NotFoundException e) {
            Log.e("NewMap.showPopup", e);
        }
    }

    @Nullable
    private Geocache getSingleModeCache() {
        if (StringUtils.isNotBlank(geocodeIntent)) {
            return DataStore.loadCache(geocodeIntent, LoadFlags.LOAD_CACHE_OR_DB);
        }

        return null;
    }

    @Nullable
    private Geocache getCurrentTargetCache() {
        if (StringUtils.isNotBlank(targetGeocode)) {
            return DataStore.loadCache(targetGeocode, LoadFlags.LOAD_CACHE_OR_DB);
        }

        return null;
    }

    private void savePrefs() {
        Settings.setMapZoom(MapMode.SINGLE, mapView.getMapZoomLevel());
        Settings.setMapCenter(new MapsforgeGeoPoint(mapView.getModel().mapViewPosition.getCenter()));
    }

    private static class RequestDetailsThread extends Thread {

        @NonNull private final Geocache cache;
        @NonNull private final WeakReference<NewMap> map;

        RequestDetailsThread(@NonNull final Geocache cache, @NonNull final NewMap map) {
            this.cache = cache;
            this.map = new WeakReference<>(map);
        }

        public boolean requestRequired() {
            return CacheType.UNKNOWN == cache.getType() || cache.getDifficulty() == 0;
        }

        @Override
        public void run() {
            final NewMap map = this.map.get();
            if (map == null) {
                return;
            }
            if (requestRequired()) {
                /* final SearchResult search = */GCMap.searchByGeocodes(Collections.singleton(cache.getGeocode()));
            }
            map.popupGeocodes.add(cache.getGeocode());
            CachePopup.startActivityAllowTarget(map, cache.getGeocode());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AbstractDialogFragment.REQUEST_CODE_TARGET_INFO) {
            if (resultCode == AbstractDialogFragment.RESULT_CODE_SET_TARGET) {
                final TargetInfo targetInfo = data.getExtras().getParcelable(Intents.EXTRA_TARGET_INFO);

                if (targetInfo != null) {
                    lastNavTarget = targetInfo.coords;
                    if (navigationLayer != null) {
                        navigationLayer.setDestination(targetInfo.coords);
                        navigationLayer.requestRedraw();
                    }
                    if (distanceView != null) {
                        distanceView.setDestination(targetInfo.coords);
                        distanceView.setCoordinates(geoDirUpdate.getCurrenLocation());
                    }
                    if (StringUtils.isNotBlank(targetInfo.geocode)) {
                        targetGeocode = targetInfo.geocode;
                        final Geocache target = getCurrentTargetCache();
                        targetView.setTarget(targetGeocode, target != null ? target.getName() : StringUtils.EMPTY);
                    }
                }
            }
            final List<String> changedGeocodes = new ArrayList<>();
            String geocode = popupGeocodes.poll();
            while (geocode != null) {
                changedGeocodes.add(geocode);
                geocode = popupGeocodes.poll();
            }
            if (caches != null) {
                caches.invalidate(changedGeocodes);
            }
        }
    }
}
