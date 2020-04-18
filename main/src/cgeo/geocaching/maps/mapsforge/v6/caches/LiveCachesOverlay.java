package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.mapsforge.map.layer.Layer;

public class LiveCachesOverlay extends AbstractCachesOverlay {

    private final Disposable timer;
    private boolean downloading = false;
    public long loadThreadRun = -1;

    public LiveCachesOverlay(final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, bundle, anchorLayer, mapHandlers);

        this.timer = startTimer();
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
    }

    private static final class LoadTimerAction implements Runnable {

        @NonNull
        private final WeakReference<LiveCachesOverlay> overlayRef;
        private int previousZoom = -100;
        private Viewport previousViewport;

        LoadTimerAction(@NonNull final LiveCachesOverlay overlay) {
            this.overlayRef = new WeakReference<>(overlay);
        }

        @Override
        public void run() {
            final LiveCachesOverlay overlay = overlayRef.get();
            if (overlay == null || overlay.isDownloading()) {
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
                    final long currentTime = System.currentTimeMillis();

                    if (1000 < (currentTime - overlay.loadThreadRun)) {
                        overlay.downloading = true;
                        previousZoom = zoomNow;
                        previousViewport = viewportNow;
                        overlay.download();
                    }
                } else if (!previousViewport.equals(viewportNow)) {
                    overlay.updateTitle();
                }
            } catch (final Exception e) {
                Log.w("LiveCachesOverlay.startLoadtimer.start", e);
            } finally {
                overlay.refreshed();
                overlay.downloading = false;
            }
        }
    }

    private void download() {
        try {
            showProgress();

            final SearchResult searchResult = ConnectorFactory.searchByViewport(getViewport());

            final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            MapUtils.filter(result);
            // update the caches
            // first remove filtered out
            final Set<String> filteredCodes = searchResult.getFilteredGeocodes();
            Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes.toString());
            DataStore.removeCaches(filteredCodes, EnumSet.of(RemoveFlag.CACHE));

            Log.d(String.format(Locale.ENGLISH, "Live caches found: %d", result.size()));

            //render
            update(result);

        } finally {
            hideProgress();
        }
    }

    @Override
    public void onDestroy() {
        timer.dispose();

        super.onDestroy();
    }

    public boolean isDownloading() {
        return downloading;
    }
}
