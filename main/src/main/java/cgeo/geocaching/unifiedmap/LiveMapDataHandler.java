package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LiveMapDataHandler {

    private final Action action;
    private final Disposable actionDisposable;

    private boolean dirty;
    private final Parameters params = new Parameters();

    private static class Parameters {
        private Viewport viewport;
        private GeocacheFilter filter;
        private boolean waypointFilterChanged;
        private boolean enabled;
        private final Set<Geocache> updatedCaches = new HashSet<>();
        private boolean liveEnabled;

        Parameters copy() {
            final Parameters p = new Parameters();
            p.viewport = viewport;
            p.filter = filter;
            p.liveEnabled = liveEnabled;
            p.waypointFilterChanged = waypointFilterChanged;
            p.enabled = enabled;
            p.updatedCaches.addAll(updatedCaches);
            return p;
        }

        void reset() {
            updatedCaches.clear();
            waypointFilterChanged = false;
        }
    }

    private static final class Action implements Runnable {

        private final LiveMapDataHandler handler;
        private final UnifiedMapViewModel model;
        private final LiveMapGeocacheLoader liveLoader;

        private GeocacheFilter lastFilter;
        private Viewport lastViewport;
        private boolean lastLiveEnabled;

        Action(final LiveMapDataHandler handler, final UnifiedMapViewModel model) {
            this.handler = handler;
            this.model = model;
            this.liveLoader = new LiveMapGeocacheLoader(model.liveLoadStatus::postValue, this::updateLiveLoad);
        }

        private void updateLiveLoad(final Set<Geocache> result) {
            final GeocacheFilter filter = lastFilter;
            final Viewport viewport = lastViewport;
            if (filter != null) {
                filter.filterList(result);
            }
            model.caches.write(caches -> {
                caches.removeAll(result);
                caches.addAll(result);
            });
            UnifiedMapActivity.refreshWaypoints(model, filter, viewport, false);
        }

        @Override
        public void run() {
            try {
                //fast abort if there's nothing to do
                if (!handler.dirty) {
                    return;
                }

                //get parameters to work on and reset them
                final Parameters params;
                synchronized (this.handler) {
                    handler.dirty = false;
                    params = handler.params.copy();
                    handler.params.reset();
                }

                if (!params.enabled) {
                    return;
                }

                //updates caches
                if (!params.updatedCaches.isEmpty()) {
                    Log.iForce("Adding caches: " + params.updatedCaches);
                    model.caches.write(caches -> {
                        //first removes all, then add again. This prevents non-adding of Geocache-instances of same cache with newer data
                        caches.removeAll(params.updatedCaches);
                        caches.addAll(params.updatedCaches);
                        if (params.filter != null) {
                            params.filter.filterList(caches);
                        }
                    });
                }

                final boolean filterChanged = !GeocacheFilter.filtersSame(params.filter, lastFilter);
                final boolean mapMoved = MapUtils.mapHasMoved(params.viewport, lastViewport);
                final boolean lastViewportContainsCurrent = lastViewport != null && lastViewport.includes(params.viewport);
                final boolean liveWasTurnedOn = !lastLiveEnabled && params.liveEnabled;

                //database refresh
                if (filterChanged || (mapMoved && !lastViewportContainsCurrent)) {
                    final Set<Geocache> dbCaches = MapUtils.getGeocachesFromDatabase(params.viewport, params.filter);
                    model.caches.write(caches -> caches.addAll(dbCaches));
                }
                //live refresh
                if (params.liveEnabled && (liveWasTurnedOn || filterChanged || mapMoved)) {
                    liveLoader.requestUpdate(params.viewport, params.filter);
                }
                //waypoints
                if (filterChanged || mapMoved || params.waypointFilterChanged) {
                    UnifiedMapActivity.refreshWaypoints(model, params.filter, params.viewport, false);
                }
                if (mapMoved) {
                    lastViewport = params.viewport;
                }
                lastFilter = params.filter;
                lastLiveEnabled = params.liveEnabled;
            } catch (final Exception e) {
                Log.w("LiveMapDataHandler.run", e);
            }
        }

    }

    public LiveMapDataHandler(final UnifiedMapViewModel model) {
        this.action = new Action(this, model);
        this.actionDisposable = Schedulers.newThread().schedulePeriodicallyDirect(action, 0, 250, TimeUnit.MILLISECONDS);
    }

    public synchronized void setViewport(final Viewport viewport) {
        this.params.viewport = viewport;
        this.dirty = true;
    }

    public synchronized void setFilter(final GeocacheFilter filter) {
        this.params.filter = filter;
        this.dirty = true;
    }

    public synchronized void addChangedCache(final Geocache cache) {
        this.params.updatedCaches.add(cache);
        this.dirty = true;
    }

    public synchronized void setLiveEnabled(final boolean enabled) {
        this.params.liveEnabled = enabled;
        this.dirty = true;
    }

    public synchronized void setEnabled(final boolean enabled) {
        this.params.enabled = enabled;
        this.dirty = true;
    }

    public synchronized  boolean isEnabled() {
        return this.params.enabled;
    }

    public synchronized void setWaypointFilter(final boolean changed) {
        this.params.waypointFilterChanged = changed;
        this.dirty = true;
    }

    public void destroy() {
        this.actionDisposable.dispose();
        this.action.liveLoader.destroy();
    }
}
