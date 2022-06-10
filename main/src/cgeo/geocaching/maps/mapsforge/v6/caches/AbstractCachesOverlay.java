package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.location.WaypointDistanceInfo;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;

public abstract class AbstractCachesOverlay {

    private final int overlayId;
    private final Set<GeoEntry> geoEntries;
    private final WeakReference<CachesBundle> bundleRef;
    private final Layer anchorLayer;
    private final Layer circleLayer;
    private final GeoitemLayers layerList = new GeoitemLayers();
    private final MapHandlers mapHandlers;
    private boolean invalidated = true;
    private boolean showCircles;
    private GeocacheFilterContext filterContext;
    private final WeakReference<NewMap> mapRef;

    public AbstractCachesOverlay(final NewMap map, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        this.overlayId = overlayId;
        this.geoEntries = geoEntries;
        this.bundleRef = new WeakReference<>(bundle);
        this.anchorLayer = anchorLayer;
        this.mapHandlers = mapHandlers;
        this.circleLayer = bundle.getCirclesSeparator();
        this.showCircles = Settings.isShowCircles();
        mapRef = new WeakReference<>(map);
        Log.d(String.format(Locale.ENGLISH, "AbstractCacheOverlay: construct overlay %d", overlayId));
    }

    public void onDestroy() {
        Log.d(String.format(Locale.ENGLISH, "AbstractCacheOverlay: onDestroy overlay %d", overlayId));
        clearLayers();
    }

    Set<String> getVisibleCacheGeocodes() {
        final Set<String> geocodesInViewport = new HashSet<>();
        final CachesBundle bundle = bundleRef.get();
        if (bundle != null) {
            final Collection<Geocache> cachesInViewport = bundle.getViewport().filter(DataStore.loadCaches(getCacheGeocodes(), LoadFlags.LOAD_CACHE_OR_DB));
            for (final Geocache cache : cachesInViewport) {
                geocodesInViewport.add(cache.getGeocode());
            }

        }
        return geocodesInViewport;
    }

    int getVisibleCachesCount() {
        final CachesBundle bundle = bundleRef.get();
        if (bundle == null) {
            return 0;
        }
        return bundle.getViewport().count(DataStore.loadCaches(getCacheGeocodes(), LoadFlags.LOAD_CACHE_OR_DB));
    }

    int getCachesCount() {
        return layerList.getCacheCount();
    }

    @SuppressWarnings("unused")
    protected int getAllVisibleCachesCount() {
        final CachesBundle bundle = bundleRef.get();
        if (bundle == null) {
            return 0;
        }
        return bundle.getVisibleCachesCount();
    }

    public void invalidate() {
        invalidated = true;
        showCircles = Settings.isShowCircles();
    }

    public void invalidate(final Collection<String> invalidGeocodes) {
        removeItems(invalidGeocodes);
        invalidate();
    }

    public void invalidateAll() {
        removeItems(getGeocodes());
        invalidate();
    }

    protected boolean isInvalidated() {
        return invalidated;
    }

    protected void refreshed() {
        invalidated = false;
    }

    void switchCircles() {
        synchronized (this.bundleRef.get().getMapView()) {
            showCircles = Settings.isShowCircles();
            final Layers layers = getLayers();
            final int circleIndex;
            if (layers != null) {
                circleIndex = layers.indexOf(circleLayer) + 1;
                for (final GeoitemLayer layer : layerList) {
                    final Layer circle = layer.getCircle();
                    if (circle != null) {
                        if (showCircles) {
                            layers.add(circleIndex, circle);
                        } else {
                            layers.remove(circle);
                        }
                    }
                }
            }
        }
    }

    protected void setFilterContext(final GeocacheFilterContext filterContext) {
        this.filterContext = filterContext;
    }

    protected GeocacheFilterContext getFilterContext() {
        return filterContext;
    }

    protected void update(final Set<Geocache> cachesToDisplay) {

        final Collection<String> removeCodes = getGeocodes();
        final Collection<String> newCodes = new HashSet<>();

        if (!cachesToDisplay.isEmpty()) {
            final NewMap map = mapRef.get();
            final boolean lastCompactIconMode = null != map && map.getLastCompactIconMode();
            final boolean newCompactIconMode = null != map && map.checkCompactIconMode(overlayId, getViewport().count(cachesToDisplay));

            if (lastCompactIconMode != newCompactIconMode) {
                // remove all codes from this layer and restart
                syncLayers(removeCodes, newCodes);
                update(cachesToDisplay);
                return;
            }

            for (final Geocache cache : cachesToDisplay) {

                if (cache == null) {
                    continue;
                }

                if (cache.getCoords() == null || !cache.getCoords().isValid()) {
                    continue;
                }
                if (removeCodes.contains(cache.getGeocode())) {
                    removeCodes.remove(cache.getGeocode());
                } else if (addItem(cache, newCompactIconMode)) {
                    newCodes.add(cache.getGeocode());
                }
            }
        }

        syncLayers(removeCodes, newCodes);

        final CachesBundle bundle = bundleRef.get();
        if (bundle != null) {
            bundle.handleWaypoints();
        }

        repaint();
    }

    protected final boolean addItem(final Geocache cache, final boolean isDotMode) {
        final GeoEntry entry = new GeoEntry(cache.getGeocode(), overlayId);
        if (geoEntries.add(entry)) {
            layerList.add(getCacheItem(cache, this.mapHandlers.getTapHandler(), isDotMode));

            Log.d(String.format(Locale.ENGLISH, "Cache %s for id %d added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

            return true;
        }

        Log.d(String.format(Locale.ENGLISH, "Cache %s for id %d not added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

        return false;
    }

    protected final boolean addItem(final Waypoint waypoint, final boolean isDotMode) {
        final GeoEntry entry = new GeoEntry(waypoint.getGpxId(), overlayId);
        final GeoitemLayer waypointItem = getWaypointItem(waypoint, this.mapHandlers.getTapHandler(), isDotMode);
        if (waypointItem != null && geoEntries.add(entry)) {
            layerList.add(waypointItem);

            Log.d(String.format(Locale.ENGLISH, "Waypoint %s for id %d added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

            return true;
        }

        Log.d(String.format(Locale.ENGLISH, "Waypoint %s for id %d not added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

        return false;
    }

    protected void addLayers() {
        final Layers layers = getLayers();
        if (layers == null) {
            return;
        }
        synchronized (this.bundleRef.get().getMapView()) {
            int index = layers.indexOf(anchorLayer) + 1;
            final int circleIndex = layers.indexOf(circleLayer) + 1;
            for (final GeoitemLayer layer : layerList) {
                layers.add(index, layer);
                if (showCircles) {
                    final Layer circle = layer.getCircle();
                    if (circle != null) {
                        layers.add(circleIndex, circle);
                        index++;
                    }
                }
            }
        }
    }

    protected Collection<String> getGeocodes() {
        return layerList.getGeocodes();
    }

    protected Collection<String> getCacheGeocodes() {
        return layerList.getCacheGeocodes();
    }

    protected Viewport getViewport() {
        final CachesBundle bundle = this.bundleRef.get();
        if (bundle == null) {
            return null;
        }
        return bundle.getViewport();
    }

    protected int getMapZoomLevel() {
        final CachesBundle bundle = this.bundleRef.get();
        if (bundle == null) {
            return 0;
        }
        return bundle.getMapZoomLevel();
    }

    protected void showProgress() {
        mapHandlers.sendEmptyProgressMessage(NewMap.SHOW_PROGRESS);
    }

    protected void hideProgress() {
        mapHandlers.sendEmptyProgressMessage(NewMap.HIDE_PROGRESS);
    }

    protected void updateTitle() {
        mapHandlers.sendEmptyDisplayMessage(NewMap.UPDATE_TITLE);
        final CachesBundle bundle = this.bundleRef.get();
        if (bundle != null) {
            bundle.handleWaypoints();
        }
    }

    protected void repaint() {
        mapHandlers.sendEmptyDisplayMessage(NewMap.UPDATE_TITLE);
    }

    protected void clearLayers() {
        final Layers layers = getLayers();
        if (layers == null) {
            return;
        }

        synchronized (this.bundleRef.get().getMapView()) {
            for (final GeoitemLayer layer : layerList) {
                geoEntries.remove(new GeoEntry(layer.getItemCode(), overlayId));
                try {
                    layers.remove(layer);
                } catch (IllegalStateException e) {
                    Log.d("Ignored exception on layer removal", e);
                }
                final Layer circle = layer.getCircle();
                if (circle != null) {
                    layers.remove(circle);
                }
            }
        }

        layerList.clear();

        Log.d(String.format(Locale.ENGLISH, "Layers for id %d cleared, remaining geoEntries: %d", overlayId, geoEntries.size()));
    }

    protected void syncLayers(final Collection<String> removeCodes, final Collection<String> newCodes) {

        // check if there is something to do
        if (removeCodes.isEmpty() && newCodes.isEmpty()) {
            return;
        }

        final Layers layers = getLayers();
        if (layers == null) {
            return;
        }

        removeItems(removeCodes);
        synchronized (this.bundleRef.get().getMapView()) {
            int index = layers.indexOf(anchorLayer) + 1;
            final int circleIndex = layers.indexOf(circleLayer) + 1;
            for (final String code : newCodes) {
                final GeoitemLayer layer = layerList.getItem(code);
                layers.add(index, layer);
                if (showCircles) {
                    final Layer circle = layer.getCircle();
                    if (circle != null) {
                        layers.add(circleIndex, circle);
                        index++;
                    }
                }
            }
        }

        Log.d(String.format(Locale.ENGLISH, "Layers for id %d synced. Codes removed: %d, new codes: %d, geoEntries: %d", overlayId, removeCodes.size(), newCodes.size(), geoEntries.size()));
    }

    private void removeItems(final Collection<String> removeCodes) {
        final Layers layers = getLayers();
        if (layers == null) {
            return;
        }
        synchronized (this.bundleRef.get().getMapView()) {
            for (final String code : removeCodes) {
                final GeoitemLayer item = layerList.getItem(code);
                if (item != null) {
                    geoEntries.remove(new GeoEntry(code, overlayId));
                    layers.remove(item);
                    final Layer circle = item.getCircle();
                    if (circle != null) {
                        layers.remove(circle);
                    }
                    layerList.remove(item);
                }
            }
        }
    }

    private Layers getLayers() {
        final CachesBundle bundle = this.bundleRef.get();
        if (bundle == null) {
            return null;
        }
        final LayerManager layerManager = bundle.getLayerManager();
        if (layerManager == null) {
            return null;
        }
        return layerManager.getLayers();
    }

    static boolean mapMoved(final Viewport referenceViewport, final Viewport newViewport) {
        return Math.abs(newViewport.getLatitudeSpan() - referenceViewport.getLatitudeSpan()) > 50e-6 || Math.abs(newViewport.getLongitudeSpan() - referenceViewport.getLongitudeSpan()) > 50e-6 || Math.abs(newViewport.center.getLatitude() - referenceViewport.center.getLatitude()) > referenceViewport.getLatitudeSpan() / 4 || Math.abs(newViewport.center.getLongitude() - referenceViewport.center.getLongitude()) > referenceViewport.getLongitudeSpan() / 4;
    }

    private static GeoitemLayer getCacheItem(final Geocache cache, final TapHandler tapHandler, final boolean isDotMode) {
        final Geopoint target = cache.getCoords();
        final Bitmap marker;
        if (isDotMode) {
            marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getCacheDotMarker(CgeoApplication.getInstance().getResources(), cache).getDrawable());
        } else {
            marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, null).getDrawable());
        }
        return new GeoitemLayer(cache.getGeoitemRef(), cache.applyDistanceRule(), tapHandler, new LatLong(target.getLatitude(), target.getLongitude()), marker, 0, -marker.getHeight() / 2);
    }

    private static GeoitemLayer getWaypointItem(final Waypoint waypoint, final TapHandler tapHandler, final boolean isDotMode) {
        final Geopoint target = waypoint.getCoords();
        if (target != null && target.isValid()) {
            final Bitmap marker;
            if (isDotMode) {
                marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getWaypointDotMarker(CgeoApplication.getInstance().getResources(), waypoint).getDrawable());
            } else {
                marker = AndroidGraphicFactory.convertToBitmap(MapMarkerUtils.getWaypointMarker(CgeoApplication.getInstance().getResources(), waypoint, true).getDrawable());
            }
            return new GeoitemLayer(waypoint.getGeoitemRef(), waypoint.applyDistanceRule(), tapHandler, new LatLong(target.getLatitude(), target.getLongitude()), marker, 0, -marker.getHeight() / 2);
        }

        return null;
    }

    public WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord) {
        int minDistance = 50000000;
        String name = "";
        final Set<Geocache> caches = DataStore.loadCaches(getCacheGeocodes(), LoadFlags.LOAD_CACHE_OR_DB);
        for (final Geocache cache : caches) {
            final Geopoint cacheCoords = cache.getCoords();
            if (cacheCoords != null) {
                final int distance = (int) (1000f * cacheCoords.distanceTo(coord));
                if (distance > 0 && distance < minDistance) {
                    minDistance = distance;
                    name = cache.getShortGeocode() + " " + cache.getName();
                }
                final List<Waypoint> waypoints = cache.getWaypoints();
                for (final Waypoint waypoint : waypoints) {
                    final Geopoint wpCoords = waypoint.getCoords();
                    if (wpCoords != null) {
                        final int wpDistance = (int) (1000f * wpCoords.distanceTo(coord));
                        if (wpDistance > 0 && wpDistance < minDistance) {
                            minDistance = wpDistance;
                            name = waypoint.getName() + " (" + waypoint.getWaypointType().gpx + ")";
                        }
                    }
                }
            }
        }
        return new WaypointDistanceInfo(name, minDistance);
    }
}
