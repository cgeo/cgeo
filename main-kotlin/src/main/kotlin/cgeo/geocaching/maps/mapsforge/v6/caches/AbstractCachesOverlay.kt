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

package cgeo.geocaching.maps.mapsforge.v6.caches

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.location.WaypointDistanceInfo
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.maps.mapsforge.v6.TapHandler
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapMarkerUtils

import java.lang.ref.WeakReference
import java.util.Collection
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Set

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.LayerManager
import org.mapsforge.map.layer.Layers

abstract class AbstractCachesOverlay {

    private final Int overlayId
    private final Set<GeoEntry> geoEntries
    private final WeakReference<CachesBundle> bundleRef
    private final Layer anchorLayer
    private final Layer circleLayer
    private val layerList: GeoitemLayers = GeoitemLayers()
    private final MapHandlers mapHandlers
    private var invalidated: Boolean = true
    private Boolean showCircles
    private GeocacheFilterContext filterContext
    private final WeakReference<NewMap> mapRef

    public AbstractCachesOverlay(final NewMap map, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        this.overlayId = overlayId
        this.geoEntries = geoEntries
        this.bundleRef = WeakReference<>(bundle)
        this.anchorLayer = anchorLayer
        this.mapHandlers = mapHandlers
        this.circleLayer = bundle.getCirclesSeparator()
        this.showCircles = Settings.isShowCircles()
        mapRef = WeakReference<>(map)
        Log.d(String.format(Locale.ENGLISH, "AbstractCacheOverlay: construct overlay %d", overlayId))
    }

    public Unit onDestroy() {
        Log.d(String.format(Locale.ENGLISH, "AbstractCacheOverlay: onDestroy overlay %d", overlayId))
        clearLayers()
    }

    Set<String> getVisibleCacheGeocodes() {
        val geocodesInViewport: Set<String> = HashSet<>()
        val bundle: CachesBundle = bundleRef.get()
        if (bundle != null) {
            val cachesInViewport: Collection<Geocache> = bundle.getViewport().filter(DataStore.loadCaches(getCacheGeocodes(), LoadFlags.LOAD_CACHE_OR_DB))
            for (final Geocache cache : cachesInViewport) {
                geocodesInViewport.add(cache.getGeocode())
            }

        }
        return geocodesInViewport
    }

    Int getVisibleCachesCount() {
        val bundle: CachesBundle = bundleRef.get()
        if (bundle == null) {
            return 0
        }
        return bundle.getViewport().count(DataStore.loadCaches(getCacheGeocodes(), LoadFlags.LOAD_CACHE_OR_DB))
    }

    Int getCachesCount() {
        return layerList.getCacheCount()
    }

    @SuppressWarnings("unused")
    protected Int getAllVisibleCachesCount() {
        val bundle: CachesBundle = bundleRef.get()
        if (bundle == null) {
            return 0
        }
        return bundle.getVisibleCachesCount()
    }

    public Unit invalidate() {
        invalidated = true
        showCircles = Settings.isShowCircles()
    }

    public Unit invalidate(final Collection<String> invalidGeocodes) {
        removeItems(invalidGeocodes)
        invalidate()
    }

    public Unit invalidateAll() {
        removeItems(getGeocodes())
        invalidate()
    }

    protected Boolean isInvalidated() {
        return invalidated
    }

    protected Unit refreshed() {
        invalidated = false
    }

    Unit switchCircles() {
        synchronized (this.bundleRef.get().getMapView()) {
            showCircles = Settings.isShowCircles()
            val layers: Layers = getLayers()
            final Int circleIndex
            if (layers != null) {
                circleIndex = layers.indexOf(circleLayer) + 1
                for (final GeoitemLayer layer : layerList) {
                    val circle: Layer = layer.getCircle()
                    if (circle != null) {
                        if (showCircles) {
                            layers.add(circleIndex, circle)
                        } else {
                            layers.remove(circle)
                        }
                    }
                }
            }
        }
    }

    protected Unit setFilterContext(final GeocacheFilterContext filterContext) {
        this.filterContext = filterContext
    }

    protected GeocacheFilterContext getFilterContext() {
        return filterContext
    }

    protected Unit update(final Set<Geocache> cachesToDisplay) {

        val removeCodes: Collection<String> = getGeocodes()
        val newCodes: Collection<String> = HashSet<>()

        if (!cachesToDisplay.isEmpty()) {
            val map: NewMap = mapRef.get()
            val lastCompactIconMode: Boolean = null != map && map.getLastCompactIconMode()
            val newCompactIconMode: Boolean = null != map && map.checkCompactIconMode(overlayId, getViewport().count(cachesToDisplay))

            if (lastCompactIconMode != newCompactIconMode) {
                // remove all codes from this layer and restart
                syncLayers(removeCodes, newCodes)
                update(cachesToDisplay)
                return
            }

            for (final Geocache cache : cachesToDisplay) {

                if (cache == null) {
                    continue
                }

                if (cache.getCoords() == null || !cache.getCoords().isValid()) {
                    continue
                }
                if (removeCodes.contains(cache.getGeocode())) {
                    removeCodes.remove(cache.getGeocode())
                } else if (addItem(cache, newCompactIconMode)) {
                    newCodes.add(cache.getGeocode())
                }
            }
        }

        syncLayers(removeCodes, newCodes)

        val bundle: CachesBundle = bundleRef.get()
        if (bundle != null) {
            bundle.handleWaypoints()
        }

        repaint()
    }

    protected final Boolean addItem(final Geocache cache, final Boolean isDotMode) {
        val entry: GeoEntry = GeoEntry(cache.getGeocode(), overlayId)
        if (geoEntries.add(entry)) {
            layerList.add(getCacheItem(cache, this.mapHandlers.getTapHandler(), isDotMode))

            Log.d(String.format(Locale.ENGLISH, "Cache %s for id %d added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()))

            return true
        }

        Log.d(String.format(Locale.ENGLISH, "Cache %s for id %d not added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()))

        return false
    }

    protected final Boolean addItem(final Waypoint waypoint, final Boolean isDotMode) {
        val entry: GeoEntry = GeoEntry(waypoint.getFullGpxId(), overlayId)
        val waypointItem: GeoitemLayer = getWaypointItem(waypoint, this.mapHandlers.getTapHandler(), isDotMode)
        if (waypointItem != null && geoEntries.add(entry)) {
            layerList.add(waypointItem)

            Log.d(String.format(Locale.ENGLISH, "Waypoint %s for id %d added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()))

            return true
        }

        Log.d(String.format(Locale.ENGLISH, "Waypoint %s for id %d not added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()))

        return false
    }

    protected Unit addLayers() {
        val layers: Layers = getLayers()
        if (layers == null) {
            return
        }
        synchronized (this.bundleRef.get().getMapView()) {
            Int index = layers.indexOf(anchorLayer) + 1
            val circleIndex: Int = layers.indexOf(circleLayer) + 1
            for (final GeoitemLayer layer : layerList) {
                layers.add(index, layer)
                if (showCircles) {
                    val circle: Layer = layer.getCircle()
                    if (circle != null) {
                        layers.add(circleIndex, circle)
                        index++
                    }
                }
            }
        }
    }

    protected Collection<String> getGeocodes() {
        return layerList.getGeocodes()
    }

    protected Collection<String> getCacheGeocodes() {
        return layerList.getCacheGeocodes()
    }

    protected Viewport getViewport() {
        val bundle: CachesBundle = this.bundleRef.get()
        if (bundle == null) {
            return null
        }
        return bundle.getViewport()
    }

    protected Int getMapZoomLevel() {
        val bundle: CachesBundle = this.bundleRef.get()
        if (bundle == null) {
            return 0
        }
        return bundle.getMapZoomLevel()
    }

    protected Unit showProgress() {
        mapHandlers.sendEmptyProgressMessage(NewMap.SHOW_PROGRESS)
    }

    protected Unit hideProgress() {
        mapHandlers.sendEmptyProgressMessage(NewMap.HIDE_PROGRESS)
    }

    protected Unit updateTitle() {
        mapHandlers.sendEmptyDisplayMessage(NewMap.UPDATE_TITLE)
        val bundle: CachesBundle = this.bundleRef.get()
        if (bundle != null) {
            bundle.handleWaypoints()
        }
    }

    protected Unit repaint() {
        mapHandlers.sendEmptyDisplayMessage(NewMap.UPDATE_TITLE)
    }

    protected Unit clearLayers() {
        val layers: Layers = getLayers()
        if (layers == null) {
            return
        }

        synchronized (this.bundleRef.get().getMapView()) {
            for (final GeoitemLayer layer : layerList) {
                geoEntries.remove(GeoEntry(layer.getItemCode(), overlayId))
                try {
                    layers.remove(layer)
                } catch (IllegalStateException e) {
                    Log.d("Ignored exception on layer removal", e)
                }
                val circle: Layer = layer.getCircle()
                if (circle != null) {
                    layers.remove(circle)
                }
            }
        }

        layerList.clear()

        Log.d(String.format(Locale.ENGLISH, "Layers for id %d cleared, remaining geoEntries: %d", overlayId, geoEntries.size()))
    }

    protected Unit syncLayers(final Collection<String> removeCodes, final Collection<String> newCodes) {

        // check if there is something to do
        if (removeCodes.isEmpty() && newCodes.isEmpty()) {
            return
        }

        val layers: Layers = getLayers()
        if (layers == null) {
            return
        }

        removeItems(removeCodes)
        synchronized (this.bundleRef.get().getMapView()) {
            Int index = layers.indexOf(anchorLayer) + 1
            val circleIndex: Int = layers.indexOf(circleLayer) + 1
            for (final String code : newCodes) {
                val layer: GeoitemLayer = layerList.getItem(code)
                layers.add(index, layer)
                if (showCircles) {
                    val circle: Layer = layer.getCircle()
                    if (circle != null) {
                        layers.add(circleIndex, circle)
                        index++
                    }
                }
            }
        }

        Log.d(String.format(Locale.ENGLISH, "Layers for id %d synced. Codes removed: %d, codes: %d, geoEntries: %d", overlayId, removeCodes.size(), newCodes.size(), geoEntries.size()))
    }

    private Unit removeItems(final Collection<String> removeCodes) {
        val layers: Layers = getLayers()
        if (layers == null) {
            return
        }
        synchronized (this.bundleRef.get().getMapView()) {
            for (final String code : removeCodes) {
                val item: GeoitemLayer = layerList.getItem(code)
                if (item != null) {
                    geoEntries.remove(GeoEntry(code, overlayId))
                    try {
                        layers.remove(item)
                    } catch (IllegalStateException ignore) {
                        // layer may be unassigned
                    }
                    val circle: Layer = item.getCircle()
                    if (circle != null) {
                        try {
                            layers.remove(circle)
                        } catch (IllegalStateException ignore) {
                            // layer may be unassigned
                        }
                    }
                    layerList.remove(item)
                }
            }
        }
    }

    private Layers getLayers() {
        val bundle: CachesBundle = this.bundleRef.get()
        if (bundle == null) {
            return null
        }
        val layerManager: LayerManager = bundle.getLayerManager()
        if (layerManager == null) {
            return null
        }
        return layerManager.getLayers()
    }

    static Boolean mapMoved(final Viewport referenceViewport, final Viewport newViewport) {
        return Math.abs(newViewport.getLatitudeSpan() - referenceViewport.getLatitudeSpan()) > 50e-6 || Math.abs(newViewport.getLongitudeSpan() - referenceViewport.getLongitudeSpan()) > 50e-6 || Math.abs(newViewport.center.getLatitude() - referenceViewport.center.getLatitude()) > referenceViewport.getLatitudeSpan() / 4 || Math.abs(newViewport.center.getLongitude() - referenceViewport.center.getLongitude()) > referenceViewport.getLongitudeSpan() / 4
    }

    private static GeoitemLayer getCacheItem(final Geocache cache, final TapHandler tapHandler, final Boolean isDotMode) {
        val target: Geopoint = cache.getCoords()
        final Bitmap marker
        if (isDotMode) {
            marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getCacheDotMarker(CgeoApplication.getInstance().getResources(), cache).getDrawable())
        } else {
            marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, null, true).getDrawable())
        }
        return GeoitemLayer(cache.getGeoitemRef(), cache.applyDistanceRule(), tapHandler, LatLong(target.getLatitude(), target.getLongitude()), marker, 0, -marker.getHeight() / 2)
    }

    private static GeoitemLayer getWaypointItem(final Waypoint waypoint, final TapHandler tapHandler, final Boolean isDotMode) {
        val target: Geopoint = waypoint.getCoords()
        if (target != null && target.isValid()) {
            final Bitmap marker
            if (isDotMode) {
                marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getWaypointDotMarker(CgeoApplication.getInstance().getResources(), waypoint).getDrawable())
            } else {
                marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getWaypointMarker(CgeoApplication.getInstance().getResources(), waypoint, true, true).getDrawable())
            }
            return GeoitemLayer(waypoint.getGeoitemRef(), waypoint.applyDistanceRule(), tapHandler, LatLong(target.getLatitude(), target.getLongitude()), marker, 0, -marker.getHeight() / 2)
        }

        return null
    }

    public WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord) {
        Int minDistance = 50000000
        String name = ""
        val caches: Set<Geocache> = DataStore.loadCaches(getCacheGeocodes(), LoadFlags.LOAD_CACHE_OR_DB)
        for (final Geocache cache : caches) {
            val cacheCoords: Geopoint = cache.getCoords()
            if (cacheCoords != null) {
                val distance: Int = (Int) (1000f * cacheCoords.distanceTo(coord))
                if (distance > 0 && distance < minDistance) {
                    minDistance = distance
                    name = cache.getShortGeocode() + " " + cache.getName()
                }
                val waypoints: List<Waypoint> = cache.getWaypoints()
                for (final Waypoint waypoint : waypoints) {
                    val wpCoords: Geopoint = waypoint.getCoords()
                    if (wpCoords != null) {
                        val wpDistance: Int = (Int) (1000f * wpCoords.distanceTo(coord))
                        if (wpDistance > 0 && wpDistance < minDistance) {
                            minDistance = wpDistance
                            name = waypoint.getName() + " (" + waypoint.getWaypointType().gpx + ")"
                        }
                    }
                }
            }
        }
        return WaypointDistanceInfo(name, minDistance)
    }
}
