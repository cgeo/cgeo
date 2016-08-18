package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rsudev on 11.12.15.
 */
public class CachesBundle {

    private static final int BASE_SEPARATOR = 0;
    private static final int STORED_SEPARATOR = 1;
    private static final int LIVE_SEPARATOR = 2;

    private static final int BASE_OVERLAY_ID = 0;
    private static final int STORED_OVERLAY_ID = 1;
    private static final int LIVE_OVERLAY_ID = 2;

    private final MfMapView mapView;
    private final MapHandlers mapHandlers;

    private static final int INITIAL_ENTRY_COUNT = 200;
    private final Set<GeoEntry> geoEntries = Collections.synchronizedSet(new HashSet<GeoEntry>(INITIAL_ENTRY_COUNT));

    private AbstractCachesOverlay baseOverlay;
    private AbstractCachesOverlay storedOverlay;
    private LiveCachesOverlay liveOverlay;
    private final List<SeparatorLayer> separators = new ArrayList<>();

    /**
     * Base initialization without any caches up-front
     *
     * @param mapView
     *            the map view this bundle is displayed on
     * @param mapHandlers
     *            the handlers of the map to send events to
     */
    public CachesBundle(final MfMapView mapView, final MapHandlers mapHandlers) {
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
    }

    /**
     * Initialization with search result (nearby, list)
     *
     * @param search
     *            the SearchResult to display through this bundle
     * @param mapView
     *            the map view this bundle is displayed on
     * @param mapHandlers
     *            the handlers of the map to send events to
     */
    public CachesBundle(final SearchResult search, final MfMapView mapView, final MapHandlers mapHandlers) {
        this(mapView, mapHandlers);
        this.baseOverlay = new CachesOverlay(search, BASE_OVERLAY_ID, this.geoEntries, this.mapView, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with single cache
     *
     * @param geocode
     *            the geocode for single cache display through this bundle
     * @param mapView
     *            the map view this bundle is displayed on
     * @param mapHandlers
     *            the handlers of the map to send events to
     */
    public CachesBundle(final String geocode, final MfMapView mapView, final MapHandlers mapHandlers) {
        this(mapView, mapHandlers);
        this.baseOverlay = new CachesOverlay(geocode, BASE_OVERLAY_ID, this.geoEntries, this.mapView, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with single waypoint
     *
     * @param coords
     *            coordinates for single waypoint to display through this bundle
     * @param waypointType
     *            type for single waypoint to display through this bundle
     * @param mapView
     *            the map view this bundle is displayed on
     * @param mapHandlers
     *            the handlers of the map to send events to
     */
    public CachesBundle(final Geopoint coords, final WaypointType waypointType, final MfMapView mapView, final MapHandlers mapHandlers) {
        this(mapView, mapHandlers);
        this.baseOverlay = new SinglePointOverlay(coords, waypointType, BASE_OVERLAY_ID, this.geoEntries, this.mapView, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    public void handleLiveLayers(final boolean enable) {
        if (enable) {
            final SeparatorLayer separator1 = this.separators.get(STORED_SEPARATOR);
            this.storedOverlay = new StoredCachesOverlay(STORED_OVERLAY_ID, this.geoEntries, this.mapView, separator1, this.mapHandlers);
            final SeparatorLayer separator2 = this.separators.get(LIVE_SEPARATOR);
            this.liveOverlay = new LiveCachesOverlay(LIVE_OVERLAY_ID, this.geoEntries, this.mapView, separator2, this.mapHandlers);
        } else {
            if (this.storedOverlay != null) {
                this.storedOverlay.onDestroy();
                this.storedOverlay = null;
            }
            if (this.liveOverlay != null) {
                this.liveOverlay.onDestroy();
                this.liveOverlay = null;
            }
        }
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
        for (final SeparatorLayer layer : this.separators) {
            this.mapView.getLayerManager().getLayers().remove(layer);
        }
        this.separators.clear();
    }

    public int getVisibleItemsCount() {

        int result = 0;

        if (this.baseOverlay != null) {
            result += this.baseOverlay.getVisibleItemsCount();
        }
        if (this.storedOverlay != null) {
            result += this.storedOverlay.getVisibleItemsCount();
        }
        if (this.liveOverlay != null) {
            result += this.liveOverlay.getVisibleItemsCount();
        }

        return result;
    }

    public Set<String> getVisibleGeocodes() {

        final Set<String> result = new HashSet<>();

        if (this.liveOverlay != null) {
            result.addAll(this.liveOverlay.getVisibleGeocodes());
        }
        if (this.storedOverlay != null) {
            result.addAll(this.storedOverlay.getVisibleGeocodes());
        }

        return result;
    }

    public int getItemsCount() {

        int result = 0;

        if (baseOverlay != null) {
            result += baseOverlay.getItemsCount();
        }
        if (storedOverlay != null) {
            result += storedOverlay.getItemsCount();
        }
        if (liveOverlay != null) {
            result += liveOverlay.getItemsCount();
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
    }

    public void invalidate(final Collection<String> geocodes) {
        if (storedOverlay != null) {
            storedOverlay.invalidate(geocodes);
        }
        if (liveOverlay != null) {
            liveOverlay.invalidate(geocodes);
        }
    }

    public boolean isDownloading() {
        return liveOverlay != null && liveOverlay.isDownloading();
    }
}
