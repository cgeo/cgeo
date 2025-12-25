// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.mapsforge.v6.caches

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.Log
import cgeo.geocaching.location.Viewport.containingGCliveCaches

import androidx.annotation.NonNull

import java.lang.ref.WeakReference
import java.util.EnumSet
import java.util.Locale
import java.util.Set
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.mapsforge.map.layer.Layer

class LiveCachesOverlay : AbstractCachesOverlay() {

    private final Disposable timer
    private var downloading: Boolean = false

    private var lastSearchResult: SearchResult = null
    private var lastViewport: Viewport = null

    public LiveCachesOverlay(final NewMap map, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers, final GeocacheFilterContext filterContext) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers)
        setFilterContext(filterContext)
        this.timer = startTimer()
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS)
    }

    private static class LoadTimerAction : Runnable {

        private final WeakReference<LiveCachesOverlay> overlayRef
        private var previousZoom: Int = -100
        private Viewport previousCycleViewport; //viewport on last timer cycle
        private Viewport previousMoveViewport; //viewport on last move
        private var lastMovedTimestamp: Long = -1

        LoadTimerAction(final LiveCachesOverlay overlay) {
            this.overlayRef = WeakReference<>(overlay)
        }

        override         public Unit run() {
            val overlay: LiveCachesOverlay = overlayRef.get()
            if (overlay == null || overlay.isDownloading()) {
                return
            }
            try {
                // get current viewport
                val viewportNow: Viewport = overlay.getViewport()
                // Since zoomNow is used only for local comparison purposes,
                // it is ok to use the Google Maps compatible zoom level of OSM Maps
                val zoomNow: Int = overlay.getMapZoomLevel()

                // check if map moved or zoomed
                //TODO Portree Use Rectangle inside with bigger search window. That will stop reloading on every move
                val moved: Boolean = overlay.isInvalidated() || previousCycleViewport == null || zoomNow != previousZoom ||
                        previousMoveViewport == null || mapMoved(previousMoveViewport, viewportNow)

                // save values
                if (moved) {
                    val currentTime: Long = System.currentTimeMillis()

                    if (1000 < (currentTime - lastMovedTimestamp)) {
                        overlay.downloading = true
                        previousZoom = zoomNow
                        overlay.download()
                        previousMoveViewport = viewportNow
                        lastMovedTimestamp = System.currentTimeMillis()
                    }
                } else if (!previousCycleViewport == (viewportNow)) {
                    overlay.updateTitle()
                }
                previousCycleViewport = viewportNow
            } catch (final Exception e) {
                Log.w("LiveCachesOverlay.startLoadtimer.start", e)
            } finally {
                overlay.refreshed()
                overlay.downloading = false
            }
        }
    }

    private Unit download() {
        try {
            showProgress()

            val useLastSearchResult: Boolean = null != lastSearchResult && null != lastViewport && lastViewport.includes(getViewport())
            val newViewport: Viewport = getViewport().resize(3.0)
            val searchResult: SearchResult = useLastSearchResult ? lastSearchResult : ConnectorFactory.searchByViewport(newViewport, getFilterContext().get())

            val result: Set<Geocache> = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)
            MapUtils.filter(result, getFilterContext())
            // update the caches
            // first remove filtered out
            val filteredCodes: Set<String> = searchResult.getFilteredGeocodes()
            Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes)
            DataStore.removeCaches(filteredCodes, EnumSet.of(RemoveFlag.CACHE))

            Log.d(String.format(Locale.ENGLISH, "Live caches found: %d", result.size()))

            //render
            update(result)

            lastSearchResult = searchResult
            if (null == lastViewport || !useLastSearchResult || (!result.isEmpty() && lastSearchResult.getCount() > 400)) {
                lastViewport = containingGCliveCaches(result)
            }
            Log.d("searchByViewport: cached=" + useLastSearchResult + ", results=" + lastSearchResult.getCount() + ", viewport=" + lastViewport)

        } finally {
            hideProgress()
        }
    }

    override     public Unit onDestroy() {
        timer.dispose()

        super.onDestroy()
    }

    public Boolean isDownloading() {
        return downloading
    }
}
