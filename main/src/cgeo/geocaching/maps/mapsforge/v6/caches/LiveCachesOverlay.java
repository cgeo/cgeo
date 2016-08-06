package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import android.support.annotation.NonNull;
import org.mapsforge.map.layer.Layer;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class LiveCachesOverlay extends AbstractCachesOverlay {

    private final Subscription timer;
    private boolean downloading = false;
    public long loadThreadRun = -1;
    private MapTokens tokens;

    public LiveCachesOverlay(final int overlayId, final Set<GeoEntry> geoEntries, final MfMapView mapView, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, mapView, anchorLayer, mapHandlers);

        this.timer = startTimer();
    }

    private Subscription startTimer() {
        return Schedulers.newThread().createWorker().schedulePeriodically(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
    }

    private static final class LoadTimerAction implements Action0 {

        @NonNull private final WeakReference<LiveCachesOverlay> overlayRef;
        private int previousZoom = -100;
        private Viewport previousViewport;

        LoadTimerAction(@NonNull final LiveCachesOverlay overlay) {
            this.overlayRef = new WeakReference<>(overlay);
        }

        @Override
        public void call() {
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
                        mapMoved(previousViewport, viewportNow) || !previousViewport.includes(viewportNow);

                // save new values
                if (moved) {
                    final long currentTime = System.currentTimeMillis();

                    if (1000 < (currentTime - overlay.loadThreadRun)) {
                        overlay.downloading = true;
                        previousZoom = zoomNow;
                        previousViewport = viewportNow;
                        overlay.download();
                    }
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

            if (Settings.isGCConnectorActive() && tokens == null) {
                tokens = GCLogin.getInstance().getMapTokens();
                if (StringUtils.isEmpty(tokens.getUserSession()) || StringUtils.isEmpty(tokens.getSessionToken())) {
                    tokens = null;
                    //TODO: show missing map token toast
                    //                    if (!noMapTokenShowed) {
                    //                        ActivityMixin.showToast(activity, res.getString(R.string.map_token_err));
                    //                        noMapTokenShowed = true;
                    //                    }
                }
            }
            final SearchResult searchResult = ConnectorFactory.searchByViewport(getViewport().resize(1.2), tokens);

            final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            AbstractCachesOverlay.filter(result);
            // update the caches
            // first remove filtered out
            final Set<String> filteredCodes = searchResult.getFilteredGeocodes();
            Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes.toString());
            DataStore.removeCaches(filteredCodes, EnumSet.of(RemoveFlag.CACHE));

            Log.d(String.format(Locale.ENGLISH, "Live caches found: %d", result.size()));

            //render
            fill(result);

        } finally {
            hideProgress();
        }
    }

    @Override
    public void onDestroy() {
        timer.unsubscribe();

        super.onDestroy();
    }

    public boolean isDownloading() {
        return downloading;
    }
}
