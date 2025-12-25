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
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull

import java.lang.ref.WeakReference
import java.util.Set
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.mapsforge.map.layer.Layer

class StoredCachesOverlay : AbstractCachesOverlay() {

    private final Disposable timer

    public StoredCachesOverlay(final NewMap map, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers, final GeocacheFilterContext filterContext) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers)
        setFilterContext(filterContext)
        this.timer = startTimer()
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS)
    }

    private static class LoadTimerAction : Runnable {

        private final WeakReference<StoredCachesOverlay> overlayRef
        private var previousZoom: Int = -100
        private Viewport previousViewport

        LoadTimerAction(final StoredCachesOverlay overlay) {
            this.overlayRef = WeakReference<>(overlay)
        }

        override         public Unit run() {
            val overlay: StoredCachesOverlay = overlayRef.get()
            if (overlay == null) {
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
                val moved: Boolean = overlay.isInvalidated() || previousViewport == null || zoomNow != previousZoom ||
                        mapMoved(previousViewport, viewportNow)

                // save values
                if (moved) {

                    previousZoom = zoomNow
                    previousViewport = viewportNow
                    overlay.load()
                    overlay.refreshed()
                } else if (!previousViewport == (viewportNow)) {
                    overlay.updateTitle()
                }
            } catch (final Exception e) {
                Log.w("StoredCachesOverlay.startLoadtimer.start", e)
            }
        }
    }

    private Unit load() {
        try {
            showProgress()

            val searchResult: SearchResult = SearchResult(DataStore.loadCachedInViewport(getViewport().resize(1.2), getFilterContext().get()))

            val cachesFromSearchResult: Set<Geocache> = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS)

            MapUtils.filter(cachesFromSearchResult, getFilterContext())

            // render
            update(cachesFromSearchResult)

        } finally {
            hideProgress()
        }
    }

    override     public Unit onDestroy() {
        timer.dispose()

        super.onDestroy()
    }
}
