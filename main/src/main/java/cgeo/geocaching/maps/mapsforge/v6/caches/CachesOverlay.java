package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.mapsforge.map.layer.Layer;

public class CachesOverlay extends AbstractCachesOverlay {

    private final SearchResult search;
    private final Disposable timer;
    private boolean firstRun = true;
    private boolean updating = false;

    CachesOverlay(final NewMap map, final SearchResult search, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers);

        this.search = search;
        this.timer = startTimer();
    }

    CachesOverlay(final NewMap map, final String geocode, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer layerAnchor, final MapHandlers mapHandlers) {
        super(map, overlayId, geoEntries, bundle, layerAnchor, mapHandlers);

        this.search = new SearchResult();
        this.search.addGeocode(geocode);
        this.timer = startTimer();
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(new CachesOverlay.LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
    }

    private final class LoadTimerAction implements Runnable {

        @NonNull
        private final WeakReference<CachesOverlay> overlayRef;
        private Viewport previousViewport;

        LoadTimerAction(@NonNull final CachesOverlay overlay) {
            this.overlayRef = new WeakReference<>(overlay);
        }

        @Override
        public void run() {
            final CachesOverlay overlay = overlayRef.get();
            if (overlay == null || overlay.updating) {
                return;
            }
            overlay.updating = true;
            try {
                // Initially bring the main list in
                if (overlay.firstRun || overlay.isInvalidated()) {
                    final Set<Geocache> cachesToDisplay = overlay.search.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
                    MapUtils.filter(cachesToDisplay, getFilterContext());
                    overlay.display(cachesToDisplay);
                    overlay.firstRun = false;
                    overlay.refreshed();
                }

                // get current viewport
                final Viewport viewportNow = overlay.getViewport();

                if (previousViewport != null && !previousViewport.equals(viewportNow)) {
                    overlay.updateTitle();
                }
                previousViewport = viewportNow;
            } catch (final Exception e) {
                Log.w("CachesOverlay.LoadTimer.run", e);
            } finally {
                overlay.updating = false;
            }
        }
    }

    private void display(final Set<Geocache> cachesToDisplay) {
        try {
            showProgress();
            update(cachesToDisplay);
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
