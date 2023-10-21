package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.location.Viewport.containingGCliveCaches;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

class LoadInBackgroundHandler {
    private final Disposable timer;
    private final WeakReference<UnifiedMapActivity> activityRef;

    LoadInBackgroundHandler(final UnifiedMapActivity activity) {
        timer = Schedulers.newThread().schedulePeriodicallyDirect(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
        activityRef = new WeakReference<>(activity);
    }

    private static final class LoadTimerAction implements Runnable {

        @NonNull
        private final WeakReference<LoadInBackgroundHandler> handlerRef;
        private int previousZoom = -100;
        private Viewport previousViewport;
        private SearchResult lastSearchResult = null;

        LoadTimerAction(final LoadInBackgroundHandler handler) {
            handlerRef = new WeakReference<>(handler);
        }

        @Override
        public void run() {
            final UnifiedMapActivity activity = getActivity(handlerRef.get());
            if (activity == null) {
                return;
            }
            final AbstractMapFragment map = activity.getMapFragment();
            if (map == null) {
                return;
            }

            try {
                // get current viewport & zoom level
                final Viewport currentViewport = map.getViewport();
                if (currentViewport.getLatitudeMin() == 0.0 && currentViewport.getLongitudeMin() == 0.0 && currentViewport.getLatitudeMax() == 0.0 && currentViewport.getLongitudeMax() == 0.0) {
                    return;
                }
                final int currentZoom = map.getCurrentZoom();

                // check if map moved or zoomed
                final boolean moved = previousViewport == null || currentZoom != previousZoom || mapMoved(previousViewport, currentViewport);
                if (moved) {
                    load(currentViewport);
                    previousZoom = currentZoom;
                    previousViewport = currentViewport;
                } else if (!previousViewport.equals(currentViewport)) {
                    // @todo: overlay.updateTitle();
                }
            } catch (final Exception e) {
                Log.w("LoadInBackground.run", e);
            }
        }

        private void load(final Viewport viewport) {
            final UnifiedMapActivity activity = getActivity(handlerRef.get());
            if (activity == null) {
                return;
            }
            try {
                new Handler(Looper.getMainLooper()).post(activity::showProgressSpinner);

                if (Settings.isLiveMap()) {
                    // retrieving live caches (if enabled)
                    final boolean useLastSearchResult = null != lastSearchResult && null != previousViewport && previousViewport.includes(viewport);
                    final Viewport newViewport = viewport.resize(3.0);
                    final SearchResult searchResult = useLastSearchResult ? lastSearchResult : ConnectorFactory.searchByViewport(newViewport, null /* filter */);

                    final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
/* @todo: filtering
                    MapUtils.filter(result, getFilterContext());
                    final Set<String> filteredCodes = searchResult.getFilteredGeocodes();
                    Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes);
                    DataStore.removeCaches(filteredCodes, EnumSet.of(LoadFlags.RemoveFlag.CACHE));
                    activity.addSearchResultByGeocodes(filteredCodes);
*/                  activity.addSearchResultByGeocaches(searchResult);

                    lastSearchResult = searchResult;
                    if (null == previousViewport || !useLastSearchResult || (!result.isEmpty() && lastSearchResult.getCount() > 400)) {
                        previousViewport = containingGCliveCaches(result);
                    }
                    Log.d("searchByViewport: cached=" + useLastSearchResult + ", results=" + lastSearchResult.getCount() + ", viewport=" + previousViewport);
                } else {
                    // retrieving stored caches
                    final SearchResult searchResult = new SearchResult(DataStore.loadCachedInViewport(viewport.resize(1.2)));
                    Log.e("load.searchResult: " + searchResult.getGeocodes());
                    final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
                    Log.e("load.cachesFromSearchResult: " + cachesFromSearchResult.size());
//                  MapUtils.filter(cachesFromSearchResult, getFilterContext());
                    activity.addSearchResultByGeocaches(cachesFromSearchResult);
                }
            } catch (Exception e) {
                Log.e("load exception: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(activity::hideProgressSpinner);
            }
        }

    }

    static boolean mapMoved(final Viewport referenceViewport, final Viewport newViewport) {
        return Math.abs(newViewport.getLatitudeSpan() - referenceViewport.getLatitudeSpan()) > 50e-6 || Math.abs(newViewport.getLongitudeSpan() - referenceViewport.getLongitudeSpan()) > 50e-6 || Math.abs(newViewport.center.getLatitude() - referenceViewport.center.getLatitude()) > referenceViewport.getLatitudeSpan() / 4 || Math.abs(newViewport.center.getLongitude() - referenceViewport.center.getLongitude()) > referenceViewport.getLongitudeSpan() / 4;
    }

    static UnifiedMapActivity getActivity(final LoadInBackgroundHandler handler) {
        return handler == null || handler.activityRef == null ? null : handler.activityRef.get();
    }

    public void onDestroy() {
        timer.dispose();
    }
}
