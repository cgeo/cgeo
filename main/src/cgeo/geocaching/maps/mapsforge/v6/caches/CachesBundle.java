package cgeo.geocaching.maps.mapsforge.v6.caches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;

/**
 * Created by rsudev on 11.12.15.
 */
public class CachesBundle {

    private final static int BASE_SEPARATOR = 0;
    private final static int STORED_SEPARATOR = 1;
    private final static int LIVE_SEPARATOR = 2;

    private final static int BASE_OVERLAY_ID = 0;
    private final static int STORED_OVERLAY_ID = 1;
    private final static int LIVE_OVERLAY_ID = 2;

    private final MfMapView mapView;
    private final MapHandlers mapHandlers;

    private static final int INITIAL_ENTRY_COUNT = 200;
    private final Set<GeoEntry> geoEntries = Collections.synchronizedSet(new HashSet<GeoEntry>(INITIAL_ENTRY_COUNT));

    private AbstractCachesOverlay baseOverlay;
    private AbstractCachesOverlay storedOverlay;
    private AbstractCachesOverlay liveOverlay;
    private final List<SeparatorLayer> separators = new ArrayList<>();

    /**
     * Base initialization without any caches up-front
     *
     * @param mapView
     * @param mapHandlers
     */
    public CachesBundle(MfMapView mapView, MapHandlers mapHandlers) {
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
     * @param mapView
     * @param mapHandlers
     */
    public CachesBundle(SearchResult search, MfMapView mapView, MapHandlers mapHandlers) {
        this(mapView, mapHandlers);
        this.baseOverlay = new CachesOverlay(search, BASE_OVERLAY_ID, this.geoEntries, this.mapView, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with single cache
     *
     * @param geocode
     * @param mapView
     * @param mapHandlers
     */
    public CachesBundle(String geocode, MfMapView mapView, MapHandlers mapHandlers) {
        this(mapView, mapHandlers);
        this.baseOverlay = new CachesOverlay(geocode, BASE_OVERLAY_ID, this.geoEntries, this.mapView, separators.get(BASE_SEPARATOR), this.mapHandlers);
    }

    /**
     * Initialization with single waypoint
     *
     * @param coords
     * @param waypointType
     * @param mapView
     * @param mapHandlers
     */
    public CachesBundle(Geopoint coords, WaypointType waypointType, MfMapView mapView, MapHandlers mapHandlers) {
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
}
