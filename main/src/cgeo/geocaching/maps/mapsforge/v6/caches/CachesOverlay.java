package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;

import org.mapsforge.map.layer.Layer;

import android.os.AsyncTask;

import java.util.List;
import java.util.Set;

public class CachesOverlay extends AbstractCachesOverlay {

    private final SearchResult search;

    public CachesOverlay(final SearchResult search, final int overlayId, final Set<GeoEntry> geoEntries, final MfMapView mapView, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, mapView, anchorLayer, mapHandlers);

        this.search = search;
        startDisplay();
    }

    public CachesOverlay(final String geocode, final int overlayId, final Set<GeoEntry> geoEntries, final MfMapView mapView, final Layer layerAnchor, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, mapView, layerAnchor, mapHandlers);

        this.search = new SearchResult();
        this.search.addGeocode(geocode);
        startDisplay();
    }

    private void startDisplay() {
        new Loader().execute(null, null, null);
    }

    private void fill() {
        try {
            showProgress();

            clearLayers();

            // display caches
            final Set<Geocache> cachesToDisplay = search.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);

            if (!cachesToDisplay.isEmpty()) {
                // Only show waypoints for single view or setting
                // when less than showWaypointsthreshold Caches shown
                final boolean showWaypoints = cachesToDisplay.size() == 1 || cachesToDisplay.size() < Settings.getWayPointsThreshold();

                for (final Geocache cache : cachesToDisplay) {

                    if (cache == null) {
                        continue;
                    }
                    if (showWaypoints) {
                        final List<Waypoint> waypoints = cache.getWaypoints();
                        for (final Waypoint waypoint : waypoints) {
                            if (waypoint == null || waypoint.getCoords() == null) {
                                continue;
                            }
                            addItem(waypoint);
                        }
                    }

                    if (cache.getCoords() == null) {
                        continue;
                    }
                    addItem(cache);
                }
            }

            addLayers();

            repaint();
        } finally {
            hideProgress();
        }
    }

    private class Loader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(final Void... arg0) {
            fill();
            return null;
        }

    }

}
