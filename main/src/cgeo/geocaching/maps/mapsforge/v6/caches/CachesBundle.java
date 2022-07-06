package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.location.WaypointDistanceInfo;
import cgeo.geocaching.maps.MapOptions;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CompactIconModeUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;

public class CachesBundle {

    private static final int CIRCLES_SEPARATOR = 0;
    private static final int WP_SEPARATOR = 1;
    private static final int BASE_SEPARATOR = 2;
    private static final int STORED_SEPARATOR = 3;
    private static final int LIVE_SEPARATOR = 4;

    public static final int NO_OVERLAY_ID = -1;
    private static final int WP_OVERLAY_ID = 0;
    private static final int BASE_OVERLAY_ID = 1;
    private static final int STORED_OVERLAY_ID = 2;
    private static final int LIVE_OVERLAY_ID = 3;

    private final MfMapView mapView;
    private final MapHandlers mapHandlers;

    private static final int INITIAL_ENTRY_COUNT = 200;
    private final Set<GeoEntry> geoEntries = Collections.synchronizedSet(new GeoEntrySet(INITIAL_ENTRY_COUNT));

    private WaypointsOverlay wpOverlay;
    private AbstractCachesOverlay baseOverlay;
    private AbstractCachesOverlay storedOverlay;
    private LiveCachesOverlay liveOverlay;
    private final List<SeparatorLayer> separators = new ArrayList<>();
    private boolean mapModeSingle = false;

    /**
     * Base initialization without any caches up-front
     *
     * @param mapView     the map view this bundle is displayed on
     * @param mapHandlers the handlers of the map to send events to
     */
    public CachesBundle(final NewMap map, final MfMapView mapView, final MapHandlers mapHandlers) {
        this.mapView = mapView;
        this.mapHandlers = mapHandlers;

        // prepare separators
        final SeparatorLayer separator1 = new SeparatorLayer();
        this.separators.add(separator1);
        this.mapView.getLayerManager().getLayers().add(separator1);
        final SeparatorLayer separator2 = new SeparatorLayer();
        this.separators.add(separator2);
        this.mapView.getLayerManager().getLayers().add(separator2);
        final SeparatorLayer separator3 = new SeparatorLayer();
        this.separators.add(separator3);
        this.mapView.getLayerManager().getLayers().add(separator3);
        final SeparatorLayer separator4 = new SeparatorLayer();
        this.separators.add(separator4);
        this.mapView.getLayerManager().getLayers().add(separator4);
        final SeparatorLayer separator5 = new SeparatorLayer();
        this.separators.add(separator5);
        this.mapView.getLayerManager().getLayers().add(separator5);

        this.wpOverlay = new WaypointsOverlay(map, WP_OVERLAY_ID, this.geoEntries, this, separators.get(WP_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with search result (nearby, list)
     *
     * @param search      the SearchResult to display through this bundle
     * @param mapView     the map view this bundle is displayed on
     * @param mapHandlers the handlers of the map to send events to
     */
    public CachesBundle(final NewMap map, final SearchResult search, final MfMapView mapView, final MapHandlers mapHandlers) {
        this(map, mapView, mapHandlers);
        this.baseOverlay = new CachesOverlay(map, search, BASE_OVERLAY_ID, this.geoEntries, this, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with single cache
     *
     * @param geocode     the geocode for single cache display through this bundle
     * @param mapView     the map view this bundle is displayed on
     * @param mapHandlers the handlers of the map to send events to
     */
    public CachesBundle(final NewMap map, final String geocode, final MfMapView mapView, final MapHandlers mapHandlers) {
        this(map, mapView, mapHandlers);
        this.mapModeSingle = true;
        this.baseOverlay = new CachesOverlay(map, geocode, BASE_OVERLAY_ID, this.geoEntries, this, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with single waypoint
     *
     * @param coords       coordinates for single waypoint to display through this bundle
     * @param waypointType type for single waypoint to display through this bundle
     * @param mapView      the map view this bundle is displayed on
     * @param mapHandlers  the handlers of the map to send events to
     */
    public CachesBundle(final NewMap map, final Geopoint coords, final WaypointType waypointType, final MfMapView mapView, final MapHandlers mapHandlers, @Nullable final String geocode) {
        this(map, mapView, mapHandlers);
        this.baseOverlay = new SinglePointOverlay(map, coords, waypointType, BASE_OVERLAY_ID, this.geoEntries, this, separators.get(BASE_SEPARATOR), this.mapHandlers, geocode);
    }

    public void handleLiveLayers(final NewMap map, @NonNull final MapOptions mapOptions) {

        if (mapOptions.isLiveEnabled) {
            if (this.liveOverlay == null) {
                final SeparatorLayer separator2 = this.separators.get(LIVE_SEPARATOR);
                this.liveOverlay = new LiveCachesOverlay(map, LIVE_OVERLAY_ID, this.geoEntries, this, separator2, this.mapHandlers, mapOptions.filterContext);
            }
        } else {
            // Disable only download, keep stored caches
            if (this.liveOverlay != null) {
                this.liveOverlay.onDestroy();
                this.liveOverlay = null;
            }
        }
    }

    /**
     * Enables the stored cache layer. No disabling again!
     *
     * @param enable true - enable stored layer, false - leave untouched
     */
    public void handleStoredLayers(final NewMap map, @NonNull final MapOptions mapOptions) {
        if (!mapOptions.isStoredEnabled || this.storedOverlay != null) {
            return;
        }

        final SeparatorLayer separator1 = this.separators.get(STORED_SEPARATOR);
        this.storedOverlay = new StoredCachesOverlay(map, STORED_OVERLAY_ID, this.geoEntries, this, separator1, this.mapHandlers, mapOptions.filterContext);
    }

    public void onDestroy() {
        if (this.baseOverlay != null) {
            this.baseOverlay.onDestroy();
            this.baseOverlay = null;
        }
        if (this.storedOverlay != null) {
            this.storedOverlay.onDestroy();
            this.storedOverlay = null;
        }
        if (this.liveOverlay != null) {
            this.liveOverlay.onDestroy();
            this.liveOverlay = null;
        }
        if (this.wpOverlay != null) {
            this.wpOverlay.onDestroy();
            this.wpOverlay = null;
        }
        for (final SeparatorLayer layer : this.separators) {
            this.mapView.getLayerManager().getLayers().remove(layer);
        }
        this.separators.clear();
    }

    public int getVisibleCachesCount(final int exceptOverlayId, final int countForThisOverlay) {
        int result = countForThisOverlay;
        if (this.baseOverlay != null && exceptOverlayId != BASE_OVERLAY_ID) {
            result += this.baseOverlay.getVisibleCachesCount();
        }
        if (this.storedOverlay != null && exceptOverlayId != STORED_OVERLAY_ID) {
            result += this.storedOverlay.getVisibleCachesCount();
        }
        if (this.liveOverlay != null && exceptOverlayId != LIVE_OVERLAY_ID) {
            result += this.liveOverlay.getVisibleCachesCount();
        }
        return result;
    }

    public int getVisibleCachesCount() {
        return getVisibleCachesCount(NO_OVERLAY_ID, 0);
    }

    @NonNull
    public Set<String> getVisibleCacheGeocodes() {

        final Set<String> result = new HashSet<>();

        if (this.baseOverlay != null) {
            result.addAll(this.baseOverlay.getVisibleCacheGeocodes());
        }
        if (this.liveOverlay != null) {
            result.addAll(this.liveOverlay.getVisibleCacheGeocodes());
        }
        if (this.storedOverlay != null) {
            result.addAll(this.storedOverlay.getVisibleCacheGeocodes());
        }

        return result;
    }

    public int getCachesCount() {

        int result = 0;

        if (baseOverlay != null) {
            result += baseOverlay.getCachesCount();
        }
        if (storedOverlay != null) {
            result += storedOverlay.getCachesCount();
        }
        if (liveOverlay != null) {
            result += liveOverlay.getCachesCount();
        }

        return result;
    }

    public void invalidate() {
        if (storedOverlay != null) {
            storedOverlay.invalidate();
        }
        if (liveOverlay != null) {
            liveOverlay.invalidate();
        }
        if (wpOverlay != null) {
            wpOverlay.invalidate();
        }
        if (baseOverlay != null) {
            baseOverlay.invalidate();
        }
    }

    public void invalidate(final Collection<String> geocodes) {
        if (storedOverlay != null) {
            storedOverlay.invalidate(geocodes);
        }
        if (liveOverlay != null) {
            liveOverlay.invalidate(geocodes);
        }
        if (wpOverlay != null) {
            wpOverlay.invalidateWaypoints(geocodes);
        }
        if (baseOverlay != null) {
            baseOverlay.invalidate(geocodes);
        }
    }

    /**
     * Forces redraw of all cache layers (e.g. for icon change)
     */
    public void invalidateAll(final int exceptOverlay) {
        if (wpOverlay != null) {
            wpOverlay.invalidateAll();
        }
        if (baseOverlay != null && exceptOverlay != BASE_OVERLAY_ID) {
            baseOverlay.invalidateAll();
        }
        if (storedOverlay != null && exceptOverlay != STORED_OVERLAY_ID) {
            storedOverlay.invalidateAll();
        }
        if (liveOverlay != null && exceptOverlay != LIVE_OVERLAY_ID) {
            liveOverlay.invalidateAll();
        }
        if (wpOverlay != null) {
            wpOverlay.invalidateAll();
        }
    }

    public void switchCircles() {
        if (wpOverlay != null) {
            wpOverlay.switchCircles();
        }
        if (baseOverlay != null) {
            baseOverlay.switchCircles();
        }
        if (storedOverlay != null) {
            storedOverlay.switchCircles();
        }
        if (liveOverlay != null) {
            liveOverlay.switchCircles();
        }
    }

    public void setFilterContext(final GeocacheFilterContext filterContext) {
        if (wpOverlay != null) {
            wpOverlay.setFilterContext(filterContext);
        }
        if (baseOverlay != null) {
            baseOverlay.setFilterContext(filterContext);
        }
        if (storedOverlay != null) {
            storedOverlay.setFilterContext(filterContext);
        }
        if (liveOverlay != null) {
            liveOverlay.setFilterContext(filterContext);
        }
    }

    public boolean isDownloading() {
        return liveOverlay != null && liveOverlay.isDownloading();
    }

    Viewport getViewport() {
        return mapView.getViewport();
    }

    int getMapZoomLevel() {
        return mapView.getMapZoomLevel();
    }

    LayerManager getLayerManager() {
        return mapView.getLayerManager();
    }

    Layer getCirclesSeparator() {
        return separators.get(CIRCLES_SEPARATOR);
    }

    protected MfMapView getMapView() {
        return mapView;
    }

    public void handleWaypoints() {
        final int cachesToDisplay = getVisibleCachesCount();
        if (this.mapModeSingle || cachesToDisplay < Settings.getWayPointsThreshold()) {
            Collection<String> baseGeocodes = Collections.emptyList();
            if (baseOverlay != null) {
                baseGeocodes = baseOverlay.getCacheGeocodes();
            }
            final boolean showStored = storedOverlay != null;

            final boolean forceCompactIconMode = CompactIconModeUtils.forceCompactIconMode(cachesToDisplay);
            wpOverlay.showWaypoints(baseGeocodes, showStored, !this.mapModeSingle, forceCompactIconMode);
        } else {
            wpOverlay.hideWaypoints();
        }
    }

    public WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord) {
        WaypointDistanceInfo info = new WaypointDistanceInfo("", 50000000);
        WaypointDistanceInfo temp;
        if (baseOverlay != null) {
            temp = baseOverlay.getClosestDistanceInM(coord);
            if (temp.meters > 0 && temp.meters < info.meters) {
                info = temp;
            }
        }
        if (storedOverlay != null) {
            temp = storedOverlay.getClosestDistanceInM(coord);
            if (temp.meters > 0 && temp.meters < info.meters) {
                info = temp;
            }
        }
        if (liveOverlay != null) {
            temp = liveOverlay.getClosestDistanceInM(coord);
            if (temp.meters > 0 && temp.meters < info.meters) {
                info = temp;
            }
        }
        return info;
    }
}
