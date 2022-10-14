package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.location.Viewport.containingGCliveCaches;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.mapsforge.map.layer.Layer;

public class LiveCachesOverlay extends AbstractCachesOverlay {

    private final Disposable timer;
    private boolean downloading = false;

    private SearchResult lastSearchResult = null;
    private Viewport lastViewport = null;

    public LiveCachesOverlay(final NewMap map, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers, final GeocacheFilterContext filterContext) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers);
        setFilterContext(filterContext);
        this.timer = startTimer();
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
    }

    private static final class LoadTimerAction implements Runnable {

        @NonNull
        private final WeakReference<LiveCachesOverlay> overlayRef;
        private int previousZoom = -100;
        private Viewport previousCycleViewport; //viewport on last timer cycle
        private Viewport previousMoveViewport; //viewport on last move
        private long lastMovedTimestamp = -1;

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
                final boolean moved = overlay.isInvalidated() || previousCycleViewport == null || zoomNow != previousZoom ||
                        previousMoveViewport == null || mapMoved(previousMoveViewport, viewportNow);

                // save new values
                if (moved) {
                    final long currentTime = System.currentTimeMillis();

                    if (1000 < (currentTime - lastMovedTimestamp)) {
                        overlay.downloading = true;
                        previousZoom = zoomNow;
                        overlay.download();
                        previousMoveViewport = viewportNow;
                        lastMovedTimestamp = System.currentTimeMillis();
                    }
                } else if (!previousCycleViewport.equals(viewportNow)) {
                    overlay.updateTitle();
                }
                previousCycleViewport = viewportNow;
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

            final boolean useLastSearchResult = null != lastSearchResult && null != lastViewport && lastViewport.includes(getViewport());
            final Viewport newViewport = getViewport().resize(3.0);
            final SearchResult searchResult = useLastSearchResult ? lastSearchResult : ConnectorFactory.searchByViewport(newViewport);

            final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            MapUtils.filter(result, getFilterContext());
            // update the caches
            // first remove filtered out
            final Set<String> filteredCodes = searchResult.getFilteredGeocodes();
            Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes);
            DataStore.removeCaches(filteredCodes, EnumSet.of(RemoveFlag.CACHE));

            Log.d(String.format(Locale.ENGLISH, "Live caches found: %d", result.size()));

            //render
            update(result);

            lastSearchResult = searchResult;
            if (null == lastViewport || !useLastSearchResult || (!result.isEmpty() && lastSearchResult.getCount() > 400)) {
                lastViewport = containingGCliveCaches(result);
            }
            Log.d("searchByViewport: cached=" + useLastSearchResult + ", results=" + lastSearchResult.getCount() + ", viewport=" + lastViewport);

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
