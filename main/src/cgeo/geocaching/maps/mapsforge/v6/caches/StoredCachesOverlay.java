package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.mapsforge.map.layer.Layer;

public class StoredCachesOverlay extends AbstractCachesOverlay {

    private final Disposable timer;

    public StoredCachesOverlay(final NewMap map, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers, final GeocacheFilterContext filterContext) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers);
        setFilterContext(filterContext);
        this.timer = startTimer();
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
    }

    private static final class LoadTimerAction implements Runnable {

        @NonNull
        private final WeakReference<StoredCachesOverlay> overlayRef;
        private int previousZoom = -100;
        private Viewport previousViewport;

        LoadTimerAction(@NonNull final StoredCachesOverlay overlay) {
            this.overlayRef = new WeakReference<>(overlay);
        }

        @Override
        public void run() {
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
                        mapMoved(previousViewport, viewportNow);

                // save new values
                if (moved) {

                    previousZoom = zoomNow;
                    previousViewport = viewportNow;
                    overlay.load();
                    overlay.refreshed();
                } else if (!previousViewport.equals(viewportNow)) {
                    overlay.updateTitle();
                }
            } catch (final Exception e) {
                Log.w("StoredCachesOverlay.startLoadtimer.start", e);
            }
        }
    }

    private void load() {
        try {
            showProgress();

            final SearchResult searchResult = new SearchResult(DataStore.loadCachedInViewport(getViewport().resize(1.2)));

            final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);

            MapUtils.filter(cachesFromSearchResult, getFilterContext());

            // render
            update(cachesFromSearchResult);

        } finally {
            hideProgress();
        }
    }

    @Override
    public void onDestroy() {
        timer.dispose();

        super.onDestroy();
    }
}
