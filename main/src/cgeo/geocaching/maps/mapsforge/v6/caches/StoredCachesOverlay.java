package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import android.support.annotation.NonNull;
import org.mapsforge.map.layer.Layer;

import java.lang.ref.WeakReference;
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

        LoadTimerAction(@NonNull final StoredCachesOverlay overlay) {
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
                final boolean moved = overlay.isInvalidated() || previousViewport == null || zoomNow != previousZoom ||
                        mapMoved(previousViewport, viewportNow) || !previousViewport.includes(viewportNow);

                // save new values
                if (moved) {

                    previousZoom = zoomNow;
                    previousViewport = viewportNow;
                    overlay.load();
                    overlay.refreshed();
                }
            } catch (final Exception e) {
                Log.w("StoredCachesOverlay.startLoadtimer.start", e);
            }
        }
    }

    private void load() {
        try {
            showProgress();

            final SearchResult searchResult = new SearchResult(DataStore.loadCachedInViewport(getViewport().resize(1.2), Settings.getCacheType()));

            final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);

            filter(cachesFromSearchResult);

            // render
            fill(cachesFromSearchResult);

        } finally {
            hideProgress();
        }
    }

    @Override
    public void onDestroy() {
        timer.unsubscribe();

        super.onDestroy();
    }
}
