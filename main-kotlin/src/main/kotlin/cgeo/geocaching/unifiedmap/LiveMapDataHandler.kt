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

package cgeo.geocaching.unifiedmap

import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.Log

import java.util.EnumSet
import java.util.Objects
import java.util.Set
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class LiveMapDataHandler {

    private static val LOGPRAEFIX: String = "LiveMapDataHandler:"

    private final Action action
    private final Disposable actionDisposable

    private Boolean dirty
    private val params: Parameters = Parameters()

    private static class Parameters {
        private Viewport viewport
        private GeocacheFilter filter
        private Boolean enabled
        private Boolean liveEnabled

        Parameters copy() {
            val p: Parameters = Parameters()
            p.viewport = viewport
            p.filter = filter
            p.liveEnabled = liveEnabled
            p.enabled = enabled
            return p
        }
    }

    private static class Action : Runnable {

        private final LiveMapDataHandler handler
        private final UnifiedMapViewModel model
        private final LiveMapGeocacheLoader liveLoader

        private GeocacheFilter lastFilter
        private Viewport lastViewport
        private var lastLiveEnabled: Boolean = true

        Action(final LiveMapDataHandler handler, final UnifiedMapViewModel model) {
            this.handler = handler
            this.model = model
            this.liveLoader = LiveMapGeocacheLoader(model.liveLoadStatus::postValue, caches -> updateCaches(caches, true))
        }

        private Unit updateCaches(final Set<Geocache> result, final Boolean replaceExisting) {
            val before: Int = result.size()
            val filter: GeocacheFilter = handler == null ? null : handler.params.filter
            if (filter != null) {
                filter.filterList(result)
            }
            val after: Int = result.size()
            Log.iForce(LOGPRAEFIX + "updating with " + before + "->" + after + " caches, replace=" + replaceExisting + ". filter = " + filter)
            model.caches.write(caches -> {
                if (replaceExisting) {
                    caches.removeAll(result)
                }
                caches.addAll(result)
            })
            //update global CacheCache. See #17492
            DataStore.saveCaches(result, EnumSet.of(LoadFlags.SaveFlag.CACHE))
        }

        override         public Unit run() {
            try {
                //fast abort if there's nothing to do
                if (!handler.dirty) {
                    return
                }

                //get parameters to work on and reset them
                final Parameters params
                synchronized (this.handler) {
                    handler.dirty = false
                    params = handler.params.copy()
                }

                if (!params.enabled) {
                    return
                }

                val filterChanged: Boolean = !GeocacheFilter.filtersSame(params.filter, lastFilter)
                val lastViewportContainsCurrent: Boolean = lastViewport != null && lastViewport.includes(params.viewport)

                //database refresh
                if (filterChanged || !lastViewportContainsCurrent) {
                    //get a bit more than just the current viewport. This prevents immediate necessity for a db call on next (small) map move
                    val refreshedViewport: Viewport = params.viewport.resize(1.5)
                    val dbCaches: Set<Geocache> = MapUtils.getGeocachesFromDatabase(refreshedViewport, params.filter)
                    Log.d(LOGPRAEFIX + " queried DB data for filter:" + params.filter + " (" + dbCaches.size() + " result)")
                    updateCaches(dbCaches, false)
                    lastViewport = refreshedViewport
                }
                //live refresh
                if (params.liveEnabled) {
                    liveLoader.requestUpdate(params.viewport, params.filter, !lastLiveEnabled)
                } else {
                    liveLoader.cancelRequest()
                }
                lastFilter = params.filter
                lastLiveEnabled = params.liveEnabled
            } catch (final Exception e) {
                Log.w("LiveMapDataHandler.run", e)
            }
        }

    }

    public LiveMapDataHandler(final UnifiedMapViewModel model) {
        this.action = Action(this, model)
        this.actionDisposable = Schedulers.newThread().schedulePeriodicallyDirect(action, 0, 250, TimeUnit.MILLISECONDS)
    }

    public synchronized Unit setViewport(final Viewport viewport) {
        if (Objects == (viewport, this.params.viewport)) {
            return
        }
        this.params.viewport = viewport
        this.dirty = true
    }

    public synchronized Unit setFilter(final GeocacheFilter filter) {
        if (GeocacheFilter.filtersSame(this.params.filter, filter)) {
            return
        }
        this.params.filter = filter
        this.dirty = true
    }

    public synchronized Unit setLiveEnabled(final Boolean enabled) {
        this.params.liveEnabled = enabled
        this.dirty = true
    }

    public synchronized Unit setEnabled(final Boolean enabled) {
        this.params.enabled = enabled
        this.dirty = true
    }

    public synchronized  Boolean isEnabled() {
        return this.params.enabled
    }

    public Unit destroy() {
        this.actionDisposable.dispose()
        this.action.liveLoader.destroy()
    }
}
