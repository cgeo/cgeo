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
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull

import java.lang.ref.WeakReference
import java.util.Set
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.mapsforge.map.layer.Layer

class CachesOverlay : AbstractCachesOverlay() {

    private final SearchResult search
    private final Disposable timer
    private var firstRun: Boolean = true
    private var updating: Boolean = false

    CachesOverlay(final NewMap map, final SearchResult search, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers)

        this.search = search
        this.timer = startTimer()
    }

    CachesOverlay(final NewMap map, final String geocode, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer layerAnchor, final MapHandlers mapHandlers) {
        super(map, overlayId, geoEntries, bundle, layerAnchor, mapHandlers)

        this.search = SearchResult()
        this.search.addGeocode(geocode)
        this.timer = startTimer()
    }

    private Disposable startTimer() {
        return Schedulers.newThread().schedulePeriodicallyDirect(CachesOverlay.LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS)
    }

    private class LoadTimerAction : Runnable {

        private final WeakReference<CachesOverlay> overlayRef
        private Viewport previousViewport

        LoadTimerAction(final CachesOverlay overlay) {
            this.overlayRef = WeakReference<>(overlay)
        }

        override         public Unit run() {
            val overlay: CachesOverlay = overlayRef.get()
            if (overlay == null || overlay.updating) {
                return
            }
            overlay.updating = true
            try {
                // Initially bring the main list in
                if (overlay.firstRun || overlay.isInvalidated()) {
                    val cachesToDisplay: Set<Geocache> = overlay.search.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS)
                    MapUtils.filter(cachesToDisplay, getFilterContext())
                    overlay.display(cachesToDisplay)
                    overlay.firstRun = false
                    overlay.refreshed()
                }

                // get current viewport
                val viewportNow: Viewport = overlay.getViewport()

                if (previousViewport != null && !previousViewport == (viewportNow)) {
                    overlay.updateTitle()
                }
                previousViewport = viewportNow
            } catch (final Exception e) {
                Log.w("CachesOverlay.LoadTimer.run", e)
            } finally {
                overlay.updating = false
            }
        }
    }

    private Unit display(final Set<Geocache> cachesToDisplay) {
        try {
            showProgress()
            update(cachesToDisplay)
        } finally {
            hideProgress()
        }
    }

    override     public Unit onDestroy() {
        timer.dispose()

        super.onDestroy()
    }
}
