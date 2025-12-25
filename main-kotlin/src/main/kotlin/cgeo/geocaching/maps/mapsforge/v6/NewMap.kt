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

package cgeo.geocaching.maps.mapsforge.v6

import cgeo.geocaching.AbstractDialogFragment
import cgeo.geocaching.AbstractDialogFragment.TargetInfo
import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.CompassActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.FilteredActivity
import cgeo.geocaching.databinding.MapMapsforgeV6Binding
import cgeo.geocaching.downloader.DownloaderUtils
import cgeo.geocaching.enumerations.CoordinateType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.gui.GeocacheFilterActivity
import cgeo.geocaching.location.GeoItemHolder
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.ProximityNotification
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.log.LoggingUI
import cgeo.geocaching.maps.MapMode
import cgeo.geocaching.maps.MapOptions
import cgeo.geocaching.maps.MapProviderFactory
import cgeo.geocaching.maps.MapSettingsUtils
import cgeo.geocaching.maps.MapState
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.RouteTrackUtils
import cgeo.geocaching.maps.Tracks
import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.maps.interfaces.OnMapDragListener
import cgeo.geocaching.maps.mapsforge.AbstractMapsforgeMapSource
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider
import cgeo.geocaching.maps.mapsforge.v6.caches.CachesBundle
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemLayer
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef
import cgeo.geocaching.maps.mapsforge.v6.layers.CoordsMarkerLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.GeoObjectLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.HistoryLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.NavigationLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.PositionLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.RouteLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.TapHandlerLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.TrackLayer
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.maps.routing.RoutingMode
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.TrailHistoryElement
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.service.CacheDownloaderService
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.ui.GeoItemSelectorUtils
import cgeo.geocaching.ui.RepeatOnHoldListener
import cgeo.geocaching.ui.ToggleItemType
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.ApplicationSettings
import cgeo.geocaching.utils.CompactIconModeUtils
import cgeo.geocaching.utils.FilterUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.HistoryTrackUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapLineUtils
import cgeo.geocaching.utils.MenuUtils
import cgeo.geocaching.utils.functions.Func1
import cgeo.geocaching.Intents.ACTION_INDIVIDUALROUTE_CHANGED
import cgeo.geocaching.Intents.ACTION_INVALIDATE_MAPLIST
import cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE
import cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT
import cgeo.geocaching.maps.MapProviderFactory.MAP_LANGUAGE_DEFAULT_ID
import cgeo.geocaching.maps.mapsforge.v6.caches.CachesBundle.NO_OVERLAY_ID

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.util.Supplier

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Queue
import java.util.Set
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.RejectedExecutionException

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.model.Point
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.core.util.Parameters
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.graphics.AndroidResourceBitmap
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.Layers
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.model.common.Observer

@SuppressLint("ClickableViewAccessibility")
// This is definitely a valid issue, but can't be refactored in one step
@SuppressWarnings("PMD.ExcessiveClassLength")
class NewMap : AbstractNavigationBarMapActivity() : Observer, FilteredActivity, AbstractDialogFragment.TargetUpdateReceiver {
    private static val STATE_ROUTETRACKUTILS: String = "routetrackutils"

    private static val ROUTING_SERVICE_KEY: String = "NewMap"

    private MfMapView mapView
    private TileCache tileCache
    private MapSource mapSource
    private ITileLayer tileLayer
    private HistoryLayer historyLayer
    private PositionLayer positionLayer
    private NavigationLayer navigationLayer
    private TapHandlerLayer tapHandlerLayer
    private RouteLayer routeLayer
    private TrackLayer trackLayer
    private GeoObjectLayer geoObjectLayer
    private CoordsMarkerLayer coordsMarkerLayer
    private Geopoint coordsMarkerPosition
    private CachesBundle caches
    private val mapHandlers: MapHandlers = MapHandlers(TapHandler(this), DisplayHandler(this), ShowProgressHandler(this))

    private var renderThemeHelper: RenderThemeHelper = null; //must be initialized in onCreate()

    private DistanceView distanceView
    private View mapAttribution

    private val trailHistory: ArrayList<TrailHistoryElement> = null

    private var targetGeocode: String = null
    private var lastNavTarget: Geopoint = null
    private val popupGeocodes: Queue<String> = ConcurrentLinkedQueue<>()

    private ProgressBar spinner

    private val geoDirUpdate: UpdateLoc = UpdateLoc(this)
    /**
     * initialization with an empty subscription to make static code analysis tools more happy
     */
    private ProximityNotification proximityNotification
    private val resumeDisposables: CompositeDisposable = CompositeDisposable()
    private MapOptions mapOptions
    private TargetView targetView
    private var individualRoute: IndividualRoute = null
    private var tracks: Tracks = null
    private UnifiedMapViewModel.SheetInfo sheetInfo = null

    private static Boolean followMyLocationSwitch = Settings.getFollowMyLocation()
    private final Int[] inFollowMyLocation = { 0 }

    private static val BUNDLE_MAP_STATE: String = "mapState"
    private static val BUNDLE_PROXIMITY_NOTIFICATION: String = "proximityNotification"
    private static val BUNDLE_FILTERCONTEXT: String = "filterContext"
    private static val BUNDLE_SHEETINFO: String = "sheetInfo"

    // Handler messages
    // DisplayHandler
    public static val UPDATE_TITLE: Int = 0
    // ShowProgressHandler
    public static val HIDE_PROGRESS: Int = 0
    public static val SHOW_PROGRESS: Int = 1

    private var lastViewport: Viewport = null
    private var lastCompactIconMode: Boolean = false

    private MapMode mapMode

    private var routeTrackUtils: RouteTrackUtils = null

    override     public Unit onCreate(final Bundle savedInstanceState) {
        ApplicationSettings.setLocale(this)
        super.onCreate(savedInstanceState)

        Log.d("NewMap: onCreate")

        routeTrackUtils = RouteTrackUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS), this::centerOnPosition, this::clearIndividualRoute, this::reloadIndividualRoute, this::setTrack, this::isTargetSet)
        tracks = Tracks(routeTrackUtils, this::setTrack)

        ResourceBitmapCacheMonitor.addRef()
        AndroidGraphicFactory.createInstance(this.getApplication())
        AndroidGraphicFactory.INSTANCE.setSvgCacheDir(LocalStorage.getMapsforgeSvgCacheDir())

        MapsforgeMapProvider.getInstance().updateOfflineMaps()

        this.renderThemeHelper = RenderThemeHelper(this)

        // Support for multi-threaded map painting
        Parameters.NUMBER_OF_THREADS = Settings.getMapOsmThreads()
        Log.i("OSM #threads=" + Parameters.NUMBER_OF_THREADS)

        // Use fast parent tile rendering to increase performance when zooming in
        Parameters.PARENT_TILES_RENDERING = Parameters.ParentTilesRendering.SPEED

        // Get parameters from the intent
        mapOptions = MapOptions(this, getIntent().getExtras())

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            mapOptions.mapState = savedInstanceState.getParcelable(BUNDLE_MAP_STATE)
            mapOptions.filterContext = savedInstanceState.getParcelable(BUNDLE_FILTERCONTEXT)
            proximityNotification = savedInstanceState.getParcelable(BUNDLE_PROXIMITY_NOTIFICATION)
            followMyLocationSwitch = mapOptions.mapState.followsMyLocation()
            sheetInfo = savedInstanceState.getParcelable(BUNDLE_SHEETINFO)
        } else {
            if (mapOptions.mapState != null) {
                followMyLocationSwitch = mapOptions.mapState.followsMyLocation()
            } else {
                followMyLocationSwitch = followMyLocationSwitch && mapOptions.mapMode == MapMode.LIVE
            }
            configureProximityNotifications()
        }
        individualRoute = null
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(this)
        }

        ActivityMixin.onCreate(this, true)

        // set layout
        ActivityMixin.setTheme(this)

        // adding the bottom navigation component is handled by {@link AbstractBottomNavigationActivity#setContentView}
        ActionBarUtils.setContentView(this, MapMapsforgeV6Binding.inflate(getLayoutInflater()).getRoot(), true)

        setTitle()
        this.mapAttribution = findViewById(R.id.map_attribution)

        // map settings popup
        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, individualRoute, this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, this::configureProximityNotifications, mapOptions.filterContext))

        // routes / tracks popup
        findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(individualRoute, this::setTarget, null))

        // prepare circular progress spinner
        spinner = findViewById(R.id.map_progressbar)
        spinner.setVisibility(View.GONE)

        // initialize map
        mapView = findViewById(R.id.mfmapv5)

        mapView.setClickable(true)
        mapView.getMapScaleBar().setVisible(true)
        mapView.setBuiltInZoomControls(true)

        // style zoom controls
        mapView.setBuiltInZoomControls(false)
        findViewById(R.id.map_zoomin).setOnTouchListener(RepeatOnHoldListener(500, v -> mapView.getModel().mapViewPosition.zoomIn()))
        findViewById(R.id.map_zoomout).setOnTouchListener(RepeatOnHoldListener(500, v -> mapView.getModel().mapViewPosition.zoomOut()))

        //make room for map attribution icon button
        val mapAttPx: Int = Math.round(this.getResources().getDisplayMetrics().density * 30)
        mapView.getMapScaleBar().setMarginHorizontal(mapAttPx)

        // create a tile cache of suitable size. always initialize it based on the smallest tile size to expect (256 for online tiles)
        tileCache = AndroidUtil.createTileCache(this, "mapcache", 256, 1f, this.mapView.getModel().frameBufferModel.getOverdrawFactor())

        // attach drag handler
        val dragHandler: DragHandler = DragHandler(this)
        mapView.setOnMapDragListener(dragHandler)

        mapMode = (mapOptions != null && mapOptions.mapMode != null) ? mapOptions.mapMode : MapMode.LIVE

        // prepare initial settings of mapView
        if (mapOptions.mapState != null) {
            this.mapView.getModel().mapViewPosition.setCenter(MapsforgeUtils.toLatLong(mapOptions.mapState.getCenter()))
            this.mapView.setMapZoomLevel((Byte) mapOptions.mapState.getZoomLevel())
            this.targetGeocode = mapOptions.mapState.getTargetGeocode()
            this.lastNavTarget = mapOptions.mapState.getLastNavTarget()
            mapOptions.isLiveEnabled = mapOptions.mapState.isLiveEnabled()
            mapOptions.isStoredEnabled = mapOptions.mapState.isStoredEnabled()
        } else if (mapOptions.searchResult != null) {
            val viewport: Viewport = DataStore.getBounds(mapOptions.searchResult.getGeocodes())

            if (viewport != null) {
                postZoomToViewport(viewport)
            }
        } else if (StringUtils.isNotEmpty(mapOptions.geocode) && mapOptions.mapMode != MapMode.COORDS) {
            val viewport: Viewport = DataStore.getBounds(mapOptions.geocode, Settings.getZoomIncludingWaypoints())

            if (viewport != null) {
                postZoomToViewport(viewport)
            }
            targetGeocode = mapOptions.geocode
            val temp: Geocache = getCurrentTargetCache()
            if (temp != null) {
                lastNavTarget = temp.getCoords()
            }
        } else if (mapOptions.coords != null) {
            postZoomToViewport(Viewport(mapOptions.coords, 0, 0))
            if (mapOptions.mapMode == MapMode.LIVE) {
                coordsMarkerPosition = mapOptions.coords
                if (coordsMarkerLayer != null) {
                    coordsMarkerLayer.setCoordsMarker(coordsMarkerPosition)
                }
                mapOptions.coords = null;   // no direction line, even if enabled in settings
                followMyLocationSwitch = false;   // do not center on GPS position, even if in LIVE mode
            }
        } else {
            postZoomToViewport(Viewport(Settings.getMapCenter().getCoords(), 0, 0))
        }

        FilterUtils.initializeFilterBar(this, this)
        MapUtils.updateFilterBar(this, mapOptions.filterContext)

        Routing.connect(ROUTING_SERVICE_KEY, () -> resumeRoute(true), this)
        CompactIconModeUtils.setCompactIconModeThreshold(getResources())

        MapsforgeMapProvider.getInstance().updateOfflineMaps()

        MapUtils.showMapOneTimeMessages(this, mapMode)
        getLifecycle().addObserver(GeocacheChangedBroadcastReceiver(this) {
            override             protected Unit onReceive(final Context context, final String geocode) {
                caches.invalidate(Collections.singleton(geocode))
            }
        })

        this.getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, ACTION_INVALIDATE_MAPLIST) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                invalidateOptionsMenu()
            }
        })

        getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, ACTION_INDIVIDUALROUTE_CHANGED) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                reloadIndividualRoute()
            }
        })

    }

    private Unit postZoomToViewport(final Viewport viewport) {
        mapView.post(() -> mapView.zoomToViewport(viewport, this.mapMode))
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        val result: Boolean = super.onCreateOptionsMenu(menu)
        getMenuInflater().inflate(R.menu.map_activity, menu)

        MapProviderFactory.addMapviewMenuItems(this, menu)
        MapProviderFactory.addMapViewLanguageMenuItems(menu)

        initFollowMyLocation(followMyLocationSwitch)
        FilterUtils.initializeFilterMenu(this, this)

        return result
    }

    override     public Int getSelectedBottomItemId() {
        return mapOptions.mapMode == MapMode.LIVE ? MENU_MAP : MENU_HIDE_NAVIGATIONBAR
    }

    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu)
        if (mapOptions != null) {
            ViewUtils.extendMenuActionBarDisplayItemCount(this, menu)
        }

        for (final MapSource mapSource : MapProviderFactory.getMapSources()) {
            MenuUtils.setVisible(menu.findItem(mapSource.getNumericalId()), mapSource.isAvailable())
        }

        try {
            val itemMapLive: MenuItem = menu.findItem(R.id.menu_map_live)
            ToggleItemType.LIVE_MODE.toggleMenuItem(itemMapLive, mapOptions.isLiveEnabled)

            itemMapLive.setVisible(mapOptions.coords == null || mapOptions.mapMode == MapMode.LIVE)

            menu.findItem(R.id.menu_store_caches).setVisible(!caches.isDownloading() && !caches.getVisibleCacheGeocodes().isEmpty())

            val tileLayerHasThemes: Boolean = tileLayerHasThemes()
            menu.findItem(R.id.menu_theme_mode).setVisible(tileLayerHasThemes)
            menu.findItem(R.id.menu_theme_options).setVisible(tileLayerHasThemes)

            menu.findItem(R.id.menu_as_list).setVisible(!caches.isDownloading() && caches.getVisibleCachesCount() > 1)

            menu.findItem(R.id.menu_hint).setVisible(mapOptions.mapMode == MapMode.SINGLE)
            menu.findItem(R.id.menu_compass).setVisible(mapOptions.mapMode == MapMode.SINGLE)
            if (mapOptions.mapMode == MapMode.SINGLE) {
                LoggingUI.onPrepareOptionsMenu(menu, getSingleModeCache())
            }
            HistoryTrackUtils.onPrepareOptionsMenu(menu)
        } catch (final RuntimeException e) {
            Log.e("NewMap.onPrepareOptionsMenu", e)
        }

        return true
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)

        invalidateOptionsMenu()
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val id: Int = item.getItemId()
        if (id == R.id.menu_map_live) {
            mapOptions.isLiveEnabled = !mapOptions.isLiveEnabled
            if (mapOptions.isLiveEnabled) {
                mapOptions.isStoredEnabled = true
                mapOptions.filterContext = GeocacheFilterContext(LIVE)
                caches.setFilterContext(mapOptions.filterContext)
                refreshMapData(false, true)
            }

            if (mapOptions.mapMode == MapMode.LIVE) {
                Settings.setLiveMap(mapOptions.isLiveEnabled)
            }
            caches.handleStoredLayers(this, mapOptions)
            caches.handleLiveLayers(this, mapOptions)
            ActivityMixin.invalidateOptionsMenu(this)
            if (mapOptions.mapMode == MapMode.SINGLE) {
                // reset target cache on single mode map
                targetGeocode = mapOptions.geocode
            }
            mapOptions.mapMode = MapMode.LIVE
            updateSelectedBottomNavItemId()
            mapOptions.title = StringUtils.EMPTY
            setTitle()
        } else if (id == R.id.menu_filter) {
            showFilterMenu()
        } else if (id == R.id.menu_store_caches) {
            val visibleCacheGeocodes: Set<String> = caches.getVisibleCacheGeocodes()
            CacheDownloaderService.downloadCaches(this, visibleCacheGeocodes, false, false, () -> caches.invalidate(visibleCacheGeocodes))
        } else if (id == R.id.menu_theme_mode) {
            this.renderThemeHelper.selectMapTheme(this.tileLayer, this.tileCache)
        } else if (id == R.id.menu_theme_options) {
            this.renderThemeHelper.selectMapThemeOptions()
        } else if (id == R.id.menu_as_list) {
            CacheListActivity.startActivityMap(this, SearchResult(caches.getVisibleCacheGeocodes()))
            ActivityMixin.overrideTransitionToFade(this)
        } else if (id == R.id.menu_hillshading) {
            Settings.setMapShadingShowLayer(!Settings.getMapShadingShowLayer())
            item.setChecked(Settings.getMapShadingShowLayer())
            changeMapSource(mapSource)
        } else if (id == R.id.menu_hint) {
            menuShowHint()
        } else if (id == R.id.menu_compass) {
            menuCompass()
        } else if (LoggingUI.onMenuItemSelected(item, this, getSingleModeCache(), null)) {
            return true
        } else if (id == R.id.menu_check_routingdata) {
            val bb: BoundingBox = mapView.getBoundingBox()
            MapUtils.checkRoutingData(this, bb.minLatitude, bb.minLongitude, bb.maxLatitude, bb.maxLongitude)
        } else if (id == R.id.menu_check_hillshadingdata) {
            val bb: BoundingBox = mapView.getBoundingBox()
            MapUtils.checkHillshadingData(this, bb.minLatitude, bb.minLongitude, bb.maxLatitude, bb.maxLongitude)
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> historyLayer.requestRedraw(), this::clearTrailHistory)
                || DownloaderUtils.onOptionsItemSelected(this, id)) {
            return true
        } else if (id == R.id.menu_routetrack) {
            routeTrackUtils.showPopup(individualRoute, this::setTarget, null)
        } else {
            val language: String = MapProviderFactory.getLanguage(id)
            val mapSource: MapSource = MapProviderFactory.getMapSource(id)
            if (language != null || id == MAP_LANGUAGE_DEFAULT_ID) {
                item.setChecked(true)
                changeLanguage(language)
                return true
            }
            if (mapSource != null) {
                item.setChecked(true)
                changeMapSource(mapSource)
                return true
            }
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private Unit refreshMapData(final Boolean circlesSwitched, final Boolean filterChanged) {
        if (circlesSwitched) {
            caches.switchCircles()
        }
        if (caches != null) {
            caches.invalidate()
        }
        if (null != trackLayer) {
            trackLayer.requestRedraw()
        }
        if (null != geoObjectLayer) {
            geoObjectLayer.requestRedraw()
        }

        if (filterChanged) {
            MapUtils.updateFilterBar(this, mapOptions.filterContext)
        }
    }

    private Unit routingModeChanged(final RoutingMode newValue) {
        Settings.setRoutingMode(newValue)
        if ((null != individualRoute && individualRoute.getNumSegments() > 0) || null != tracks) {
            ViewUtils.showShortToast(this, R.string.brouter_recalculating)
        }
        reloadIndividualRoute()
        if (null != tracks) {
            try {
                AndroidRxUtils.andThenOnUi(Schedulers.computation(), () -> tracks.traverse((key, route, color, width) -> {
                    if (route is Route) {
                        ((Route) route).calculateNavigationRoute()
                    }
                    trackLayer.updateRoute(key, route, color, width)
                }), () -> trackLayer.requestRedraw())
            } catch (RejectedExecutionException e) {
                Log.e("NewMap.routingModeChanged: RejectedExecutionException: " + e.getMessage())
            }
        }
        navigationLayer.requestRedraw()
    }

    private Unit compactIconModeChanged(final Int newValue) {
        Settings.setCompactIconMode(newValue)
        caches.invalidateAll(NO_OVERLAY_ID)
    }

    private Unit clearTrailHistory() {
        this.historyLayer.reset()
        this.historyLayer.requestRedraw()
        showToast(res.getString(R.string.map_trailhistory_cleared))
    }

    private Unit clearIndividualRoute() {
        individualRoute.clearRoute(routeLayer)
        individualRoute.clearRoute((route) -> {
            routeLayer.updateIndividualRoute(route)
            updateRouteTrackButtonVisibility()
        })
        distanceView.showRouteDistance()
        showToast(res.getString(R.string.map_individual_route_cleared))
    }

    private Unit centerOnPosition(final Double latitude, final Double longitude, final Viewport viewport) {
        followMyLocationSwitch = false
        initFollowMyLocation(false)
        mapView.getModel().mapViewPosition.setMapPosition(MapPosition(LatLong(latitude, longitude), (Byte) mapView.getMapZoomLevel()))
        postZoomToViewport(viewport)
    }

    private Unit menuCompass() {
        val cache: Geocache = getCurrentTargetCache()
        if (cache != null) {
            CompassActivity.startActivityCache(this, cache)
        }
    }

    private Unit menuShowHint() {
        val cache: Geocache = getCurrentTargetCache()
        if (cache != null) {
            cache.showHintToast(this)
        }
    }

    override     public Unit showFilterMenu() {
        FilterUtils.openFilterActivity(this, mapOptions.filterContext,
                SearchResult(caches.getVisibleCacheGeocodes()).getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB))
    }

    override     public Boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, mapOptions.filterContext)
    }

    override     public Unit refreshWithFilter(final GeocacheFilter filter) {
        mapOptions.filterContext.set(filter)
        refreshMapData(false, true)
    }

    private Unit changeMapSource(final MapSource newSource) {
        val oldSource: MapSource = Settings.getMapSource()
        val restartRequired: Boolean = !MapProviderFactory.isSameActivity(oldSource, newSource)

        // Update MapSource in settings
        Settings.setMapSource(newSource)

        if (restartRequired) {
            mapRestart()
        } else if (mapView != null) {  // changeMapSource can be called by onCreate()
            switchTileLayer(newSource)
        }
    }

    private Unit changeLanguage(final String language) {
        Settings.setMapLanguage(language)
        mapRestart()
    }

    /**
     * Restart the current activity with the default map source.
     */
    private Unit mapRestart() {
        mapOptions.mapState = currentMapState()
        finish()
        mapOptions.startIntentWithoutTransition(this, Settings.getMapProvider().getMapClass())
    }

    /**
     * Get the current map state from the map view if it exists or from the mapStateIntent field otherwise.
     *
     * @return the current map state as an array of Int, or null if no map state is available
     */
    private MapState currentMapState() {
        if (mapView == null) {
            return null
        }
        val mapCenter: Geopoint = mapView.getViewport().getCenter()
        return MapState(mapCenter.getCoords(), mapView.getMapZoomLevel(), followMyLocationSwitch, Settings.isShowCircles(), targetGeocode, lastNavTarget, mapOptions.isLiveEnabled, mapOptions.isStoredEnabled)
    }

    private Unit configureProximityNotifications() {
        // reconfigure, but only if necessary
        proximityNotification = Settings.isGeneralProximityNotificationActive() ? proximityNotification != null ? proximityNotification : ProximityNotification(true, false) : null
    }

    private Unit switchTileLayer(final MapSource newSource) {
        val oldLayer: ITileLayer = this.tileLayer
        ITileLayer newLayer = null

        mapSource = newSource

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(
                    MapAttributionDisplayHandler(() -> this.mapSource.calculateMapAttribution(this)))
        }

        if (newSource is AbstractMapsforgeMapSource) {
            newLayer = ((AbstractMapsforgeMapSource) newSource).createTileLayer(tileCache, this.mapView.getModel().mapViewPosition)
        }
        ActivityMixin.invalidateOptionsMenu(this)

        // Exchange layer
        if (newLayer != null) {
            mapView.setZoomLevelMax(newLayer.getZoomLevelMax())
            mapView.setZoomLevelMin(newLayer.getZoomLevelMin())

            // make sure map zoom level is within zoom level boundaries
            val currentZoomLevel: Int = mapView.getMapZoomLevel()
            if (currentZoomLevel < newLayer.getZoomLevelMin()) {
                mapView.setMapZoomLevel(newLayer.getZoomLevelMin())
            } else if (currentZoomLevel > newLayer.getZoomLevelMax()) {
                mapView.setMapZoomLevel(newLayer.getZoomLevelMax())
            }

            val layers: Layers = this.mapView.getLayerManager().getLayers()
            Int index = 0
            if (oldLayer != null) {
                index = layers.indexOf(oldLayer.getTileLayer()) + 1
            }
            layers.add(index, newLayer.getTileLayer())
            this.tileLayer = newLayer
            this.renderThemeHelper.reapplyMapTheme(this.tileLayer, this.tileCache)
            //trigger mapscalebar redraw - otherwise the shown distances will be wrong.
            //See https://github.com/mapsforge/mapsforge/discussions/1313
            this.mapView.getMapScaleBar().setDistanceUnitAdapter(this.mapView.getMapScaleBar().getDistanceUnitAdapter())
            this.tileLayer.onResume()
        } else {
            this.tileLayer = null
        }

        // Cleanup
        if (oldLayer != null) {
            this.mapView.getLayerManager().getLayers().remove(oldLayer.getTileLayer())
            oldLayer.getTileLayer().onDestroy()
        }
        tileCache.purge()
    }

    private Unit resumeTileLayer() {
        if (this.tileLayer != null) {
            this.tileLayer.onResume()
        }
    }

    private Unit pauseTileLayer() {
        if (this.tileLayer != null) {
            this.tileLayer.onPause()
        }
    }

    private Boolean tileLayerHasThemes() {
        if (tileLayer != null) {
            return tileLayer.hasThemes()
        }

        return false
    }

    private Unit resumeRoute(final Boolean force) {
        if (null == individualRoute || force) {
            individualRoute = IndividualRoute(this::setTarget)
            reloadIndividualRoute()
        } else {
            individualRoute.updateRoute(routeLayer)
        }
    }

    private Unit resumeTrack(final String key, final Boolean preventReloading) {
        if (null == tracks) {
            if (!preventReloading) {
                this.tracks = Tracks(this.routeTrackUtils, this::setTrack)
            }
        } else {
            val gdp: IGeoItemSupplier = tracks.getRoute(key)
            if (trackLayer != null && (gdp == null || gdp is Route)) {
                trackLayer.updateRoute(key, gdp, tracks.getColor(key), tracks.getWidth(key))
            }
            if (null != geoObjectLayer && (gdp == null || gdp is GeoItemHolder)) {
                if (gdp == null || gdp.isHidden()) {
                    geoObjectLayer.removeGeoObjectLayer(key)
                } else {
                    //settings... this will be implemented more beautiful in unified map
                    val widthFactor: Float = 2f
                    val defaultWidth: Float = tracks.getWidth(key) / widthFactor
                    val defaultStrokeColor: Int = tracks.getColor(key)
                    val defaultFillColor: Int = Color.argb(32, Color.red(defaultStrokeColor), Color.green(defaultStrokeColor), Color.blue(defaultStrokeColor))
                    val widthAdjuster: Func1<Float, Float> = w -> MapLineUtils.getWidthFromRaw(w == null ? 1 : w.intValue(), false) * widthFactor

                    val l: Layer = GeoObjectLayer.createGeoObjectLayer(gdp.getItem(), geoObjectLayer.getDisplayModel(),
                            defaultWidth, defaultStrokeColor, defaultFillColor, widthAdjuster)
                    geoObjectLayer.addGeoObjectLayer(key, l)
                }
            }
        }
    }

    override     protected Unit onResume() {
        super.onResume()
        Log.d("NewMap: onResume")

        resumeTileLayer()
        resumeRoute(Settings.removeFromRouteOnLog())
        if (tracks != null) {
            tracks.resumeAllTracks(this::resumeTrack)
        }
        mapView.getModel().mapViewPosition.addObserver(this)
        MapUtils.updateFilterBar(this, mapOptions.filterContext)
    }

    override     protected Unit onStart() {
        super.onStart()
        Log.d("NewMap: onStart")

        sheetManageLifecycleOnStart(sheetInfo, newSheetInfo -> sheetInfo = newSheetInfo)
        initializeLayers()
    }

    override     protected Unit clearSheetInfo() {
        sheetInfo = null
    }

    private Unit initializeLayers() {

        switchTileLayer(Settings.getMapSource())

        // History Layer
        this.historyLayer = HistoryLayer(trailHistory)
        this.mapView.getLayerManager().getLayers().add(this.historyLayer)

        // RouteLayer
        this.routeLayer = RouteLayer(realRouteDistance -> {
            if (null != this.distanceView) {
                this.distanceView.setRouteDistance(realRouteDistance)
            }
        }, this.mapHandlers.getTapHandler(), mapView.getLayerManager())
        this.mapView.getLayerManager().getLayers().add(this.routeLayer)

        //GeoobjectLayer
        this.geoObjectLayer = GeoObjectLayer()
        this.mapView.getLayerManager().getLayers().add(this.geoObjectLayer)

        // TrackLayer
        this.trackLayer = TrackLayer()
        this.mapView.getLayerManager().getLayers().add(this.trackLayer)

        // NavigationLayer
        Geopoint navTarget = lastNavTarget
        if (navTarget == null) {
            navTarget = mapOptions.coords
            if (navTarget == null && StringUtils.isNotEmpty(mapOptions.geocode)) {
                val bounds: Viewport = DataStore.getBounds(mapOptions.geocode)
                if (bounds != null) {
                    navTarget = bounds.center
                }
            }
        }
        this.navigationLayer = NavigationLayer(navTarget, realDistance -> {
            if (null != this.distanceView) {
                this.distanceView.setRealDistance(realDistance)
            }
        })
        this.mapView.getLayerManager().getLayers().add(this.navigationLayer)

        // GeoitemLayer
        GeoitemLayer.resetColors()

        // Coords marker
        this.coordsMarkerLayer = CoordsMarkerLayer()
        coordsMarkerLayer.setCoordsMarker(coordsMarkerPosition)
        this.mapView.getLayerManager().getLayers().add(this.coordsMarkerLayer)

        // TapHandler
        this.tapHandlerLayer = TapHandlerLayer(this.mapHandlers.getTapHandler(), this)
        this.mapView.getLayerManager().getLayers().add(this.tapHandlerLayer)

        // Caches bundle
        if (mapOptions.searchResult != null) {
            this.caches = CachesBundle(this, mapOptions.searchResult, this.mapView, this.mapHandlers)
        } else if (StringUtils.isNotEmpty(mapOptions.geocode)) {
            if (mapOptions.mapMode == MapMode.COORDS && mapOptions.coords != null) {
                this.caches = CachesBundle(this, mapOptions.coords, mapOptions.waypointType, mapOptions.waypointPrefix, this.mapView, this.mapHandlers, mapOptions.geocode)
            } else {
                this.caches = CachesBundle(this, mapOptions.geocode, this.mapView, this.mapHandlers)
            }
        } else if (mapOptions.coords != null) {
            this.caches = CachesBundle(this, mapOptions.coords, mapOptions.waypointType, mapOptions.waypointPrefix, this.mapView, this.mapHandlers, null)
        } else {
            caches = CachesBundle(this, this.mapView, this.mapHandlers)
        }

        // Stored enabled map
        caches.handleStoredLayers(this, mapOptions)
        // Live enabled map
        caches.handleLiveLayers(this, mapOptions)
        caches.setFilterContext(mapOptions.filterContext)

        // Position layer
        this.positionLayer = PositionLayer()
        this.mapView.getLayerManager().getLayers().add(positionLayer)

        //Distance view
        this.distanceView = DistanceView(findViewById(R.id.distanceinfo), navTarget, Settings.isBrouterShowBothDistances())

        //Target view
        this.targetView = TargetView(findViewById(R.id.target), StringUtils.EMPTY, StringUtils.EMPTY)
        val target: Geocache = getCurrentTargetCache()
        if (target != null) {
            targetView.setTarget(target.getShortGeocode(), target.getName())
        }

        // resume location access
        resumeDisposables.add(geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR))
    }

    override     public Unit onPause() {
        Log.d("NewMap: onPause")

        savePrefs()

        pauseTileLayer()
        mapView.getModel().mapViewPosition.removeObserver(this)
        super.onPause()
    }

    override     protected Unit onStop() {
        Log.d("NewMap: onStop")
        terminateLayers()
        super.onStop()
    }

    private Unit terminateLayers() {

        this.resumeDisposables.clear()

        this.caches.onDestroy()
        this.caches = null

        this.mapView.getLayerManager().getLayers().remove(this.positionLayer)
        this.positionLayer = null
        this.mapView.getLayerManager().getLayers().remove(this.navigationLayer)
        this.navigationLayer = null
        this.mapView.getLayerManager().getLayers().remove(this.routeLayer)
        this.routeLayer = null
        this.mapView.getLayerManager().getLayers().remove(this.geoObjectLayer)
        this.geoObjectLayer = null
        this.mapView.getLayerManager().getLayers().remove(this.trackLayer)
        this.trackLayer = null
        this.mapView.getLayerManager().getLayers().remove(this.historyLayer)
        this.historyLayer = null

        if (this.tileLayer != null) {
            this.mapView.getLayerManager().getLayers().remove(this.tileLayer.getTileLayer())
            this.tileLayer.getTileLayer().onDestroy()
            this.tileLayer = null
        }
    }

    override     protected Unit onDestroy() {
        Log.d("NewMap: onDestroy")
        this.tileCache.destroy()
        this.mapView.getModel().mapViewPosition.destroy()
        this.mapView.destroy()
        ResourceBitmapCacheMonitor.release()

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(null)
        }
        super.onDestroy()
    }

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState())

        Log.d("New map: onSaveInstanceState")

        val state: MapState = prepareMapState()
        outState.putParcelable(BUNDLE_MAP_STATE, state)
        if (proximityNotification != null) {
            outState.putParcelable(BUNDLE_PROXIMITY_NOTIFICATION, proximityNotification)
        }
        if (mapOptions.filterContext != null) {
            outState.putParcelable(BUNDLE_FILTERCONTEXT, mapOptions.filterContext)
        }
        if (sheetInfo != null) {
            outState.putParcelable(BUNDLE_SHEETINFO, sheetInfo)
        }
    }

    private MapState prepareMapState() {
        return MapState(MapsforgeUtils.toGeopoint(mapView.getModel().mapViewPosition.getCenter()), mapView.getMapZoomLevel(), followMyLocationSwitch, false, targetGeocode, lastNavTarget, mapOptions.isLiveEnabled, mapOptions.isStoredEnabled)
    }

    private Unit centerMap(final Geopoint geopoint) {
        mapView.getModel().mapViewPosition.setCenter(LatLong(geopoint.getLatitude(), geopoint.getLongitude()))
    }

    public Location getCoordinates() {
        val center: LatLong = mapView.getModel().mapViewPosition.getCenter()
        val loc: Location = Location("newmap")
        loc.setLatitude(center.latitude)
        loc.setLongitude(center.longitude)
        return loc
    }

    private Unit initFollowMyLocation(final Boolean followMyLocation) {
        synchronized (inFollowMyLocation) {
            if (inFollowMyLocation[0] > 0) {
                return
            }
            inFollowMyLocation[0]++
        }
        Settings.setFollowMyLocation(followMyLocation)
        followMyLocationSwitch = followMyLocation

        val followMyLocationButton: ImageButton = findViewById(R.id.map_followmylocation_btn)
        if (followMyLocationButton != null) { // can be null after screen rotation
            followMyLocationButton.setImageResource(followMyLocation ? R.drawable.map_followmylocation_btn : R.drawable.map_followmylocation_off_btn)
            followMyLocationButton.setOnClickListener(v -> initFollowMyLocation(!followMyLocationSwitch))
        }

        if (followMyLocation) {
            val currentLocation: Location = LocationDataProvider.getInstance().currentGeo(); // get location even if none was delivered to the view-model yet
            mapView.setCenter(LatLong(currentLocation.getLatitude(), currentLocation.getLongitude()))
        }
        synchronized (inFollowMyLocation) {
            inFollowMyLocation[0]--
        }
    }

    public Unit triggerLongTapContextMenu(final Point tapXY) {
        if (Settings.isLongTapOnMapActivated()) {
            MapUtils.createMapLongClickPopupMenu(this, Geopoint(tapHandlerLayer.getLongTapLatLong().latitude, tapHandlerLayer.getLongTapLatLong().longitude),
                            android.graphics.Point((Int) tapXY.x, (Int) tapXY.y), individualRoute, routeLayer,
                            this::updateRouteTrackButtonVisibility,
                            getCurrentTargetCache(), mapOptions, this::setTarget)
                    .setOnDismissListener(menu -> tapHandlerLayer.resetLongTapLatLong())
                    .show()
        }
    }

    override     public Unit onReceiveTargetUpdate(final TargetInfo targetInfo) {
        if (Settings.isAutotargetIndividualRoute()) {
            Settings.setAutotargetIndividualRoute(false)
            ViewUtils.showShortToast(this, R.string.map_disable_autotarget_individual_route)
        }
        setTarget(targetInfo.coords, targetInfo.geocode)
    }

    private static class DisplayHandler : Handler() {

        private final WeakReference<NewMap> mapRef

        DisplayHandler(final NewMap map) {
            this.mapRef = WeakReference<>(map)
        }

        override         public Unit handleMessage(final Message msg) {
            val map: NewMap = mapRef.get()
            if (map == null) {
                return
            }
            if (msg.what == UPDATE_TITLE) {
                map.setTitle()
                map.setSubtitle()
            }
        }

    }

    private Unit setTitle() {
        ActionBarUtils.setTitle(this, calculateTitle())
    }

    private String calculateTitle() {
        if (mapOptions.isLiveEnabled) {
            return res.getString(R.string.map_live)
        }
        if (mapOptions.mapMode == MapMode.SINGLE) {
            val cache: Geocache = getSingleModeCache()
            if (cache != null) {
                return cache.getName()
            }
        }
        return StringUtils.defaultIfEmpty(mapOptions.title, res.getString(R.string.map_offline))
    }

    private Unit setSubtitle() {
        ActionBarUtils.setSubtitle(this, calculateSubtitle())
    }

    private String calculateSubtitle() {
        if (!mapOptions.isLiveEnabled && mapOptions.mapMode == MapMode.SINGLE) {
            val cache: Geocache = getSingleModeCache()
            if (cache != null) {
                return Formatter.formatMapSubtitle(cache)
            }
            return ""
        }

        // count caches in the sub title
        val visible: Int = countVisibleCaches()
        val total: Int = countTotalCaches()

        val subtitle: StringBuilder = StringBuilder()
        if (visible != total && Settings.isDebug()) {
            subtitle.append(visible).append('/').append(res.getQuantityString(R.plurals.cache_counts, total, total))
        } else {
            subtitle.append(res.getQuantityString(R.plurals.cache_counts, visible, visible))
        }

        return subtitle.toString()
    }

    private Int countVisibleCaches() {
        return caches != null ? caches.getVisibleCachesCount() : 0
    }

    private Int countTotalCaches() {
        return caches != null ? caches.getCachesCount() : 0
    }

    /**
     * Updates the progress.
     */
    private static class ShowProgressHandler : Handler() {
        private var counter: Int = 0

        private final WeakReference<NewMap> mapRef

        ShowProgressHandler(final NewMap map) {
            this.mapRef = WeakReference<>(map)
        }

        override         public Unit handleMessage(final Message msg) {
            val what: Int = msg.what

            if (what == HIDE_PROGRESS) {
                if (--counter == 0) {
                    showProgress(false)
                }
            } else if (what == SHOW_PROGRESS) {
                showProgress(true)
                counter++
            }
        }

        private Unit showProgress(final Boolean show) {
            val map: NewMap = mapRef.get()
            if (map == null) {
                return
            }
            map.spinner.setVisibility(show ? View.VISIBLE : View.GONE)
        }

    }

    // class: update location
    private static class UpdateLoc : GeoDirHandler() {
        // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

        // minimum time in milliseconds between position overlay updates
        private static val MIN_UPDATE_INTERVAL: Long = 500
        // minimum change of heading in grad for position overlay update
        private static val MIN_HEADING_DELTA: Float = 15f
        // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
        private static val MIN_LOCATION_DELTA: Float = 0.01f

        Location currentLocation = LocationDataProvider.getInstance().currentGeo()
        Float currentHeading

        private var timeLastPositionOverlayCalculation: Long = 0
        private var timeLastDistanceCheck: Long = 0
        /**
         * weak reference to the outer class
         */
        private final WeakReference<NewMap> mapRef

        UpdateLoc(final NewMap map) {
            mapRef = WeakReference<>(map)
        }

        override         public Unit updateGeoDir(final cgeo.geocaching.sensors.GeoData geo, final Float dir) {
            currentLocation = geo
            currentHeading = AngleUtils.getDirectionNow(dir)
            repaintPositionOverlay()
        }

        public Location getCurrentLocation() {
            return currentLocation
        }

        /**
         * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
         */
        Unit repaintPositionOverlay() {
            val currentTimeMillis: Long = System.currentTimeMillis()
            if (currentTimeMillis > (timeLastPositionOverlayCalculation + MIN_UPDATE_INTERVAL)) {
                timeLastPositionOverlayCalculation = currentTimeMillis

                try {
                    val map: NewMap = mapRef.get()
                    if (map != null) {
                        val needsRepaintForDistanceOrAccuracy: Boolean = needsRepaintForDistanceOrAccuracy()
                        val needsRepaintForHeading: Boolean = needsRepaintForHeading()

                        if (needsRepaintForDistanceOrAccuracy && NewMap.followMyLocationSwitch) {
                            map.centerMap(Geopoint(currentLocation))
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {

                            map.historyLayer.setCoordinates(currentLocation)
                            map.navigationLayer.setCoordinates(currentLocation)
                            map.distanceView.setCoordinates(currentLocation)
                            map.distanceView.showRouteDistance()
                            map.positionLayer.setCoordinates(currentLocation)
                            map.positionLayer.setHeading(currentHeading)
                            map.positionLayer.requestRedraw()

                            if (null != map.proximityNotification && (timeLastDistanceCheck == 0 || currentTimeMillis > (timeLastDistanceCheck + MIN_UPDATE_INTERVAL))) {
                                map.proximityNotification.checkDistance(map.caches.getClosestDistanceInM(Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude())))
                                timeLastDistanceCheck = System.currentTimeMillis()
                            }
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.w("Failed to update location", e)
                }
            }
        }

        Boolean needsRepaintForHeading() {
            val map: NewMap = mapRef.get()
            if (map == null) {
                return false
            }
            return Math.abs(AngleUtils.difference(currentHeading, map.positionLayer.getHeading())) > MIN_HEADING_DELTA
        }

        Boolean needsRepaintForDistanceOrAccuracy() {
            val map: NewMap = mapRef.get()
            if (map == null) {
                return false
            }
            val lastLocation: Location = map.getCoordinates()

            Float dist = Float.MAX_VALUE
            if (lastLocation != null) {
                if (lastLocation.getAccuracy() != currentLocation.getAccuracy()) {
                    return true
                }
                dist = currentLocation.distanceTo(lastLocation)
            }

            final Float[] mapDimension = Float[1]
            if (map.mapView.getWidth() < map.mapView.getHeight()) {
                val span: Double = map.mapView.getLongitudeSpan() / 1e6
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude() + span, mapDimension)
            } else {
                val span: Double = map.mapView.getLatitudeSpan() / 1e6
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude() + span, currentLocation.getLongitude(), mapDimension)
            }

            return dist > (mapDimension[0] * MIN_LOCATION_DELTA)
        }
    }

    private static class DragHandler : OnMapDragListener {

        private final WeakReference<NewMap> mapRef

        DragHandler(final NewMap parent) {
            mapRef = WeakReference<>(parent)
        }

        override         public Unit onDrag() {
            val map: NewMap = mapRef.get()
            if (map != null && NewMap.followMyLocationSwitch) {
                NewMap.followMyLocationSwitch = false
                map.initFollowMyLocation(false)
            }
        }
    }

    public Unit showSelection(final List<GeoitemRef> items, final Boolean longPressMode) {
        if (items.isEmpty() && !longPressMode) {
            if (sheetRemoveFragment()) {
                return
            }
            ActionBarUtils.toggleActionBar(this)
        }
        if (items.isEmpty()) {
            return
        }

        if (items.size() == 1) {
            if (longPressMode) {
                if (Settings.isLongTapOnMapActivated()) {
                    triggerCacheWaypointLongTapContextMenu(items.get(0))
                }
            } else {
                showPopup(items.get(0))
            }
            return
        }
        try {
            val sorted: ArrayList<GeoitemRef> = ArrayList<>(items)
            Collections.sort(sorted, GeoitemRef.NAME_COMPARATOR)

            final SimpleDialog.ItemSelectModel<GeoitemRef> model = SimpleDialog.ItemSelectModel<>()
            model
                .setItems(sorted)
                .setDisplayViewMapper((item, itemGroup, ctx, view, parent) ->
                        GeoItemSelectorUtils.createGeoItemView(NewMap.this, item, GeoItemSelectorUtils.getOrCreateView(NewMap.this, view, parent)),
                        (item, itemGroup) -> item.getName() + "::" + item.getGeocode())
                .setItemPadding(0)

            SimpleDialog.of(this).setTitle(R.string.map_select_multiple_items).selectSingle(model, item -> {
                if (longPressMode) {
                    if (Settings.isLongTapOnMapActivated()) {
                        triggerCacheWaypointLongTapContextMenu(item)
                    }
                } else {
                    showPopup(item)
                }
            })

        } catch (final NotFoundException e) {
            Log.e("NewMap.showSelection", e)
        }
    }

    private Unit triggerCacheWaypointLongTapContextMenu(final GeoitemRef item) {
        val mapSize: Long = MercatorProjection.getMapSize(mapView.getModel().mapViewPosition.getZoomLevel(), mapView.getModel().displayModel.getTileSize())
        Geopoint geopoint = null
        if (item.getType() == CoordinateType.CACHE) {
            val cache: Geocache = DataStore.loadCache(item.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
            if (cache != null) {
                geopoint = cache.getCoords()
            }
        } else {
            val waypoint: Waypoint = DataStore.loadWaypoint(item.getId())
            if (waypoint != null) {
                geopoint = waypoint.getCoords()
            }
        }
        if (geopoint == null) {
            geopoint = mapView.getViewport().center
        }

        val routeItem: RouteItem = RouteItem(item)
        if (Settings.isShowRouteMenu()) {
            val tapX: Int = (Int) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - MercatorProjection.longitudeToPixelX(mapView.getViewport().bottomLeft.getLongitude(), mapSize))
            val tapY: Int = (Int) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - MercatorProjection.latitudeToPixelY(mapView.getViewport().topRight.getLatitude(), mapSize))
            MapUtils.createCacheWaypointLongClickPopupMenu(this, routeItem, tapX, tapY, individualRoute, routeLayer, this::updateRouteTrackButtonVisibility)
                    .setOnDismissListener(menu -> tapHandlerLayer.resetLongTapLatLong())
                    .show()
        } else {
            toggleRouteItem(item)
        }
    }

    private Unit showPopup(final GeoitemRef item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return
        }

        try {
            if (item.getType() == CoordinateType.CACHE) {
                val cache: Geocache = DataStore.loadCache(item.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
                if (cache != null) {
                    popupGeocodes.add(cache.getGeocode())
                    sheetInfo = UnifiedMapViewModel.SheetInfo(cache.getGeocode(), 0)
                    sheetShowDetails(sheetInfo)
                    return
                }
                return
            }

            if (item.getType() == CoordinateType.WAYPOINT && item.getId() >= 0) {
                popupGeocodes.add(item.getGeocode())
                sheetInfo = UnifiedMapViewModel.SheetInfo(item.getGeocode(), item.getId())
                sheetShowDetails(sheetInfo)
            }

        } catch (final NotFoundException e) {
            Log.e("NewMap.showPopup", e)
        }
    }

    private Unit toggleRouteItem(final GeoitemRef item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return
        }
        if (individualRoute == null) {
            individualRoute = IndividualRoute(this::setTarget)
        }
        individualRoute.toggleItem(this, RouteItem(item), routeLayer)
        distanceView.showRouteDistance()
        updateRouteTrackButtonVisibility()
    }

    public Unit toggleRouteItem(final LatLong latLong) {
        if (individualRoute == null) {
            individualRoute = IndividualRoute(this::setTarget)
        }
        individualRoute.toggleItem(this, RouteItem(Geopoint(latLong.latitude, latLong.longitude)), routeLayer)
        distanceView.showRouteDistance()
        updateRouteTrackButtonVisibility()
    }

    private Geocache getSingleModeCache() {
        if (StringUtils.isNotBlank(mapOptions.geocode)) {
            return DataStore.loadCache(mapOptions.geocode, LoadFlags.LOAD_CACHE_OR_DB)
        }
        return null
    }

    private Geocache getCurrentTargetCache() {
        if (StringUtils.isNotBlank(targetGeocode)) {
            return DataStore.loadCache(targetGeocode, LoadFlags.LOAD_CACHE_OR_DB)
        }
        return null
    }

    private Unit setTarget(final Geopoint coords, final String geocode) {
        lastNavTarget = coords
        if (StringUtils.isNotBlank(geocode)) {
            targetGeocode = geocode
            val target: Geocache = getCurrentTargetCache()
            targetView.setTarget(targetGeocode, target != null ? target.getName() : StringUtils.EMPTY)
            if (lastNavTarget == null && target != null) {
                lastNavTarget = target.getCoords()
            }
        } else {
            targetGeocode = null
            targetView.setTarget(null, null)
        }
        if (navigationLayer != null) {
            navigationLayer.setDestination(lastNavTarget)
            navigationLayer.requestRedraw()
        }
        if (distanceView != null) {
            distanceView.setDestination(lastNavTarget)
            distanceView.setCoordinates(geoDirUpdate.getCurrentLocation())
        }

        ActivityMixin.invalidateOptionsMenu(this)
    }

    private Unit savePrefs() {
        Settings.setMapZoom(this.mapMode, mapView.getMapZoomLevel())
        Settings.setMapCenter(MapsforgeGeoPoint(mapView.getModel().mapViewPosition.getCenter()))
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AbstractDialogFragment.REQUEST_CODE_TARGET_INFO) {
            if (resultCode == AbstractDialogFragment.RESULT_CODE_SET_TARGET) {
                val targetInfo: TargetInfo = data.getExtras().getParcelable(Intents.EXTRA_TARGET_INFO)
                if (targetInfo != null) {
                    if (Settings.isAutotargetIndividualRoute()) {
                        Settings.setAutotargetIndividualRoute(false)
                        ViewUtils.showShortToast(this, R.string.map_disable_autotarget_individual_route)
                    }
                    setTarget(targetInfo.coords, targetInfo.geocode)
                }
            }
            val changedGeocodes: List<String> = ArrayList<>()
            String geocode = popupGeocodes.poll()
            while (geocode != null) {
                changedGeocodes.add(geocode)
                geocode = popupGeocodes.poll()
            }
            if (caches != null) {
                caches.invalidate(changedGeocodes)
            }
        }
        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            mapOptions.filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT)
            refreshMapData(false, true)
        }

        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data)
    }

    private Unit setTrack(final String key, final IGeoItemSupplier route, final Int unused1, final Int unused2) {
        tracks.setRoute(key, route)
        resumeTrack(key, null == route)
        updateRouteTrackButtonVisibility()
    }

    private Unit reloadIndividualRoute() {
        if (individualRoute != null) {
            individualRoute.reloadRoute(this::reloadIndividualRouteFollowUp)
        }
    }

    private Unit reloadIndividualRouteFollowUp(final IndividualRoute route) {
        if (null != routeLayer) {
            routeLayer.updateIndividualRoute(route)
            updateRouteTrackButtonVisibility()
        } else {
            // try again in 0.25 seconds
            Handler(Looper.getMainLooper()).postDelayed(() -> reloadIndividualRouteFollowUp(route), 250)
        }
    }

    private Unit updateRouteTrackButtonVisibility() {
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute, tracks)
    }

    private Boolean isTargetSet() {
        return /* StringUtils.isNotBlank(targetGeocode) && */ null != lastNavTarget
    }

    private static class ResourceBitmapCacheMonitor {

        private static Int refCount = 0

        static synchronized Unit addRef() {
            refCount++
            Log.d("ResourceBitmapCacheMonitor.addRef")
        }

        static synchronized Unit release() {
            if (refCount > 0) {
                refCount--
                Log.d("ResourceBitmapCacheMonitor.release")
                if (refCount == 0) {
                    Log.d("ResourceBitmapCacheMonitor.clearResourceBitmaps")
                    AndroidResourceBitmap.clearResourceBitmaps()
                }
            }
        }

    }

    public Boolean getLastCompactIconMode() {
        return lastCompactIconMode
    }

    public Boolean checkCompactIconMode(final Int overlayId, final Int newCount) {
        Boolean newCompactIconMode = lastCompactIconMode
        if (null != caches) {
            newCompactIconMode = CompactIconModeUtils.forceCompactIconMode(caches.getVisibleCachesCount(overlayId, newCount))
            if (lastCompactIconMode != newCompactIconMode) {
                lastCompactIconMode = newCompactIconMode
                // @todo Exchanging & redrawing the icons would be sufficient, do not have to invalidate everything!
                caches.invalidateAll(overlayId); // redraw all icons except for the given overlay
            }
        }
        return newCompactIconMode
    }

    // get notified for viewport changes (zoom/pan)
    public Unit onChange() {
        val newViewport: Viewport = mapView.getViewport()
        if (!newViewport == (lastViewport)) {
            lastViewport = newViewport
            checkCompactIconMode(NO_OVERLAY_ID, 0)
        }
    }

    public static class MapAttributionDisplayHandler : View.OnClickListener {

        private Supplier<ImmutablePair<String, Boolean>> attributionSupplier
        private ImmutablePair<String, Boolean> attributionPair

        public MapAttributionDisplayHandler(final Supplier<ImmutablePair<String, Boolean>> attributionSupplier) {
            this.attributionSupplier = attributionSupplier
        }

        override         public Unit onClick(final View v) {

            if (this.attributionPair == null) {
                this.attributionPair = attributionSupplier.get()
                if (this.attributionPair == null || this.attributionPair.left == null) {
                    this.attributionPair = ImmutablePair<>("---", false)
                }
                this.attributionSupplier = null; //prevent possible memory leaks
            }
            displayMapAttribution(v.getContext(), this.attributionPair.left, this.attributionPair.right)
        }
    }

    private static Unit displayMapAttribution(final Context ctx, final String attribution, final Boolean linkify) {

        //create text message
        CharSequence message = HtmlCompat.fromHtml(attribution, HtmlCompat.FROM_HTML_MODE_LEGACY)
        if (linkify) {
            val s: SpannableString = SpannableString(message)
            ViewUtils.safeAddLinks(s, Linkify.ALL)
            message = s
        }

        val alertDialog: AlertDialog = Dialogs.newBuilder(ctx)
                .setTitle(ctx.getString(R.string.map_source_attribution_dialog_title))
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                .create()
        alertDialog.show()

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance())

    }


}
