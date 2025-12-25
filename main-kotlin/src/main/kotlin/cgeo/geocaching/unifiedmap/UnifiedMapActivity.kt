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

package cgeo.geocaching.unifiedmap

import cgeo.geocaching.AbstractDialogFragment
import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.FilteredActivity
import cgeo.geocaching.downloader.DownloaderUtils
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.gui.GeocacheFilterActivity
import cgeo.geocaching.list.AbstractList
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.location.WaypointDistanceInfo
import cgeo.geocaching.maps.MapOptions
import cgeo.geocaching.maps.MapSettingsUtils
import cgeo.geocaching.maps.MapStarUtils
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.PositionHistory
import cgeo.geocaching.maps.RouteTrackUtils
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.maps.routing.RoutingMode
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.MapSelectableItem
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.service.CacheDownloaderService
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.TargetDistanceComparator
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.CacheListActionBarChooser
import cgeo.geocaching.ui.GeoItemSelectorUtils
import cgeo.geocaching.ui.RepeatOnHoldListener
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ToggleItemType
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.dialog.SimplePopupMenu
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemTestLayer
import cgeo.geocaching.unifiedmap.layers.CacheCirclesLayer
import cgeo.geocaching.unifiedmap.layers.CoordsIndicatorLayer
import cgeo.geocaching.unifiedmap.layers.ElevationChart
import cgeo.geocaching.unifiedmap.layers.GeoItemsLayer
import cgeo.geocaching.unifiedmap.layers.GeofenceCirclesLayer
import cgeo.geocaching.unifiedmap.layers.IndividualRouteLayer
import cgeo.geocaching.unifiedmap.layers.NavigationTargetLayer
import cgeo.geocaching.unifiedmap.layers.PositionHistoryLayer
import cgeo.geocaching.unifiedmap.layers.PositionLayer
import cgeo.geocaching.unifiedmap.layers.TracksLayer
import cgeo.geocaching.unifiedmap.layers.WherigoLayer
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.CompactIconModeUtils
import cgeo.geocaching.utils.FilterUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.HistoryTrackUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.MenuUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.functions.Func1
import cgeo.geocaching.wherigo.WherigoGame
import cgeo.geocaching.wherigo.WherigoThingType
import cgeo.geocaching.wherigo.WherigoViewUtils
import cgeo.geocaching.wherigo.openwig.Zone
import cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT
import cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_LOWPOWER
import cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_PRECISE
import cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL
import cgeo.geocaching.settings.Settings.MAPROTATION_OFF
import cgeo.geocaching.unifiedmap.UnifiedMapType.BUNDLE_MAPTYPE
import cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_List
import cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_PlainMap
import cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_TargetCoords
import cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_TargetGeocode
import cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_Viewport
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.view.View.GONE

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.HashSet
import java.util.LinkedList
import java.util.List
import java.util.Optional
import java.util.Set
import java.util.concurrent.atomic.AtomicReference
import java.lang.Boolean.TRUE

import com.google.android.material.progressindicator.LinearProgressIndicator
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.collections4.IterableUtils
import org.apache.commons.lang3.StringUtils

class UnifiedMapActivity : AbstractNavigationBarMapActivity() : FilteredActivity, AbstractDialogFragment.TargetUpdateReceiver {

    // Activity should only contain display logic, everything else goes into the ViewModel

    private static val LOGPRAEFIX: String = "UnifiedMapActivity:"

    private static val STATE_ROUTETRACKUTILS: String = "routetrackutils"
    private static val ROUTING_SERVICE_KEY: String = "UnifiedMap"
    private static val REQUEST_CODE_LOG: Int = 1001

    private var viewModel: UnifiedMapViewModel = null
    private var tileProvider: AbstractTileProvider = null
    private var mapFragment: AbstractMapFragment = null
    private final List<GeoItemLayer<?>> layers = ArrayList<>()
    GeoItemLayer<String> clickableItemsLayer
    GeoItemLayer<String> nonClickableItemsLayer
    NavigationTargetLayer navigationTargetLayer = null
    Boolean forceCompactIcons = false

    private LocUpdater geoDirUpdate
    private val resumeDisposables: CompositeDisposable = CompositeDisposable()
    private final Int[] inFollowMyLocation = { 0 }

    private var routeTrackUtils: RouteTrackUtils = null
    private var elevationChartUtils: ElevationChart = null
    private var lastElevationChartRoute: String = null; // null=none, empty=individual route, other=track

    //private var overridePositionAndZoom: Boolean = false; // to preserve those on config changes in favour to mapType defaults
    private enum class CacheReloadState { REFRESH, INITIALIZE, RESUME }
    private var cacheReloadState: CacheReloadState = CacheReloadState.REFRESH

    private Int wherigoListenerId
    private Menu toolbarMenu

    private val listChooser: CacheListActionBarChooser = CacheListActionBarChooser(this, this::getSupportActionBar, newListId -> {
        val lNew: Optional<AbstractList> = StoredList.UserInterface.getMenuLists(false, PseudoList.NEW_LIST.id).stream().filter(l2 -> l2.id == newListId).findFirst()
        if (lNew.isPresent()) {
            UnifiedMapType(newListId).launchMap(this)
            finish()
        }
    })

    private static WeakReference<UnifiedMapActivity> unifiedMapActivity = WeakReference<>(null)

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        acquireUnifiedMap(this)

        ActionBarUtils.setContentView(this, R.layout.unifiedmap_activity, true)
        if (null != findViewById(R.id.live_map_status)) {
            findViewById(R.id.live_map_status).getBackground().mutate()
        }

        viewModel = ViewModelProvider(this).get(UnifiedMapViewModel.class)

        // get data from intent
        val extras: Bundle = getIntent().getExtras()
        val extraMapType: UnifiedMapType = extras == null ? null : extras.getParcelable(BUNDLE_MAPTYPE)
        if (extraMapType != null) {
            viewModel.mapType = extraMapType
        }

        // set cache reload state according to whether this is a first map initialization or a resumte
        this.cacheReloadState = savedInstanceState == null ? CacheReloadState.INITIALIZE : CacheReloadState.RESUME

        viewModel.followMyLocation.setValue(viewModel.mapType.followMyLocation)

        viewModel.transientIsLiveEnabled.observe(this, live -> viewModel.liveMapHandler.setLiveEnabled(live))

        viewModel.transientIsLiveEnabled.setValue(viewModel.mapType.enableLiveMap() && Settings.isLiveMap())

        routeTrackUtils = RouteTrackUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS), this::centerMap, viewModel::clearIndividualRoute, viewModel::reloadIndividualRoute, viewModel::setTrack, this::isTargetSet)
        viewModel.configureProximityNotification()
        if (viewModel.proximityNotification.getValue() != null) {
            viewModel.proximityNotification.getValue().setTextNotifications(this)
        }

        viewModel.viewportIdle.observe(this, viewport -> viewModel.liveMapHandler.setViewport(viewport))
        viewModel.liveLoadStatus.observe(this, this::setLiveLoadStatus)

        viewModel.trackUpdater.observe(this, event -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue()))
        viewModel.individualRoute.observe(this, individualRoute -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute))
        viewModel.followMyLocation.observe(this, this::initFollowMyLocation)
        viewModel.location.observe(this, this::handleLocUpdate)

        //wherigo
        val view: View = findViewById(R.id.map_wherigo_popup)
        if (view != null) {
            view.setOnClickListener(v -> openWherigoPopup())
        }

        geoDirUpdate = LocUpdater(this)

        // initialize layers
        layers.clear()

        clickableItemsLayer = GeoItemLayer<>("clickableItems")
        nonClickableItemsLayer = GeoItemLayer<>("nonClickableItems"); // default layer for all map items not worth an own layer

        layers.add(clickableItemsLayer)
        layers.add(nonClickableItemsLayer)

        GeoItemTestLayer().initforUnifiedMap(clickableItemsLayer)

        PositionLayer(this, nonClickableItemsLayer)
        CoordsIndicatorLayer(this, nonClickableItemsLayer)
        PositionHistoryLayer(this, nonClickableItemsLayer)
        TracksLayer(this, clickableItemsLayer)
        navigationTargetLayer = NavigationTargetLayer(this, clickableItemsLayer)
        CacheCirclesLayer(this, nonClickableItemsLayer)
        GeofenceCirclesLayer(this, nonClickableItemsLayer)

        IndividualRouteLayer(this, clickableItemsLayer)
        GeoItemsLayer(this, clickableItemsLayer)

        WherigoLayer.get().setLayer(clickableItemsLayer)

        viewModel.init(routeTrackUtils)

        changeMapSource(Settings.getTileProvider())

        FilterUtils.initializeFilterBar(this, this)
        MapUtils.updateFilterBar(this, viewModel.mapType.filterContext)

        Routing.connect(ROUTING_SERVICE_KEY, () -> viewModel.reloadIndividualRoute(), this)
        viewModel.reloadIndividualRoute()

        CompactIconModeUtils.setCompactIconModeThreshold(getResources())

        viewModel.caches.observeForNotification(this, () -> {
            refreshListChooser()
            refreshWaypoints(viewModel)
        })
        viewModel.viewportIdle.observe(this, vp -> {
            refreshListChooser()
            refreshWaypoints(viewModel)
            refreshLiveStatusView()
        })
        viewModel.zoomLevel.observe(this, zoomLevel -> refreshListChooser())


        MapUtils.showMapOneTimeMessages(this, viewModel.mapType.type.compatibilityMapMode)

        getLifecycle().addObserver(GeocacheChangedBroadcastReceiver(this, true) {
            override             protected Unit onReceive(final Context context, final String geocode) {
                handleGeocodeChangedBroadcastReceived(geocode)
            }
        })

        getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, Intents.ACTION_INDIVIDUALROUTE_CHANGED) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                viewModel.reloadIndividualRoute()
            }
        })

        getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, Intents.ACTION_ELEVATIONCHART_CLOSED) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                lastElevationChartRoute = null
            }
        })
        add429observer()

        findViewById(R.id.map_zoomin).setOnTouchListener(RepeatOnHoldListener(500, v -> {
            if (mapFragment != null) {
                mapFragment.zoomInOut(true)
            }
        }))
        findViewById(R.id.map_zoomout).setOnTouchListener(RepeatOnHoldListener(500, v -> {
            if (mapFragment != null) {
                mapFragment.zoomInOut(false)
            }
        }))

        refreshListChooser()
    }

    public AbstractMapFragment getMapFragment() {
        return mapFragment
    }

    public List<GeoItemLayer<?>> getLayers() {
        return layers
    }

    private Unit changeMapSource(final AbstractTileProvider newSource) {
        val oldProvider: AbstractTileProvider = tileProvider
        val oldFragment: AbstractMapFragment = mapFragment

        if (oldProvider != null && oldFragment != null) {
            oldFragment.prepareForTileSourceChange()
        }
        if (this.cacheReloadState == CacheReloadState.REFRESH) {
            //for refresh of already initialized map, save current zoom/center and restore in map source
            saveCenterAndZoom()
            this.cacheReloadState = CacheReloadState.RESUME
        }
        tileProvider = newSource
        mapFragment = tileProvider.createMapFragment()
        Settings.setTileProvider(newSource); // store tileProvider, so that next getRenderTheme retrieves correct tileProvider-specific theme


        if (oldFragment != null) {
            mapFragment.init(oldFragment.getCurrentZoom(), oldFragment.getCenter(), () -> onMapReadyTasks(newSource))
        } else {
            mapFragment.init(Settings.getMapZoom(viewModel.mapType.type.compatibilityMapMode), null, () -> onMapReadyTasks(newSource))
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapViewFragment, mapFragment)
                .commit()
    }

    private Unit onMapReadyTasks(final AbstractTileProvider newSource) {
        TileProviderFactory.resetLanguages()
        mapFragment.setTileSource(newSource, false)
        Settings.setTileProvider(newSource)
        // map settings popup
        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, viewModel.individualRoute.getValue(), this::refreshMapDataAfterSettingsChanged, this::routingModeChanged, this::compactIconModeChanged, () -> viewModel.configureProximityNotification(), viewModel.mapType.filterContext))

        // routes / tracks popup
        findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(viewModel.individualRoute.getValue(), viewModel::setTarget, this::showElevationChart))
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue())

        // react to mapType
        viewModel.coordsIndicator.setValue(null)
        reloadCachesAndWaypoints()

        // both maps have invalid values for bounding box in the beginning, so need to delay counting a bit
        ActivityMixin.postDelayed(this::refreshListChooser, 2000)

        // refresh options menu and routes/tracks display
        invalidateOptionsMenu()
        setMapRotation(Settings.getMapRotation())
    }

    private Unit reloadCachesAndWaypoints() {
        val setDefaultCenterAndZoom: Boolean = this.cacheReloadState == CacheReloadState.INITIALIZE

        val mapType: UnifiedMapType = viewModel.mapType
        Log.d(LOGPRAEFIX + "reloadCachesAnddWaypoints, setZoom=" + setDefaultCenterAndZoom + ", mapType=" + mapType.type + ", filter=" + viewModel.mapType.filterContext)
        switch (mapType.type) {
            case UMTT_PlainMap:
                // restore last saved position and zoom
                if (setDefaultCenterAndZoom) {
                    mapFragment.setZoom(Settings.getMapZoom(viewModel.mapType.type.compatibilityMapMode))
                    mapFragment.setCenter(Settings.getUMMapCenter())
                }
                break
            case UMTT_Viewport:
                // set bounds to given viewport
                if (setDefaultCenterAndZoom && mapType.viewport != null) {
                    mapFragment.setCenter(mapType.viewport.getCenter())
                    mapFragment.zoomToBounds(mapType.viewport)
                }
                if (!isTargetSet() && mapType.coords != null && mapType.viewport.contains(mapType.coords)) {
                    onReceiveTargetUpdate(AbstractDialogFragment.TargetInfo(mapType.coords,
                            StringUtils.isNotBlank(mapType.title) ? mapType.title : "---"))
                }
                break
            case UMTT_TargetGeocode: // can be either a cache or a waypoint
                // load cache/waypoint, focus map on it, and set it as target
                val cache: Geocache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_WAYPOINTS)
                if (cache != null) {
                    if (mapType.waypointId > 0) { // single waypoint mode: display waypoint only
                        viewModel.caches.write(false, Set::clear)
                        val waypoint: Waypoint = cache.getWaypointById(mapType.waypointId)
                        if (waypoint != null) {
                            if (setDefaultCenterAndZoom) {
                                mapFragment.zoomToBounds(DataStore.getBounds(mapType.target, Settings.getZoomIncludingWaypoints()))
                                mapFragment.setCenter(waypoint.getCoords())
                            }
                            viewModel.waypoints.write(wps -> {
                                wps.clear()
                                wps.add(waypoint)
                            })
                            if (!isTargetSet()) {
                                onReceiveTargetUpdate(AbstractDialogFragment.TargetInfo(waypoint.getCoords(), waypoint.getName()))
                            }
                        }
                    } else { // geocache mode: display ONLY geocache and its waypoints, if coordinates are available
                        val cacheCoordinates: Geopoint = cache.getCoords()
                        val firstWaypoint: Waypoint = cacheCoordinates == null ? cache.getFirstMatchingWaypoint(wp -> wp.getCoords() != null) : null
                        val currentCoordinates: Geopoint = null != firstWaypoint ? firstWaypoint.getCoords() : cacheCoordinates

                        if (null != currentCoordinates) {
                            viewModel.caches.write(false, c -> {
                                c.clear()
                                // we can only display the cache if it has coordinates
                                if (null != cacheCoordinates) {
                                    c.add(cache)
                                }
                            })

                            viewModel.waypoints.write(wps -> {
                                wps.clear()
                                val waypoints: Set<Waypoint> = HashSet<>(cache.getWaypoints())
                                MapUtils.filter(waypoints, getFilterContext())
                                wps.addAll(waypoints)
                            })

                            if (setDefaultCenterAndZoom) {
                                // for cache with no coordinates (generated waypoints), we want to zoom to the waypoints
                                val zoomToWaypoints: Boolean = null == cacheCoordinates || Settings.getZoomIncludingWaypoints()
                                mapFragment.zoomToBounds(DataStore.getBounds(mapType.target, zoomToWaypoints))
                                mapFragment.setCenter(currentCoordinates)
                            }

                            if (!isTargetSet()) {
                                onReceiveTargetUpdate(AbstractDialogFragment.TargetInfo(currentCoordinates, cache.getGeocode()))
                            }
                        }
                    }
                    viewModel.waypoints.notifyDataChanged()
                }
                break
            case UMTT_TargetCoords:
                // set given coords as map center
                if (setDefaultCenterAndZoom) {
                    mapFragment.setCenter(mapType.coords)
                }
                viewModel.coordsIndicator.setValue(mapType.coords)
                break
            case UMTT_List:
                // load list of caches belonging to list and scale map to see them all
                val viewport3: AtomicReference<Viewport> = AtomicReference<>()
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                    val searchResult: SearchResult = DataStore.getBatchOfStoredCaches(null, mapType.fromList, mapType.filterContext.get(), null, false, -1)
                    viewport3.set(DataStore.getBounds(searchResult.getGeocodes(), Settings.getZoomIncludingWaypoints()))
                    replaceSearchResultByGeocaches(searchResult)
                }, () -> {
                    if (viewport3.get() != null) {
                        if (setDefaultCenterAndZoom) {
                            restoreOrSetViewport(viewport3.get())
                        }
                        refreshMapData(true)
                    }
                })
                break
            case UMTT_SearchResult:
                // load list of caches and scale map to see them all
                val viewport2: AtomicReference<Viewport> = AtomicReference<>()
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                    viewport2.set(DataStore.getBounds(mapType.searchResult.getGeocodes(), Settings.getZoomIncludingWaypoints()))
                    replaceSearchResultByGeocaches(mapType.searchResult)
                }, () -> {
                    if (viewport2.get() != null) {
                        if (setDefaultCenterAndZoom) {
                            restoreOrSetViewport(viewport2.get())
                        }

                        if (mapType.coords != null) {
                            if (setDefaultCenterAndZoom) {
                                mapFragment.setCenter(mapType.coords)
                            }
                            viewModel.coordsIndicator.setValue(mapType.coords)
                        }
                        refreshMapData(true)
                    }
                })
                break
            default:
                // nothing to do
                break
        }


        if (this.cacheReloadState == CacheReloadState.RESUME) {
            mapFragment.setZoom(Settings.getMapZoom(viewModel.mapType.type.compatibilityMapMode))
            mapFragment.setCenter(Settings.getUMMapCenter())
        }
        //reset cacheReloadState
        this.cacheReloadState = CacheReloadState.REFRESH

        refreshListChooser()

        // only initialize loadInBackgroundHandler if caches should actually be loaded
        if (mapType.enableLiveMap()) {
            refreshMapData(true)
        }
        viewModel.liveMapHandler.setEnabled(mapType.enableLiveMap())
    }

    private Unit restoreOrSetViewport(final Viewport vp) {
        val storedMapCenter: Geopoint = Settings.getUMMapCenter()
        if (Settings.getBoolean(R.string.pref_autozoom_consider_lastcenter, false) && vp.contains(storedMapCenter)) {
            mapFragment.setCenter(storedMapCenter)
        } else {
            mapFragment.zoomToBounds(vp)
        }
    }

    private Unit handleGeocodeChangedBroadcastReceived(final String geocode) {
        //add info from cache to viewmodel
        val changedCache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        if (changedCache == null || changedCache.getCoords() == null) {
            return
        }

        viewModel.caches.write(caches -> {
            //need to remove first to ensure cache is really swapped ("Geocache" uses an id-only "equals()" function)
            caches.remove(changedCache)
            caches.add(changedCache)
        })
        val cacheWaypoints: List<Waypoint> = DataStore.loadWaypoints(geocode)
        if (cacheWaypoints != null) {
            viewModel.waypoints.write(waypoints -> waypoints.addAll(cacheWaypoints))
        }

        if (viewModel.mapType.fromList != 0) {
            if (changedCache.getLists().contains(viewModel.mapType.fromList)) {
                AbstractList.getListById(viewModel.mapType.fromList).updateNumberOfCaches()
            }
        }

        //call reload logic -> this will reapply filters and such
        reloadCachesAndWaypoints()
    }

    private Unit compactIconModeChanged(final Int newValue) {
        Settings.setCompactIconMode(newValue)
        viewModel.caches.notifyDataChanged(true); // TODO: necessary?
    }

    private LiveMapGeocacheLoader.LoadState oldStatus

    public Unit setLiveLoadStatus(final LiveMapGeocacheLoader.LiveDataState observedStatus) {
        Log.d(LOGPRAEFIX + "set progress to " + observedStatus.loadState)
        refreshLiveStatusView()
    }

    private Unit refreshLiveStatusView() {

        final LiveMapGeocacheLoader.LiveDataState status = viewModel.liveLoadStatus.getValue()
        val viewport: Viewport = viewModel.viewport.getValue()
        val spinner: LinearProgressIndicator = findViewById(R.id.map_progressbar)
        val liveMapStatus: ImageView = findViewById(R.id.live_map_status)

        if (spinner == null || liveMapStatus == null || status == null || !Viewport.isValid(viewport)) {
            return
        }
        liveMapStatus.setOnClickListener(v -> showLiveStatusDialog())

        //hide status if we are live
        if (!TRUE == (viewModel.transientIsLiveEnabled.getValue())) {
            spinner.setVisibility(View.GONE)
            liveMapStatus.setVisibility(View.GONE)
            return
        }
        //set live map status
        if (status.isError() || status.isPartial(viewport)) {
            liveMapStatus.setImageResource(status.isError() ? R.drawable.ic_menu_error : R.drawable.ic_menu_partial)
            liveMapStatus.getBackground().setTint(getResources().getColor(status.isError() ? R.color.cacheMarker_archived : R.color.osm_zoomcontrol))
            liveMapStatus.setVisibility(View.VISIBLE)
        } else {
            liveMapStatus.setVisibility(View.GONE)
        }
        //set spinner
        switch (status.loadState) {
            case RUNNING:
                spinner.setVisibility(View.VISIBLE)
                spinner.setIndeterminate(true)
                break
            case STOPPED:
                spinner.setVisibility(View.GONE)
                break
            case REQUESTED:
                spinner.setVisibility(View.VISIBLE)
                spinner.setIndeterminate(false)
                if (oldStatus != status.loadState) {
                    val timer: CountDownTimer = CountDownTimer(LiveMapGeocacheLoader.PROCESS_DELAY, 20) {
                        override                         public Unit onTick(final Long millisUntilFinished) {
                            spinner.setProgress((Int) ((LiveMapGeocacheLoader.PROCESS_DELAY - millisUntilFinished) * 100 / LiveMapGeocacheLoader.PROCESS_DELAY))
                        }

                        override                         public Unit onFinish() {
                            spinner.setProgress(100)
                        }
                    }
                    timer.start()
                }
                break
            default:
                break
        }
        oldStatus = status.loadState
    }

    private Unit showLiveStatusDialog() {

        val viewport: Viewport = viewModel.viewport.getValue()
        final LiveMapGeocacheLoader.LiveDataState status = viewModel.liveLoadStatus.getValue()
        if (viewport == null || status == null) {
            return
        }

        //build message
        val errors: StringBuilder = StringBuilder()
        val partials: StringBuilder = StringBuilder()
        val normals: StringBuilder = StringBuilder()
        for (LiveMapGeocacheLoader.ConnectorState data : status.connectorStates.values()) {
            if (data.isError()) {
                errors.append("\n- ").append(data.toUserDisplayableStringWithMarkup())
            } else if (data.isPartialFor(viewport)) {
                partials.append("\n- ").append(data.toUserDisplayableStringWithMarkup())
            } else {
                normals.append("\n- ").append(data.toUserDisplayableStringWithMarkup())
            }
        }
        val errorMsg: String = errors.length() == 0 ? null : LocalizationUtils.getString(R.string.live_map_status_error, errors)
        val partialMsg: String = partials.length() == 0 ? null : LocalizationUtils.getString(R.string.live_map_status_partial, partials)
        val normalMsg: String = normals.length() == 0 ? null : LocalizationUtils.getString(R.string.live_map_status_normal, normals)
        val msgWithMarkup: String = TextUtils.join(Arrays.asList(errorMsg, partialMsg, normalMsg), s -> s, "\n\n").toString()
        val msg: CharSequence = TextParam.text(msgWithMarkup).setMarkdown(true).getText(null)

        SimpleDialog.ofContext(this).setMessage(TextParam.text(msg)).show()
    }


    public static Unit refreshWaypoints(final UnifiedMapViewModel viewModel) {

        if (viewModel.mapType.type == UMTT_TargetGeocode) { // single waypoint mode. No refresh in this case
            return
        }
        val filter: GeocacheFilter = viewModel.mapType.filterContext.get()
        val viewport: Viewport = viewModel.viewport.getValue()

        val waypoints: Set<Waypoint> = HashSet<>()

        if (viewModel.mapType.hasTarget()) {
            waypoints.addAll(viewModel.caches.readWithResult(caches -> {
                val wpSet: Set<Waypoint> = HashSet<>()
                val cache: Geocache = DataStore.loadCache(viewModel.mapType.target, LoadFlags.LOAD_WAYPOINTS)
                wpSet.addAll(cache.getWaypoints())
                return wpSet
            }))
        }

        if (viewModel.caches.readWithResult(viewport::count) < Settings.getWayPointsThreshold()) {
            //show all waypoints be displayed or just the ones from visible caches?
            if (viewModel.mapType.enableLiveMap()) {
                waypoints.addAll(DataStore.loadWaypoints(viewport))
            } else {
                waypoints.addAll(viewModel.caches.readWithResult(caches -> {
                    val wpSet: Set<Waypoint> = HashSet<>()
                    for (final Geocache c : caches) {
                        wpSet.addAll(c.getWaypoints())
                    }
                    return wpSet
                }))
            }
        }

        //filter waypoints
        MapUtils.filter(waypoints, filter)
        viewModel.waypoints.write(wps -> {
                wps.clear()
                wps.addAll(waypoints)
        })
    }


    public Unit replaceSearchResultByGeocaches(final SearchResult searchResult) {
        Log.d(LOGPRAEFIX + "replace " + searchResult.getGeocodes())
        viewModel.caches.write(true, Set::clear)
        val geocaches: Set<Geocache> = DataStore.loadCaches(searchResult.getGeocodes(), LoadFlags.LOAD_CACHE_OR_DB)
        CommonUtils.filterCollection(geocaches, cache -> cache != null && cache.getCoords() != null)
        if (!geocaches.isEmpty()) {
            viewModel.caches.write(true, caches -> { // use post to make it background capable
                caches.addAll(geocaches)
            })
        }
    }

    private String calculateTitle() {
        if (TRUE == (viewModel.transientIsLiveEnabled.getValue())) {
            return getString(R.string.map_live)
        }
        if (viewModel.mapType.type == UMTT_TargetGeocode) {
            val cache: Geocache = DataStore.loadCache(viewModel.mapType.target, LoadFlags.LOAD_CACHE_OR_DB)
            if (cache != null && cache.getCoords() != null) {
                return StringUtils.defaultIfBlank(cache.getName(), "")
            }
        }
        return StringUtils.defaultIfEmpty(viewModel.mapType.title, getString(R.string.map_offline))
    }

    private Unit refreshListChooser() {
        //try to get count of visible caches
        Int visibleCaches = 0
        if (mapFragment != null && mapFragment.getViewport() != null && viewModel != null) {
            visibleCaches = viewModel.caches.readWithResult(mapFragment.getViewport()::count)
        }

        if (viewModel.mapType.fromList != 0) {
            //List
            listChooser.setList(viewModel.mapType.fromList, visibleCaches, false)
        } else if (viewModel.mapType.type == UMTT_TargetGeocode) {
            //single cache
            val targetCache: Geocache = getCurrentTargetCache()
            if (targetCache != null) {
                listChooser.setDirect(calculateTitle(), Formatter.formatMapSubtitle(targetCache))
            } else {
                listChooser.setDirect(calculateTitle(), visibleCaches)
            }
        } else {
            //all others, e.g. "Live"
            listChooser.setDirect(calculateTitle(), visibleCaches)
        }

        val newForceCompactIcons: Boolean = CompactIconModeUtils.forceCompactIconMode(visibleCaches)
        if (newForceCompactIcons != forceCompactIcons) {
            forceCompactIcons = newForceCompactIcons
            refreshMapData(false)
        }
    }

    /**
     * centers map on coords given + resets "followMyLocation" state
     **/
    private Unit centerMap(final Geopoint geopoint) {
        viewModel.followMyLocation.setValue(false)
        mapFragment.setCenter(geopoint)
    }

    private Unit initFollowMyLocation(final Boolean followMyLocation) {
        synchronized (inFollowMyLocation) {
            if (mapFragment == null || inFollowMyLocation[0] > 0) {
                return
            }
            inFollowMyLocation[0]++
        }
        Settings.setFollowMyLocation(followMyLocation)

        val followMyLocationButton: ImageButton = findViewById(R.id.map_followmylocation_btn)
        if (followMyLocationButton != null) { // can be null after screen rotation
            followMyLocationButton.setImageResource(followMyLocation ? R.drawable.map_followmylocation_btn : R.drawable.map_followmylocation_off_btn)
            followMyLocationButton.setOnClickListener(v -> viewModel.followMyLocation.setValue(Boolean.FALSE == (viewModel.followMyLocation.getValue())))
        }

        if (followMyLocation) {
            val currentLocation: Location = LocationDataProvider.getInstance().currentGeo(); // get location even if none was delivered to the view-model yet
            mapFragment.setCenter(Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()))
        }
        checkDrivingMode()
        synchronized (inFollowMyLocation) {
            inFollowMyLocation[0]--
        }
    }

    private Unit handleLocUpdate(final LocUpdater.LocationWrapper locationWrapper) {
        // no need to handle location update if map fragment is gone
        if (mapFragment == null) {
            return
        }

        val mapRotation: Int = Settings.getMapRotation()
        if (locationWrapper.needsRepaintForHeading && (mapRotation == MAPROTATION_AUTO_LOWPOWER || mapRotation == MAPROTATION_AUTO_PRECISE)) {
            mapFragment.setBearing(locationWrapper.heading)
        }

        if (locationWrapper.needsRepaintForDistanceOrAccuracy && TRUE == (viewModel.followMyLocation.getValue())) {
            mapFragment.setCenter(Geopoint(locationWrapper.location))

            if (viewModel.proximityNotification.getValue() != null) {
                viewModel.proximityNotification.getValue().checkDistance(getClosestDistanceInM(Geopoint(locationWrapper.location.getLatitude(), locationWrapper.location.getLongitude()), viewModel))
            }
        }

        if (elevationChartUtils != null) {
            elevationChartUtils.showPositionOnTrack(Geopoint(locationWrapper.location.getLatitude(), locationWrapper.location.getLongitude()))
        }
    }

    private Unit checkDrivingMode() {
        val mapRotation: Int = Settings.getMapRotation()
        val shouldBeInDrivingMode: Boolean = TRUE == (viewModel.followMyLocation.getValue()) && (mapRotation == MAPROTATION_AUTO_LOWPOWER || mapRotation == MAPROTATION_AUTO_PRECISE)
        mapFragment.setDrivingMode(shouldBeInDrivingMode)
    }

    // ========================================================================
    // Routes, tracks and targets handling

    private Unit routingModeChanged(final RoutingMode newValue) {
        Settings.setRoutingMode(newValue)
        if ((null != viewModel.individualRoute && viewModel.individualRoute.getValue().getNumSegments() > 0) || null != viewModel.getTracks()) {
            ViewUtils.showShortToast(this, R.string.brouter_recalculating)
        }
        viewModel.reloadIndividualRoute()
        viewModel.reloadTracks(routeTrackUtils)
        if (navigationTargetLayer != null) {
            navigationTargetLayer.triggerRepaint()
        }
    }

    public Geocache getCurrentTargetCache() {
        final UnifiedMapViewModel.Target target = viewModel.target.getValue()
        if (target != null && StringUtils.isNotBlank(target.geocode)) {
            return DataStore.loadCache(target.geocode, LoadFlags.LOAD_CACHE_OR_DB)
        }
        return null
    }

    // glue method for old map
    // can be removed when removing CGeoMap and NewMap, routeTrackUtils need to be adapted then
    @SuppressWarnings("unused")
    private Unit centerMap(final Double latitude, final Double longitude, final Viewport viewport) {
        centerMap(Geopoint(latitude, longitude)); // todo consider viewport
    }

    private Unit updateRouteTrackButtonVisibility() {
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue(), viewModel.getTracks())
    }

    private Boolean isTargetSet() {
        return viewModel.target.getValue() != null && (viewModel.target.getValue().geopoint != null || !StringUtils.isEmpty(viewModel.target.getValue().geocode))
    }

    // ========================================================================
    // Bottom navigation methods

    override     public Int getSelectedBottomItemId() {
        return viewModel == null || viewModel.mapType == null || viewModel.mapType.type == UMTT_PlainMap ||
                viewModel.mapType.type == UMTT_Viewport || viewModel.mapType.type == UMTT_List || viewModel.mapType.type == UMTT_TargetCoords ? MENU_MAP : MENU_HIDE_NAVIGATIONBAR
    }

    // ========================================================================
    // Menu handling

    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        val result: Boolean = super.onPrepareOptionsMenu(menu)
        TileProviderFactory.addMapViewLanguageMenuItems(menu)
        ViewUtils.extendMenuActionBarDisplayItemCount(this, menu)
        HistoryTrackUtils.onPrepareOptionsMenu(menu)

        // init followMyLocation
        initFollowMyLocation(TRUE == (viewModel.followMyLocation.getValue()))

        // live map mode
        val itemMapLive: MenuItem = menu.findItem(R.id.menu_map_live)
        ToggleItemType.LIVE_MODE.toggleMenuItem(itemMapLive, TRUE == (viewModel.transientIsLiveEnabled.getValue()))
        itemMapLive.setVisible(true)

        val liveButton: View = findViewById(R.id.menu_map_live)
        if (liveButton != null) {
            liveButton.setOnLongClickListener(v -> {
                viewModel.mapType = UnifiedMapType.getPlainMapWithTarget(viewModel.mapType); // switch to PLAIN mode
                viewModel.transientIsLiveEnabled.setValue(false)
                Settings.setLiveMap(false)
                reloadCachesAndWaypoints()
                return true
            })
        }

        // map rotation state
        menu.findItem(R.id.menu_map_rotation).setVisible(true); // @todo: can be visible always (xml definition) when CGeoMap/NewMap is removed
        val mapRotation: Int = Settings.getMapRotation()
        switch (mapRotation) {
            case MAPROTATION_OFF:
                menu.findItem(R.id.menu_map_rotation_off).setChecked(true)
                break
            case MAPROTATION_MANUAL:
                menu.findItem(R.id.menu_map_rotation_manual).setChecked(true)
                break
            case MAPROTATION_AUTO_LOWPOWER:
                menu.findItem(R.id.menu_map_rotation_auto_lowpower).setChecked(true)
                break
            case MAPROTATION_AUTO_PRECISE:
                menu.findItem(R.id.menu_map_rotation_auto_precise).setChecked(true)
                break
            default:
                break
        }
        menu.findItem(R.id.menu_map_rotation_auto_precise).setVisible(true); // UnifiedMap supports high precision auto-rotate

        // map and theming options
        menu.findItem(R.id.menu_theme_mode).setVisible(tileProvider.supportsThemes())
        menu.findItem(R.id.menu_theme_options).setVisible(tileProvider.supportsThemeOptions())

        menu.findItem(R.id.menu_as_list).setVisible(true)

        MenuUtils.tintToolbarAndOverflowIcons(menu)

        return result
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        val result: Boolean = super.onCreateOptionsMenu(menu)
        getMenuInflater().inflate(R.menu.map_activity, menu)
        FilterUtils.initializeFilterMenu(this, this)
        MenuUtils.enableIconsInOverflowMenu(menu)
        this.toolbarMenu = menu
        initializeMapViewLongClick()
        return result
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val id: Int = item.getItemId()
        if (id == R.id.menu_map_live) {
            if (viewModel.mapType.enableLiveMap()) {
                Settings.setLiveMap(!Settings.isLiveMap())
                viewModel.transientIsLiveEnabled.setValue(Settings.isLiveMap())
                ActivityMixin.invalidateOptionsMenu(this)
                refreshListChooser()
                if (TRUE == (viewModel.transientIsLiveEnabled.getValue())) {
                    reloadCachesAndWaypoints()
                }
            } else {
                viewModel.mapType = UnifiedMapType.getPlainMapWithTarget(viewModel.mapType)
                viewModel.transientIsLiveEnabled.setValue(true)
                Settings.setLiveMap(true)
                refreshListChooser()
                MapUtils.updateFilterBar(this, viewModel.mapType.filterContext)
                updateSelectedBottomNavItemId()
                saveCenterAndZoom()
                val coordsIndicator: Geopoint = viewModel.coordsIndicator.getValue()
                onMapReadyTasks(tileProvider)
                viewModel.coordsIndicator.setValue(coordsIndicator)
            }
            //refresh live status view
            refreshLiveStatusView()
        } else if (id == R.id.menu_map_rotation_off) {
            setMapRotation(MAPROTATION_OFF)
        } else if (id == R.id.menu_map_rotation_manual) {
            setMapRotation(MAPROTATION_MANUAL)
        } else if (id == R.id.menu_map_rotation_auto_lowpower) {
            setMapRotation(MAPROTATION_AUTO_LOWPOWER)
        } else if (id == R.id.menu_map_rotation_auto_precise) {
            setMapRotation(MAPROTATION_AUTO_PRECISE)
        } else if (id == R.id.menu_check_routingdata) {
            val vp: Viewport = mapFragment.getViewportNonNull()
            MapUtils.checkRoutingData(this, vp.getLatitudeMin(), vp.getLongitudeMin(), vp.getLatitudeMax(), vp.getLongitudeMax())
        } else if (id == R.id.menu_check_hillshadingdata) {
            val vp: Viewport = mapFragment.getViewportNonNull()
            MapUtils.checkHillshadingData(this, vp.getLatitudeMin(), vp.getLongitudeMin(), vp.getLatitudeMax(), vp.getLongitudeMax())
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> viewModel.positionHistory.setValue(viewModel.positionHistory.getValue()), () -> {
            PositionHistory positionHistory = viewModel.positionHistory.getValue()
            if (positionHistory == null) {
                positionHistory = PositionHistory()
            }
            positionHistory.reset()
        })
                || DownloaderUtils.onOptionsItemSelected(this, id)) {
            return true
        } else if (id == R.id.menu_filter) {
            showFilterMenu()
        } else if (id == R.id.menu_store_caches) {
            val list: List<Geocache> = viewModel.caches.readWithResult(caches ->
                    mapFragment.getViewport().filter(caches))
            TargetDistanceComparator(LocationDataProvider.getInstance().currentGeo().getCoords()).sort(list)
            CacheDownloaderService.downloadCaches(this, Geocache.getGeocodes(list, ArrayList<>()), false, false, () -> viewModel.caches.notifyDataChanged(false))
        } else if (id == R.id.menu_theme_mode) {
            mapFragment.selectTheme(this)
        } else if (id == R.id.menu_theme_options) {
            mapFragment.selectThemeOptions(this)
        } else if (id == R.id.menu_routetrack) {
            routeTrackUtils.showPopup(viewModel.individualRoute.getValue(), viewModel::setTarget, this::showElevationChart)
        } else if (id == R.id.menu_select_mapview) {
            // dynamically create submenu to reflect possible changes in map sources
            val v: View = findViewById(R.id.menu_select_mapview)
            if (v != null) {
                val menu: PopupMenu = PopupMenu(this, v, Gravity.TOP)
                menu.inflate(R.menu.map_mapview)
                TileProviderFactory.addMapviewMenuItems(this, menu)
                menu.setOnMenuItemClickListener(this::onOptionsItemSelected)
                menu.setForceShowIcon(true)
                menu.show()
            }
        } else if (id == R.id.menu_as_list) {
            val caches: Collection<Geocache> = viewModel.caches.readWithResult(vmCaches ->
                    mapFragment.getViewport().filter(vmCaches))
            CacheListActivity.startActivityMap(this, SearchResult(caches))
        } else if (id == R.id.menu_hillshading) {
            Settings.setMapShadingShowLayer(!Settings.getMapShadingShowLayer())
            item.setChecked(Settings.getMapShadingShowLayer())
            changeMapSource(mapFragment.currentTileProvider)
        } else if (id == R.id.menu_backgroundmap) {
            Settings.setMapBackgroundMapLayer(!Settings.getMapBackgroundMapLayer())
            item.setChecked(Settings.getMapBackgroundMapLayer())
            changeMapSource(mapFragment.currentTileProvider)
        } else { // dynamic submenus: Map language, Map source
            val language: String = TileProviderFactory.getLanguage(id)
            val tileProviderLocal: AbstractTileProvider = TileProviderFactory.getTileProvider(id)
            if (language != null || id == MAP_LANGUAGE_DEFAULT_ID) {
                item.setChecked(true)
                Settings.setMapLanguage(language)
                mapFragment.setPreferredLanguage(language)
            } else if (tileProviderLocal != null) {
                item.setChecked(true)
                Settings.setPreviousTileProvider(tileProvider)
                changeMapSource(tileProviderLocal)
            }
            if (mapFragment.onOptionsItemSelected(item)) {
                return true
            } else {
                return super.onOptionsItemSelected(item)
            }
        }
        MenuUtils.tintToolbarAndOverflowIcons(toolbarMenu)
        return true
    }

    // ========================================================================
    // zoom, bearing & heading methods

    private Unit saveCenterAndZoom() {
        Settings.setMapZoom(viewModel.mapType.type.compatibilityMapMode, mapFragment.getCurrentZoom())
        Settings.setMapCenter(mapFragment.getCenter())
    }

    Unit setMapRotation(final Int mapRotation) {
        Settings.setMapRotation(mapRotation)
        mapFragment.setMapRotation(mapRotation)
        invalidateOptionsMenu()
        ViewUtils.setVisibility(findViewById(R.id.map_compassrose), mapRotation == MAPROTATION_OFF ? View.GONE : View.VISIBLE)
        checkDrivingMode()
    }

    /** to be called by map fragments' observers for zoom level change */
    public Unit notifyZoomLevel(final Float zoomLevel) {
        AndroidRxUtils.runOnUi(() -> viewModel.notifyZoomLevel(zoomLevel))
    }

    // ========================================================================

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            viewModel.mapType.filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT)
            refreshMapData(true)
        } else if (requestCode == REQUEST_CODE_LOG && resultCode == Activity.RESULT_OK && data != null) {
            ShareUtils.showLogPostedSnackbar(this, data, findViewById(R.id.activity_navigationBar))
        }
    }

    override     public Unit onReceiveTargetUpdate(final AbstractDialogFragment.TargetInfo targetInfo) {
        if (Settings.isAutotargetIndividualRoute()) {
            Settings.setAutotargetIndividualRoute(false)
            ViewUtils.showShortToast(this, R.string.map_disable_autotarget_individual_route)
        }
        viewModel.setTarget(targetInfo.coords, targetInfo.geocode)
    }

    // ========================================================================
    // Filter related methods

    override     public Unit showFilterMenu() {
        FilterUtils.openFilterActivity(this, viewModel.mapType.filterContext, viewModel.caches.getListCopy())
    }

    override     public Boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, viewModel.mapType.filterContext)
    }

    override     public Unit refreshWithFilter(final GeocacheFilter filter) {
        viewModel.mapType.filterContext.set(filter)
        onResume()
    }

    protected GeocacheFilterContext getFilterContext() {
        return viewModel.mapType.filterContext
    }

    private Unit refreshMapDataAfterSettingsChanged(final Boolean circlesSwitched, final Boolean filterChanged) {
        // parameter "circlesSwitched" is required for being called by showSettingsPopup only; can be removed after removing old map implementations
        if (!viewModel.liveMapHandler.isEnabled() && filterChanged) {
            reloadCachesAndWaypoints()
        }
        refreshMapData(filterChanged)
    }

    private Unit refreshMapData(final Boolean filterChanged) {
        Log.d(LOGPRAEFIX + "refreshMapData:filterChanged=" + filterChanged + ", mapType=" + viewModel.mapType.type + ", filter=" + viewModel.mapType.filterContext)
        //FIRST set filter in liveMapHandler (to prevent it from delivering results from old outdated filter)
        if (filterChanged) {
            viewModel.liveMapHandler.setFilter(viewModel.mapType.filterContext.get())
        }
        //THEN filter already existing caches
        viewModel.caches.write(false, caches -> MapUtils.filter(caches, viewModel.mapType.filterContext))
        viewModel.waypoints.notifyDataChanged()
        if (filterChanged) {
            MapUtils.updateFilterBar(this, viewModel.mapType.filterContext)
        }
    }

    // ========================================================================
    // Map tap handling

    public Unit onTap(final Int latitudeE6, final Int longitudeE6, final Int x, final Int y, final Boolean isLongTap) {

        val touchedPoint: Geopoint = Geopoint.forE6(latitudeE6, longitudeE6)

        // lookup elements touched by this
        val result: LinkedList<MapSelectableItem> = LinkedList<>()
        val touchedKeys: Set<String> = clickableItemsLayer.getTouched(Geopoint.forE6(latitudeE6, longitudeE6))
        val missedKeys: Set<String> = HashSet<>()

        for (String key : touchedKeys) {

            if (key.startsWith(UnifiedMapViewModel.CACHE_KEY_PREFIX)) {
                val geocode: String = key.substring(UnifiedMapViewModel.CACHE_KEY_PREFIX.length())

                Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
                //if not in CacheCache, try viewmodel cache. See #17492
                if (temp == null) {
                    temp = viewModel.caches.readWithResult(caches ->
                        IterableUtils.find(caches, cache -> geocode == (cache.getGeocode())))
                    //If found in viewmodel cache but not in CacheCache,
                    //-> then put into CacheCache for usage by Cache-Popup (popup will fail otherwise)
                    if (temp != null) {
                        DataStore.saveCache(temp, EnumSet.of(LoadFlags.SaveFlag.CACHE))
                    }
                }
                if (temp != null) {
                    result.add(MapSelectableItem(RouteItem(temp)))
                } else {
                    result.add(MapSelectableItem(null, geocode, "Geocache data missing", -1))
                }
            } else if (key.startsWith(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX) && isLongTap) { // only consider when tap was a longTap
                val identifier: String = key.substring(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX.length())

                Boolean found = false
                for (RouteItem item : viewModel.individualRoute.getValue().getRouteItems()) {
                    if (identifier == (item.getIdentifier())) {
                        found = true
                        result.add(MapSelectableItem(item))
                        break
                    }
                }
                if (!found) {
                    result.add(MapSelectableItem(null, "Route " + identifier, "Route data missing", -1))
                }
            } else if (key.startsWith(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX)) {
                val fullGpxId: String = key.substring(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX.length())
                val found: Boolean = viewModel.waypoints.readWithResult(wps -> {
                    for (Waypoint waypoint : wps) {
                        if (fullGpxId == (waypoint.getFullGpxId())) {
                            result.add(MapSelectableItem(RouteItem(waypoint)))
                            return true
                        }
                    }
                    return false
                })
                if (!found) {
                    result.add(MapSelectableItem(null, "FluuGpxId " + fullGpxId, "Waypoint not found", -1))
                }
            } else if (key.startsWith(IndividualRouteLayer.KEY_INDIVIDUAL_ROUTE) && isLongTap) {
                result.add(MapSelectableItem(viewModel.individualRoute.getValue()))
            } else if (key.startsWith(NavigationTargetLayer.KEY_TARGET_PATH) && isLongTap) {
                result.add(MapSelectableItem(viewModel.navigationTargetRoute.getValue()))
            } else if (key.startsWith(TracksLayer.TRACK_KEY_PREFIX) && viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute() is Route && isLongTap) {
                result.add(MapSelectableItem((Route) viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute()))
            } else if (key.startsWith(WherigoLayer.WHERIGO_KEY_PRAEFIX) && !isLongTap) {
                result.add(MapSelectableItem(WherigoGame.get().getZone(key.substring(WherigoLayer.WHERIGO_KEY_PRAEFIX.length())),
                        key.substring(WherigoLayer.WHERIGO_KEY_PRAEFIX.length()), // Zone name
                        WherigoGame.get().getCartridgeName(), // Wherigo
                        WherigoThingType.LOCATION.getIconId()))
            } else if (key.startsWith(GeoItemTestLayer.TESTLAYER_KEY_PREFIX)) {
                result.add(MapSelectableItem(key, "Test item: " + key.substring(GeoItemTestLayer.TESTLAYER_KEY_PREFIX.length()), clickableItemsLayer.get(key).getType().toString(), -1))
            } else {
                missedKeys.add(key)
            }
        }
        Log.iForce(LOGPRAEFIX + "TOUCHED [" + touchedPoint + "/" + isLongTap + "] (identified " + result.size() + " of " + touchedKeys.size() + ", missed: " + missedKeys + ")")
        Log.d(LOGPRAEFIX + "TOUCHED DETAILS: " + result)

        if (result.isEmpty()) {
            if (isLongTap) {
                viewModel.longTapCoords.setValue(touchedPoint)
                MapUtils.createMapLongClickPopupMenu(this, touchedPoint, Point(x, y), viewModel.individualRoute.getValue(), route -> viewModel.individualRoute.notifyDataChanged(), this::updateRouteTrackButtonVisibility, getCurrentTargetCache(), MapOptions(null, "", viewModel.mapType.fromList), viewModel::setTarget)
                        .setOnDismissListener(d -> viewModel.longTapCoords.setValue(null))
                        .show()
            } else {
                if (sheetRemoveFragment()) {
                    return
                }
                ActionBarUtils.toggleActionBar(this)
                GeoItemTestLayer.handleTapTest(clickableItemsLayer, this, touchedPoint, "", isLongTap)
            }
        } else if (result.size() == 1) {
            handleTap(result.get(0), touchedPoint, isLongTap, x, y)
        } else {
            try {
                val sorted: ArrayList<MapSelectableItem> = ArrayList<>(result)
                Collections.sort(sorted, MapSelectableItem.NAME_COMPARATOR)

                final SimpleDialog.ItemSelectModel<MapSelectableItem> model = SimpleDialog.ItemSelectModel<>()
                model
                        .setItems(sorted)
                        .setDisplayViewMapper((item, itemGroup, ctx, view, parent) ->
                                        GeoItemSelectorUtils.createMapSelectableItemView(UnifiedMapActivity.this, item, GeoItemSelectorUtils.getOrCreateView(UnifiedMapActivity.this, view, parent)),
                                (item, itemGroup) -> item == null ? "" : item.getSortFilterString())
                        .setItemPadding(0)

                SimpleDialog.of(this).setTitle(R.string.map_select_multiple_items).selectSingle(model, item -> handleTap(item, touchedPoint, isLongTap, x, y))

            } catch (final Resources.NotFoundException e) {
                Log.e(LOGPRAEFIX + "showSelection", e)
            }
        }

    }

    private Unit handleTap(final MapSelectableItem item, final Geopoint touchedPoint, final Boolean isLongTap, final Int tapX, final Int tapY) {
        val routeItem: RouteItem = item.isRouteItem() ? item.getRouteItem() : null
        if (isLongTap) {
            // toggle route item
            if (routeItem != null && Settings.isLongTapOnMapActivated()) {

                val cache: Geocache = routeItem.getGeocache()

                if (Settings.isShowRouteMenu()) {
                    val menu: SimplePopupMenu = MapUtils.createCacheWaypointLongClickPopupMenu(this, routeItem, tapX, tapY, viewModel.individualRoute.getValue(), viewModel, null)
                    if (MapStarUtils.canHaveStar(cache)) {
                        val geocode: String = routeItem.getGeocode()
                        viewModel.cachesWithStarDrawn.write(cwsd -> {
                            val isStarDrawn: Boolean = cwsd.contains(geocode)
                            MapStarUtils.addMenuIfNecessary(menu, cache, isStarDrawn, drawStar -> {
                                CommonUtils.addRemove(cwsd, geocode, !drawStar)
                                viewModel.cachesWithStarDrawn.notifyDataChanged()
                            })
                        })
                    }
                    menu.show()
                } else {
                    viewModel.toggleRouteItem(this, routeItem)
                }
            } else if (item.isRoute() && routeTrackUtils != null && item.getRoute() != null) {
                // individual route or track
                if (lastElevationChartRoute != null && StringUtils == (item.getRoute().getName(), lastElevationChartRoute)) {
                    elevationChartUtils.removeElevationChart()
                } else {
                    routeTrackUtils.showRouteTrackContextMenu(tapX, tapY, this::showElevationChart,
                            RouteTrackUtils.isIndividualRoute(item.getRoute()) ? viewModel.individualRoute.getValue() : item.getRoute(),
                            RouteTrackUtils.isNavigationTargetRoute(item.getRoute()) ? () -> viewModel.setTarget(null, null) : null)
                }
            }
        } else if (routeItem != null) {
            // open popup for element
            if (routeItem.getType() == RouteItem.RouteItemType.GEOCACHE) {
                viewModel.sheetInfo.setValue(UnifiedMapViewModel.SheetInfo(routeItem.getGeocode(), 0))
                sheetShowDetails(viewModel.sheetInfo.getValue())
                MapMarkerUtils.addHighlighting(routeItem.getGeocache(), getResources(), nonClickableItemsLayer)
            } else if (routeItem.getType() == RouteItem.RouteItemType.WAYPOINT && routeItem.getWaypointId() != 0) {
                viewModel.sheetInfo.setValue(UnifiedMapViewModel.SheetInfo(routeItem.getGeocode(), routeItem.getWaypointId()))
                sheetShowDetails(viewModel.sheetInfo.getValue())
                MapMarkerUtils.addHighlighting(routeItem.getWaypoint(), getResources(), nonClickableItemsLayer)
            }
        } else if (item.getData() is Zone) {
            WherigoViewUtils.displayThing(this, item.getData(), false)
        } else if (item.getData() is String) {
            GeoItemTestLayer.handleTapTest(clickableItemsLayer, this, touchedPoint, item.getData().toString(), isLongTap)
        } else if (item.getData() == null) {
            //error case
            SimpleDialog.of(this).setTitle(item.getName()).setMessage(item.getDescription()).show()
        }
    }

    private Unit hideElevationChart(final Boolean alsoClearManualTarget) {
        elevationChartUtils.removeElevationChart()
        lastElevationChartRoute = null

        if (alsoClearManualTarget) {
            viewModel.setTarget(null, null)
        }
    }

    private Unit showElevationChart(final Route item, final Boolean forceShowElevationChart) {
        // elevation charts for individual route and/or routes/tracks
        if (elevationChartUtils == null) {
            elevationChartUtils = ElevationChart(this, nonClickableItemsLayer)
        }
        if (forceShowElevationChart && lastElevationChartRoute != null) {
            hideElevationChart(false)
        }
        if (lastElevationChartRoute != null && StringUtils == (item.getName(), lastElevationChartRoute)) {
            elevationChartUtils.removeElevationChart()
        } else {
            elevationChartUtils.showElevationChart(item, routeTrackUtils, () -> hideElevationChart(RouteTrackUtils.isNavigationTargetRoute(item)))
            lastElevationChartRoute = item.getName()
            if (RouteTrackUtils.isIndividualRoute(item)) {
                viewModel.individualRoute.observe(this, individualRoute -> {
                    if (lastElevationChartRoute != null && lastElevationChartRoute.isEmpty()) { // still individual route being shown?
                        elevationChartUtils.showElevationChart(individualRoute, routeTrackUtils, () -> hideElevationChart(false))
                    }
                })
            } else if (RouteTrackUtils.isNavigationTargetRoute(item)) {
                viewModel.navigationTargetRoute.observe(this, navigationTargetRoute -> {
                    if (lastElevationChartRoute != null && lastElevationChartRoute == (item.getName())) { // still navigation target route being shown?
                        elevationChartUtils.showElevationChart(navigationTargetRoute, routeTrackUtils, () -> hideElevationChart(true))
                    }
                })
            } else {
                viewModel.trackUpdater.observe(this, event -> {
                    if (viewModel.getTracks().getRoute(event.peek()) is Route) {
                        val route: Route = (Route) viewModel.getTracks().getRoute(event.peek())
                        if (route != null && StringUtils == (lastElevationChartRoute, route.getName())) {
                            elevationChartUtils.showElevationChart(route, routeTrackUtils, () -> hideElevationChart(false))
                        }
                    }
                })
            }
        }
    }

    override     protected Unit clearSheetInfo() {
        MapMarkerUtils.removeHighlighting(nonClickableItemsLayer)
        viewModel.sheetInfo.setValue(null)
    }

    // ========================================================================
    // distance checks for proximity notifications
    private static WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord, final UnifiedMapViewModel viewModel) {
        WaypointDistanceInfo result
        // work on a copy to avoid race conditions
        result = getClosestDistanceInM(coord, viewModel.caches.getListCopy(), Integer.MAX_VALUE, item -> ((Geocache) item).getShortGeocode() + " " + item.getName())
        result = getClosestDistanceInM(coord, viewModel.waypoints.getListCopy(), result.meters, item -> item.getName() + " (" + ((Waypoint) item).getWaypointType().gpx + ")")
        return result
    }

    private static WaypointDistanceInfo getClosestDistanceInM(final Geopoint center, final List<? : INamedGeoCoordinate()> items, final Int minDistanceOld, final Func1<INamedGeoCoordinate, String> getName) {
        Int minDistance = minDistanceOld
        String name = ""
        for (INamedGeoCoordinate item : items) {
            val coords: Geopoint = item.getCoords()
            if (coords != null) {
                val distance: Int = (Int) (1000f * coords.distanceTo(center))
                if (distance > 0 && distance < minDistance) {
                    minDistance = distance
                    name = getName.call(item)
                }
            }
        }
        return WaypointDistanceInfo(name, minDistance)
    }

    // ========================================================================
    // Lifecycle methods

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState())
    }

    override     protected Unit onStart() {
        super.onStart()

        // close outdated details popups & restart sheet (if required)
        sheetManageLifecycleOnStart(viewModel.sheetInfo.getValue(), sheetInfo -> viewModel.sheetInfo.setValue(sheetInfo))

        // resume location access
        resumeDisposables.add(geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR))
    }

    override     protected Unit onStop() {
        this.resumeDisposables.clear()
        super.onStop()
    }

    override     public Unit onPause() {
        if (mapFragment != null) {
            saveCenterAndZoom()
        }
        if (tileProvider != null) {
            tileProvider.onPause()
        }
        if (!Settings.isFeatureEnabledDefaultTrue(R.string.pref_useDelayedMapFragment)) {
            destroyMapFragment()
        }
        WherigoGame.get().removeListener(wherigoListenerId)
        super.onPause()
    }

    public Unit destroyMapFragment() {
        if (mapFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(mapFragment).commitNowAllowingStateLoss()
        }
        mapFragment = null
    }

    public static Unit acquireUnifiedMap(final UnifiedMapActivity newUnifiedMapActivity) {
        if (!Settings.isFeatureEnabledDefaultTrue(R.string.pref_useDelayedMapFragment)) {
            return
        }
        val oldUnifiedMapActivity: UnifiedMapActivity = unifiedMapActivity.get()
        if (oldUnifiedMapActivity != null) {
            // may be optimized further to only destroy map fragment if it's a VTM instance
            oldUnifiedMapActivity.destroyMapFragment()
        }
        unifiedMapActivity = WeakReference<>(newUnifiedMapActivity)
    }

    override     protected Unit onResume() {
        if (mapFragment == null) {
            recreate(); // restart with a fresh MapView
        }

        super.onResume()
        reloadCachesAndWaypoints()
        MapUtils.updateFilterBar(this, viewModel.mapType.filterContext)
        
        if (Settings.removeFromRouteOnLog()) {
            viewModel.reloadIndividualRoute()
        }

        if (tileProvider != null) {
            tileProvider.onResume()
        }

        wherigoListenerId = WherigoGame.get().addListener(nt -> {
            val view: View = findViewById(R.id.container_wherigo)
            if (view != null) {
                view.setVisibility(WherigoGame.get().isPlaying() ? View.VISIBLE : GONE)
            }
        })
    }

    private Unit openWherigoPopup() {
        val dialog: Dialog = WherigoViewUtils.getQuickViewDialog(this)
        dialog.show()
    }

    override     protected Unit onDestroy() {
        if (tileProvider != null) {
            tileProvider.onDestroy()
        }
        super.onDestroy()
    }

    private Unit initializeMapViewLongClick() {
        Handler().post(() -> {
            val mapViewSelect: View = findViewById(R.id.menu_select_mapview)
            if (mapViewSelect != null) {
                mapViewSelect.setOnLongClickListener(v -> {
                    val localTileProvider: AbstractTileProvider = Settings.getPreviousTileProvider()
                    Settings.setPreviousTileProvider(tileProvider)
                    changeMapSource(localTileProvider)
                    return true
                })
            }
        })
    }
}
