package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import org.eclipse.jdt.annotation.NonNull;
import org.mapsforge.map.layer.Layer;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class StoredCachesOverlay extends AbstractCachesOverlay {

    private final Subscription timer;

    public StoredCachesOverlay(final int overlayId, final Set<GeoEntry> geoEntries, final MfMapView mapView, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, mapView, anchorLayer, mapHandlers);
        this.timer = startTimer();
    }

    private Subscription startTimer() {
        return Schedulers.newThread().createWorker().schedulePeriodically(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
    }

    private static final class LoadTimerAction implements Action0 {

        @NonNull private final WeakReference<StoredCachesOverlay> overlayRef;
        private int previousZoom = -100;
        private Viewport previousViewport;

        public LoadTimerAction(@NonNull final StoredCachesOverlay overlay) {
            this.overlayRef = new WeakReference<>(overlay);
        }

        @Override
        public void call() {
            final StoredCachesOverlay overlay = overlayRef.get();
            if (overlay == null) {
                return;
            }
            try {
                // get current viewport
                final Viewport viewportNow = overlay.getViewport();
                // Since zoomNow is used only for local comparison purposes,
                // it is ok to use the Google Maps compatible zoom level of OSM Maps
                final int zoomNow = overlay.getMapZoomLevel();

                // check if map moved or zoomed
                //TODO Portree Use Rectangle inside with bigger search window. That will stop reloading on every move
                final boolean moved = overlay.isInvalidated() || (previousViewport == null) || zoomNow != previousZoom ||
                        (mapMoved(previousViewport, viewportNow) || !previousViewport.includes(viewportNow));

                // update title on any change
                if (moved || !viewportNow.equals(previousViewport)) {
                    //                    map.displayHandler.sendEmptyMessage(UPDATE_TITLE);
                }
                previousZoom = zoomNow;

                // save new values
                if (moved) {

                        previousViewport = viewportNow;
                        overlay.load();
                    overlay.refreshed();
                }
            } catch (final Exception e) {
                Log.w("CGeoMap.startLoadtimer.start", e);
            } finally {
                //
            }
        }
    }

    private void load() {
        try {
            showProgress();

            final SearchResult searchResult = new SearchResult(DataStore.loadCachedInViewport(getViewport(), Settings.getCacheType()));

            final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);

            filter(cachesFromSearchResult);

            // render
            fill(cachesFromSearchResult);

        } finally {
            hideProgress();
        }
    }

    private void fill(final Set<Geocache> caches) {
        try {
            //            showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
            final Collection<String> removeCodes = getGeocodes();
            final Collection<String> newCodes = new HashSet<>();

            // display caches
            final Set<Geocache> cachesToDisplay = caches;

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
                            if (removeCodes.contains(waypoint.getGeocode())) {
                                removeCodes.remove(waypoint.getGeocode());
                            } else {
                                if (addItem(waypoint)) {
                                    newCodes.add(waypoint.getGpxId());
                                }
                            }
                        }
                    }

                    if (cache.getCoords() == null) {
                        continue;
                    }
                    if (removeCodes.contains(cache.getGeocode())) {
                        removeCodes.remove(cache.getGeocode());
                    } else {
                        if (addItem(cache)) {
                            newCodes.add(cache.getGeocode());
                        }
                    }
                }
            }

            syncLayers(removeCodes, newCodes);

            repaint();
        } finally {
            //            showProgressHandler.sendEmptyMessage(HIDE_PROGRESS);
        }
    }

    @Override
    public void onDestroy() {
        timer.unsubscribe();

        super.onDestroy();
    }

    private static boolean mapMoved(final Viewport referenceViewport, final Viewport newViewport) {
        return Math.abs(newViewport.getLatitudeSpan() - referenceViewport.getLatitudeSpan()) > 50e-6 ||
                Math.abs(newViewport.getLongitudeSpan() - referenceViewport.getLongitudeSpan()) > 50e-6 ||
                Math.abs(newViewport.center.getLatitude() - referenceViewport.center.getLatitude()) > referenceViewport.getLatitudeSpan() / 4 ||
                Math.abs(newViewport.center.getLongitude() - referenceViewport.center.getLongitude()) > referenceViewport.getLongitudeSpan() / 4;
    }

}
